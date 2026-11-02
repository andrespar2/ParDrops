import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { SellerFinance, Withdrawal, WithdrawalStatus } from '../models/financial.model';

/** API financiera del vendedor y de administración. */
@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly http = inject(HttpClient);

  /** Resumen financiero del vendedor. */
  public finance(): Observable<SellerFinance> {
    return this.http.get<SellerFinance>(`${API_BASE_URL}/seller/finance`);
  }

  /** Crea una solicitud de retiro. */
  public requestWithdrawal(payload: {
    amount: number;
    method: 'BRE_B' | 'BANK_ACCOUNT';
    breBKey?: string | null;
    bankName?: string | null;
    accountType?: string | null;
    accountNumber?: string | null;
    accountHolder?: string | null;
    notes?: string | null;
  }): Observable<Withdrawal> {
    return this.http.post<Withdrawal>(`${API_BASE_URL}/seller/withdrawals`, payload);
  }

  /** Todas las solicitudes (administración). */
  public listAll(): Observable<Withdrawal[]> {
    return this.http.get<Withdrawal[]>(`${API_BASE_URL}/admin/withdrawals`);
  }

  /** Cambia el estado de una solicitud. */
  public updateStatus(id: number, status: WithdrawalStatus, notes?: string): Observable<Withdrawal> {
    return this.http.patch<Withdrawal>(`${API_BASE_URL}/admin/withdrawals/${id}/status`, { status, notes });
  }

  /** Completa una solicitud con la referencia de la transferencia. */
  public complete(id: number, reference: string): Observable<Withdrawal> {
    return this.http.patch<Withdrawal>(`${API_BASE_URL}/admin/withdrawals/${id}/complete`, { reference });
  }

  /** Rechaza una solicitud. */
  public reject(id: number, notes: string): Observable<Withdrawal> {
    return this.http.patch<Withdrawal>(`${API_BASE_URL}/admin/withdrawals/${id}/reject`, { notes });
  }
}
