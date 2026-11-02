import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { FinanceService } from '../../core/services/finance.service';
import { SellerFinance } from '../../core/models/financial.model';
import { formatCurrency } from '../../core/config/commerce.config';

/** Página de saldo y retiros del vendedor. */
@Component({
  selector: 'app-seller-balance',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './seller-balance.component.html',
  styleUrls: ['./seller-balance.component.scss']
})
export class SellerBalanceComponent {
  private readonly financeService = inject(FinanceService);
  private readonly fb = inject(FormBuilder);

  /** Resumen financiero. */
  public readonly finance = signal<SellerFinance | null>(null);

  /** Error mostrado. */
  public readonly error = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly success = signal<string | null>(null);

  /** Formulario de retiro. */
  public readonly form = this.fb.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(1)]],
    method: ['BRE_B' as 'BRE_B' | 'BANK_ACCOUNT', [Validators.required]],
    breBKey: [''],
    bankName: [''],
    accountType: ['Ahorros'],
    accountNumber: [''],
    accountHolder: ['']
  });

  /** Importe solicitado. */
  public readonly amount = signal<number>(0);

  /** Comisión aplicable. */
  public readonly fee = computed(() => {
    const data = this.finance();
    if (!data) return 0;
    return this.form.controls.method.value === 'BANK_ACCOUNT' ? data.bankFee : 0;
  });

  /** Valor neto que recibirá. */
  public readonly net = computed(() => Math.max(0, this.amount() - this.fee()));

  constructor() {
    this.load();
    this.form.controls.amount.valueChanges.subscribe(value => this.amount.set(Number(value) || 0));
  }

  /** Carga el resumen financiero. */
  public load(): void {
    this.financeService.finance().subscribe({
      next: data => this.finance.set(data),
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'No se pudo cargar tu saldo.')
    });
  }

  /** Envía la solicitud de retiro. */
  public submit(): void {
    this.error.set(null);
    this.success.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.financeService.requestWithdrawal({
      amount: Number(value.amount),
      method: value.method,
      breBKey: value.breBKey || null,
      bankName: value.bankName || null,
      accountType: value.accountType || null,
      accountNumber: value.accountNumber || null,
      accountHolder: value.accountHolder || null
    }).subscribe({
      next: () => {
        this.success.set('Solicitud registrada. El dinero quedó reservado.');
        this.form.patchValue({ amount: 0, breBKey: '', accountNumber: '' });
        this.amount.set(0);
        this.load();
      },
      error: (error: HttpErrorResponse) => this.error.set(error.error?.message || 'No se pudo registrar la solicitud.')
    });
  }

  /** Formatea un importe. */
  public money(value: number | null | undefined): string {
    return formatCurrency(value ?? 0);
  }
}
