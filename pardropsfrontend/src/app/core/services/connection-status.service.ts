/**
 * @file src/app/core/services/connection-status.service.ts
 * @description Estado compartido de reconexión con la API.
 *
 * Cuando Render está despertando, el interceptor reintenta las peticiones GET.
 * La interfaz muestra un aviso discreto en lugar de un "Failed to fetch".
 */

import { Injectable, computed, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ConnectionStatusService {
  /** Número de peticiones que están esperando una reintento. */
  private readonly pending = signal<number>(0);

  /** Indica si la API está tardando en responder y se está reintentando. */
  public readonly wakingUp = computed(() => this.pending() > 0);

  /** Marca el inicio de una espera de reconexión. */
  public beginRetry(): void {
    this.pending.update(value => value + 1);
  }

  /** Marca el final de una espera de reconexión. */
  public endRetry(): void {
    this.pending.update(value => Math.max(0, value - 1));
  }

  /** Limpia el estado (por ejemplo al recuperar la conexión). */
  public reset(): void {
    this.pending.set(0);
  }
}
