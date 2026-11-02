import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { SupportConversation, SupportMessage } from '../models/support.model';

/**
 * API de soporte.
 *
 * Los usuarios solo ven sus conversaciones; el equipo administrativo usa los
 * endpoints de administración.
 */
@Injectable({ providedIn: 'root' })
export class SupportService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Conversaciones propias. */
  private readonly mineUrl = `${API_BASE_URL}/support/conversations`;

  /** Conversaciones para administración. */
  private readonly staffUrl = `${API_BASE_URL}/admin/support/conversations`;

  /** Lista las conversaciones del usuario. */
  public listMine(): Observable<SupportConversation[]> {
    return this.http.get<SupportConversation[]>(this.mineUrl);
  }

  /** Abre una conversación. */
  public create(subject: string, message: string): Observable<SupportConversation> {
    return this.http.post<SupportConversation>(this.mineUrl, { subject, message });
  }

  /** Mensajes de una conversación propia. */
  public messages(id: number): Observable<SupportMessage[]> {
    return this.http.get<SupportMessage[]>(`${this.mineUrl}/${id}/messages`);
  }

  /** Envía un mensaje como usuario. */
  public send(id: number, body: string): Observable<SupportMessage> {
    return this.http.post<SupportMessage>(`${this.mineUrl}/${id}/messages`, { body });
  }

  /** Lista todas las conversaciones (administración). */
  public listForStaff(): Observable<SupportConversation[]> {
    return this.http.get<SupportConversation[]>(this.staffUrl);
  }

  /** Mensajes de una conversación (administración). */
  public staffMessages(id: number): Observable<SupportMessage[]> {
    return this.http.get<SupportMessage[]>(`${this.staffUrl}/${id}/messages`);
  }

  /** Responde como soporte. */
  public reply(id: number, body: string): Observable<SupportMessage> {
    return this.http.post<SupportMessage>(`${this.staffUrl}/${id}/messages`, { body });
  }

  /** Marca la conversación como resuelta. */
  public resolve(id: number): Observable<void> {
    return this.http.patch<void>(`${this.staffUrl}/${id}/resolve`, {});
  }
}
