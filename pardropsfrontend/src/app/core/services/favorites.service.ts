import { Injectable, computed, effect, signal } from '@angular/core';

/** Clave de almacenamiento de los favoritos. */
export const FAVORITES_STORAGE_KEY = 'pardrops_favorites';

/**
 * Favoritos del visitante, persistidos en el navegador.
 *
 * Se guardan solo identificadores de producto: el catálogo se resuelve desde la
 * API, así que un producto eliminado desaparece también de la lista.
 */
@Injectable({ providedIn: 'root' })
export class FavoritesService {
  /** Identificadores favoritos. */
  private readonly idsSignal = signal<string[]>(this.read());

  /** Lista de identificadores (solo lectura). */
  public readonly ids = this.idsSignal.asReadonly();

  /** Número de favoritos. */
  public readonly count = computed(() => this.idsSignal().length);

  constructor() {
    effect(() => {
      const ids = this.idsSignal();
      try {
        localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(ids));
      } catch {
        // Sin almacenamiento disponible: se mantiene en memoria.
      }
    });
  }

  /**
   * Indica si un producto está en favoritos.
   *
   * @param productId identificador del producto.
   * @returns `true` si ya está guardado.
   */
  public isFavorite(productId: string): boolean {
    return this.idsSignal().includes(productId);
  }

  /**
   * Añade o quita un producto de favoritos.
   *
   * @param productId identificador del producto.
   */
  public toggle(productId: string): void {
    const current = this.idsSignal();
    this.idsSignal.set(
      current.includes(productId)
        ? current.filter((id) => id !== productId)
        : [...current, productId],
    );
  }

  /**
   * Elimina un producto de favoritos.
   *
   * @param productId identificador del producto.
   */
  public remove(productId: string): void {
    this.idsSignal.set(this.idsSignal().filter((id) => id !== productId));
  }

  /** Lee la lista guardada, ignorando valores inválidos. */
  private read(): string[] {
    try {
      const raw = localStorage.getItem(FAVORITES_STORAGE_KEY);
      if (!raw) {
        return [];
      }
      const parsed: unknown = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed.filter((id): id is string => typeof id === 'string') : [];
    } catch {
      return [];
    }
  }
}
