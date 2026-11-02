import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AdminAccount, AdminLevel, CreateAdminRequest } from '../models/admin-account.model';

/**
 * API de cuentas administrativas.
 *
 * La autorización real la aplica el backend: solo el administrador principal
 * puede crear, cambiar de nivel o desactivar cuentas.
 */
@Injectable({ providedIn: 'root' })
export class AdminAccountService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** URL del recurso. */
  private readonly url = `${API_BASE_URL}/admin/accounts`;

  /** Lista las cuentas administrativas. */
  public list(): Observable<AdminAccount[]> {
    return this.http.get<AdminAccount[]>(this.url);
  }

  /** Crea una cuenta administrativa. */
  public create(request: CreateAdminRequest): Observable<AdminAccount> {
    return this.http.post<AdminAccount>(this.url, request);
  }

  /** Cambia el nivel de una cuenta. */
  public updateLevel(id: number, level: Exclude<AdminLevel, 'OWNER'>): Observable<AdminAccount> {
    return this.http.patch<AdminAccount>(`${this.url}/${id}/level`, { level });
  }

  /** Activa o desactiva una cuenta. */
  public updateStatus(id: number, active: boolean): Observable<AdminAccount> {
    return this.http.patch<AdminAccount>(`${this.url}/${id}/status`, { active });
  }
}
