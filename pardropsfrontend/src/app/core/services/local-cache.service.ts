/**
 * @file src/app/core/services/local-cache.service.ts
 * @description Caché persistente y acotada en el dispositivo del usuario.
 *
 * Solo guarda respuestas públicas (catálogo, tiendas, categorías, rankings).
 * Cada entrada lleva versión, fecha de expiración y tamaño; el servicio limpia
 * lo caducado y aplica un límite global para no crecer sin control. En SSR no
 * hace nada porque `localStorage` no existe.
 */

import { Injectable } from '@angular/core';

interface CacheEnvelope<T> {
  /** Versión del contrato de datos. */
  version: string;
  /** Momento de expiración en milisegundos. */
  expiresAt: number;
  /** Momento de creación, para desalojar lo más antiguo. */
  createdAt: number;
  /** Valor serializado. */
  value: T;
}

@Injectable({ providedIn: 'root' })
export class LocalCacheService {
  /** Versión global: cambiarla invalida todas las entradas. */
  private readonly version = 'v1';

  /** Prefijo de todas las claves. */
  private readonly prefix = `pardrops:local:${this.version}:`;

  /** Límite total aproximado (2 MB). */
  private readonly maxBytes = 2 * 1024 * 1024;

  constructor() {
    this.cleanup();
  }

  /**
   * Recupera una entrada vigente.
   *
   * @param key clave lógica.
   * @returns el valor o `null` si no existe, caducó o no es válido.
   */
  public get<T>(key: string): T | null {
    if (!this.storageAvailable()) return null;
    try {
      const raw = localStorage.getItem(this.prefix + key);
      if (!raw) return null;
      const envelope = JSON.parse(raw) as CacheEnvelope<T>;
      if (!envelope || envelope.version !== this.version) {
        localStorage.removeItem(this.prefix + key);
        return null;
      }
      if (!envelope.expiresAt || envelope.expiresAt <= Date.now()) {
        localStorage.removeItem(this.prefix + key);
        return null;
      }
      return envelope.value;
    } catch {
      return null;
    }
  }

  /**
   * Guarda una entrada con expiración.
   *
   * @param key clave lógica.
   * @param value valor serializable.
   * @param ttlSeconds tiempo de vida en segundos.
   */
  public set<T>(key: string, value: T, ttlSeconds: number): void {
    if (!this.storageAvailable()) return;
    try {
      const envelope: CacheEnvelope<T> = {
        version: this.version,
        expiresAt: Date.now() + Math.max(ttlSeconds, 1) * 1000,
        createdAt: Date.now(),
        value
      };
      const serialized = JSON.stringify(envelope);
      if (serialized.length > this.maxBytes / 2) {
        return;
      }
      localStorage.setItem(this.prefix + key, serialized);
      this.enforceLimit();
    } catch {
      // Cuota llena o almacenamiento bloqueado: se degrada sin romper la UI.
      this.cleanup(true);
    }
  }

  /**
   * Elimina una entrada concreta.
   *
   * @param key clave lógica.
   */
  public remove(key: string): void {
    if (!this.storageAvailable()) return;
    try {
      localStorage.removeItem(this.prefix + key);
    } catch {
      // Sin efecto si el almacenamiento está bloqueado.
    }
  }

  /**
   * Invalida todas las entradas de datos públicos.
   */
  public clear(): void {
    if (!this.storageAvailable()) return;
    try {
      Object.keys(localStorage)
        .filter(key => key.startsWith(this.prefix))
        .forEach(key => localStorage.removeItem(key));
    } catch {
      // Sin efecto si el almacenamiento está bloqueado.
    }
  }

  /**
   * Elimina entradas caducadas o corruptas.
   *
   * @param aggressive si es `true`, vacía también las vigentes.
   */
  private cleanup(aggressive = false): void {
    if (!this.storageAvailable()) return;
    try {
      Object.keys(localStorage)
        .filter(key => key.startsWith(this.prefix))
        .forEach(key => {
          if (aggressive) {
            localStorage.removeItem(key);
            return;
          }
          try {
            const envelope = JSON.parse(localStorage.getItem(key) ?? '') as CacheEnvelope<unknown>;
            if (!envelope || envelope.expiresAt <= Date.now() || envelope.version !== this.version) {
              localStorage.removeItem(key);
            }
          } catch {
            localStorage.removeItem(key);
          }
        });
    } catch {
      // Sin efecto si el almacenamiento está bloqueado.
    }
  }

  /**
   * Desaloja las entradas más antiguas cuando se supera el límite.
   */
  private enforceLimit(): void {
    try {
      const entries = Object.keys(localStorage)
        .filter(key => key.startsWith(this.prefix))
        .map(key => ({ key, raw: localStorage.getItem(key) ?? '' }));
      let total = entries.reduce((sum, entry) => sum + entry.raw.length, 0);
      if (total <= this.maxBytes) return;

      entries
        .map(entry => {
          let createdAt = 0;
          try {
            createdAt = (JSON.parse(entry.raw) as CacheEnvelope<unknown>).createdAt ?? 0;
          } catch {
            // Se trata como la entrada más antigua.
          }
          return { ...entry, createdAt };
        })
        .sort((a, b) => a.createdAt - b.createdAt)
        .forEach(entry => {
          if (total <= this.maxBytes) return;
          localStorage.removeItem(entry.key);
          total -= entry.raw.length;
        });
    } catch {
      // Sin efecto si el almacenamiento está bloqueado.
    }
  }

  private storageAvailable(): boolean {
    return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
  }
}
