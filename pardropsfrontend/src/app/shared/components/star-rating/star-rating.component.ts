/**
 * @file src/app/shared/components/star-rating/star-rating.component.ts
 * @description Componente de estrellas de valoración.
 *
 * Se usa en dos modos: solo lectura (muestra la valoración, admitiendo medias
 * estrellas) e interactivo (el cliente elige de 1 a 5 estrellas).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, input, output, signal } from '@angular/core';

@Component({
  selector: 'app-star-rating',
  standalone: true,
  templateUrl: './star-rating.component.html',
  styleUrls: ['./star-rating.component.scss']
})
export class StarRatingComponent {
  /** Calificación mostrada (admite decimales en modo lectura). */
  public readonly rating = input<number>(0);

  /** Número máximo de estrellas. */
  public readonly max = input<number>(5);

  /** Indica si el usuario puede elegir la calificación. */
  public readonly interactive = input<boolean>(false);

  /** Tamaño visual de las estrellas. */
  public readonly size = input<'sm' | 'md'>('md');

  /** Etiqueta accesible del grupo (solo en modo interactivo). */
  public readonly label = input<string>('Selecciona una calificación');

  /** Calificación elegida por el usuario. */
  public readonly ratingChange = output<number>();

  /** Estrella bajo el cursor o el foco (0 si ninguna), para la previsualización. */
  public readonly hoverRating = signal<number>(0);

  /** Estrellas a dibujar (1..max). */
  public readonly stars = computed(() =>
    Array.from({ length: this.max() }, (_, index) => index + 1)
  );

  /** Calificación efectiva: la previsualización del cursor o la mostrada. */
  public readonly effectiveRating = computed(() =>
    this.interactive() && this.hoverRating() > 0 ? this.hoverRating() : this.rating()
  );

  /** Texto accesible de la valoración en modo lectura. */
  public readonly displayLabel = computed(
    () => `Valoración: ${this.rating()} de ${this.max()} estrellas`
  );

  /**
   * Calcula el porcentaje de relleno de una estrella.
   *
   * Permite representar calificaciones con decimales (por ejemplo, 4,5
   * estrellas rellenan la quinta a la mitad).
   *
   * @param star número de la estrella (1..max).
   * @returns porcentaje de relleno entre 0 y 100.
   */
  public fillPercentage(star: number): number {
    const value = this.effectiveRating();
    const fill = Math.max(0, Math.min(1, value - (star - 1)));
    return Math.round(fill * 100);
  }

  /**
   * Marca la estrella bajo el cursor o el foco.
   *
   * @param star número de la estrella.
   * @returns {void}
   */
  public hover(star: number): void {
    if (this.interactive()) {
      this.hoverRating.set(star);
    }
  }

  /** Quita la previsualización del cursor. */
  public clearHover(): void {
    this.hoverRating.set(0);
  }

  /**
   * Confirma la calificación elegida.
   *
   * @param star número de la estrella pulsada.
   * @returns {void}
   */
  public select(star: number): void {
    if (this.interactive()) {
      this.ratingChange.emit(star);
    }
  }
}
