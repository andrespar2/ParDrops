/**
 * @file src/app/shared/components/star-rating/star-rating.component.spec.ts
 * @description Pruebas del componente de estrellas de valoración.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { StarRatingComponent } from './star-rating.component';

/**
 * Devuelve el porcentaje de relleno aplicado a cada estrella.
 *
 * @param fixture componente montado.
 * @returns lista de anchos (porcentaje) de cada relleno.
 */
function fillWidths(fixture: ComponentFixture<StarRatingComponent>): number[] {
  const fills = fixture.nativeElement.querySelectorAll(
    '.star-rating__fill'
  ) as NodeListOf<HTMLElement>;
  return Array.from(fills).map(fill => parseFloat(fill.style.width));
}

/**
 * Devuelve el botón de la estrella indicada, ya tipado.
 *
 * @param fixture componente montado.
 * @param index   posición del botón (0..max-1).
 * @returns el botón de la estrella.
 */
function buttonAt(
  fixture: ComponentFixture<StarRatingComponent>,
  index: number
): HTMLButtonElement {
  const buttons = fixture.nativeElement.querySelectorAll(
    '.star-rating__button'
  ) as NodeListOf<HTMLButtonElement>;
  return buttons[index];
}

describe('StarRatingComponent', () => {
  let fixture: ComponentFixture<StarRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StarRatingComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StarRatingComponent);
  });

  it('muestra cinco estrellas con la media reflejada en el relleno', () => {
    fixture.componentRef.setInput('rating', 4.5);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.star-rating__star').length).toBe(5);
    expect(fillWidths(fixture)).toEqual([100, 100, 100, 100, 50]);
  });

  it('no dibuja botones cuando es de solo lectura', () => {
    fixture.componentRef.setInput('rating', 3);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('button').length).toBe(0);
  });

  it('emite la calificación elegida al pulsar una estrella', () => {
    let selected = 0;
    fixture.componentInstance.ratingChange.subscribe(value => (selected = value));

    fixture.componentRef.setInput('interactive', true);
    fixture.componentRef.setInput('rating', 0);
    fixture.detectChanges();

    buttonAt(fixture, 2).click();

    expect(selected).toBe(3);
  });

  it('previsualiza la calificación al pasar el cursor', () => {
    fixture.componentRef.setInput('interactive', true);
    fixture.componentRef.setInput('rating', 1);
    fixture.detectChanges();

    buttonAt(fixture, 3).dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(fillWidths(fixture)).toEqual([100, 100, 100, 100, 0]);
  });
});
