import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminAccountService } from '../../core/services/admin-account.service';
import { AdminAccount } from '../../core/models/admin-account.model';

/**
 * Página de cuentas administrativas.
 *
 * El administrador principal crea cuentas de gestor o soporte, cambia su nivel
 * y las desactiva. Las reglas se validan en el servidor.
 */
@Component({
  selector: 'app-admin-accounts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-accounts.component.html',
  styleUrls: ['./admin-accounts.component.scss']
})
export class AdminAccountsComponent {
  /** Servicio de cuentas. */
  private readonly service = inject(AdminAccountService);

  /** Constructor de formularios. */
  private readonly fb = inject(FormBuilder);

  /** Cuentas cargadas. */
  public readonly accounts = signal<AdminAccount[]>([]);

  /** Estado de carga. */
  public readonly loading = signal<boolean>(false);

  /** Mensaje de error. */
  public readonly error = signal<string | null>(null);

  /** Mensaje de éxito. */
  public readonly success = signal<string | null>(null);

  /** Estado del formulario de alta. */
  public readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    level: ['MANAGER' as 'MANAGER' | 'SUPPORT', [Validators.required]]
  });

  constructor() {
    this.load();
  }

  /** Carga las cuentas administrativas. */
  public load(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: accounts => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudieron cargar las cuentas.');
        this.loading.set(false);
      }
    });
  }

  /** Crea una cuenta administrativa. */
  public create(): void {
    this.error.set(null);
    this.success.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.service.create({
      email: value.email.trim(),
      fullName: value.fullName.trim(),
      password: value.password,
      level: value.level
    }).subscribe({
      next: () => {
        this.success.set('Cuenta administrativa creada.');
        this.form.reset({ level: 'MANAGER' });
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo crear la cuenta.');
      }
    });
  }

  /** Cambia el nivel de una cuenta. */
  public changeLevel(account: AdminAccount, level: string): void {
    this.error.set(null);
    this.service.updateLevel(account.id, level as 'MANAGER' | 'SUPPORT').subscribe({
      next: () => {
        this.success.set('Nivel actualizado.');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo cambiar el nivel.');
      }
    });
  }

  /** Activa o desactiva una cuenta. */
  public toggleStatus(account: AdminAccount): void {
    this.error.set(null);
    this.service.updateStatus(account.id, account.status !== 'ACTIVE').subscribe({
      next: () => {
        this.success.set(account.status === 'ACTIVE' ? 'Cuenta desactivada.' : 'Cuenta reactivada.');
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'No se pudo cambiar el estado.');
      }
    });
  }
}
