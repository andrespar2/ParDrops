import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { FinanceService } from '../../core/services/finance.service';
import { Withdrawal, WithdrawalStatus } from '../../core/models/financial.model';
import { formatCurrency } from '../../core/config/commerce.config';

/** Panel administrativo de solicitudes de retiro. */
@Component({
  selector: 'app-admin-withdrawals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-withdrawals.component.html',
  styleUrls: ['../seller-balance/seller-balance.component.scss', './admin-withdrawals.component.scss']
})
export class AdminWithdrawalsComponent {
  private readonly financeService = inject(FinanceService);

  /** Solicitudes. */
  public readonly withdrawals = signal<Withdrawal[]>([]);

  /** Estado del panel. */
  public readonly error = signal<string | null>(null);
  public readonly success = signal<string | null>(null);

  /** Referencia de transferencia por solicitud. */
  public readonly references = signal<Record<number, string>>({});

  /** Motivo de rechazo por solicitud. */
  public readonly reasons = signal<Record<number, string>>({});

  constructor() {
    this.load();
  }

  /** Carga las solicitudes. */
  public load(): void {
    this.financeService.listAll().subscribe({
      next: list => this.withdrawals.set(list),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'No se pudieron cargar las solicitudes.')
    });
  }

  /** Cambia el estado sin mover dinero. */
  public setStatus(withdrawal: Withdrawal, status: WithdrawalStatus): void {
    this.clear();
    this.financeService.updateStatus(withdrawal.id, status).subscribe({
      next: () => {
        this.success.set('Estado actualizado.');
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'No se pudo actualizar.')
    });
  }

  /** Completa la solicitud. */
  public complete(withdrawal: Withdrawal): void {
    this.clear();
    const reference = (this.references()[withdrawal.id] || '').trim();
    if (!reference) {
      this.error.set('Indica la referencia de la transferencia.');
      return;
    }
    this.financeService.complete(withdrawal.id, reference).subscribe({
      next: () => {
        this.success.set('Retiro completado y saldo actualizado.');
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'No se pudo completar.')
    });
  }

  /** Rechaza la solicitud. */
  public reject(withdrawal: Withdrawal): void {
    this.clear();
    const notes = (this.reasons()[withdrawal.id] || '').trim();
    this.financeService.reject(withdrawal.id, notes).subscribe({
      next: () => {
        this.success.set('Solicitud rechazada y saldo liberado.');
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'No se pudo rechazar.')
    });
  }

  /** Guarda la referencia escrita. */
  public setReference(id: number, value: string): void {
    this.references.update(map => ({ ...map, [id]: value }));
  }

  /** Guarda el motivo escrito. */
  public setReason(id: number, value: string): void {
    this.reasons.update(map => ({ ...map, [id]: value }));
  }

  /** Formatea un importe. */
  public money(value: number | null | undefined): string {
    return formatCurrency(value ?? 0);
  }

  /** Limpia los mensajes. */
  private clear(): void {
    this.error.set(null);
    this.success.set(null);
  }
}

