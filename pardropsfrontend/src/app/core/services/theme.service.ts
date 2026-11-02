import { Injectable, computed, effect, signal } from '@angular/core';

/** Modos de tema disponibles. */
export type ThemeMode = 'light' | 'dark' | 'system';

/** Tema realmente aplicado al documento. */
export type ResolvedTheme = 'light' | 'dark';

/** Clave de almacenamiento de la preferencia. */
export const THEME_STORAGE_KEY = 'pardrops_theme';

/**
 * Gestiona el tema claro, oscuro y automático.
 *
 * La preferencia se guarda en `localStorage` y se aplica con el atributo
 * `data-theme` en el elemento `<html>`. Un script en `index.html` aplica el
 * tema antes de renderizar para evitar el destello inicial.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  /** Consulta del sistema para el modo oscuro. */
  private readonly mediaQuery =
    typeof window !== 'undefined' && window.matchMedia
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : null;

  /** Indica si el sistema prefiere oscuro (reactivo). */
  private readonly systemPrefersDark = signal<boolean>(this.mediaQuery?.matches ?? false);

  /** Preferencia seleccionada por el usuario. */
  private readonly modeSignal = signal<ThemeMode>(this.readStoredMode());

  /** Preferencia actual (solo lectura). */
  public readonly mode = this.modeSignal.asReadonly();

  /** Tema resuelto que se está mostrando. */
  public readonly resolved = computed<ResolvedTheme>(() => {
    const mode = this.modeSignal();
    if (mode === 'system') {
      return this.systemPrefersDark() ? 'dark' : 'light';
    }
    return mode;
  });

  constructor() {
    this.mediaQuery?.addEventListener('change', (event) => {
      this.systemPrefersDark.set(event.matches);
    });

    effect(() => {
      const theme = this.resolved();
      if (typeof document !== 'undefined') {
        document.documentElement.setAttribute('data-theme', theme);
      }
    });
  }

  /**
   * Cambia la preferencia de tema y la persiste.
   *
   * @param mode modo elegido.
   */
  public setMode(mode: ThemeMode): void {
    this.modeSignal.set(mode);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, mode);
    } catch {
      // Sin almacenamiento disponible: el tema se mantiene en memoria.
    }
  }

  /**
   * Alterna entre los tres modos en orden: automático → claro → oscuro.
   */
  public cycle(): void {
    const current = this.modeSignal();
    const next: ThemeMode = current === 'system' ? 'light' : current === 'light' ? 'dark' : 'system';
    this.setMode(next);
  }

  /** Etiqueta accesible del modo actual. */
  public label(mode: ThemeMode = this.modeSignal()): string {
    if (mode === 'light') {
      return 'Tema claro';
    }
    if (mode === 'dark') {
      return 'Tema oscuro';
    }
    return 'Tema del sistema';
  }

  /** Lee la preferencia guardada, con el modo automático por defecto. */
  private readStoredMode(): ThemeMode {
    try {
      const stored = localStorage.getItem(THEME_STORAGE_KEY);
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        return stored;
      }
    } catch {
      // Sin acceso a localStorage.
    }
    return 'system';
  }
}
