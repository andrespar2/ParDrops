/**
 * @file src/app/shared/components/variant-editor/variant-editor.component.ts
 * @description Editor de variantes de un producto.
 *
 * Permite añadir, editar y quitar versiones con talla, color, precio, stock y
 * SKU. El nombre visible se compone automáticamente a partir de la talla y el
 * color para que el comprador lo vea igual en la ficha y en el pedido.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductVariant } from '../../../core/models/product.model';

@Component({
  selector: 'app-variant-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './variant-editor.component.html',
  styleUrls: ['./variant-editor.component.scss']
})
export class VariantEditorComponent {
  /** Variantes actuales. */
  @Input() public variants: ProductVariant[] = [];

  /** Emite la lista cuando cambia. */
  @Output() public readonly variantsChange = new EventEmitter<ProductVariant[]>();

  /**
   * Añade una variante vacía.
   *
   * @returns {void}
   */
  public addVariant(): void {
    const next = [
      ...this.variants,
      {
        id: this.newId(),
        name: 'Nueva versión',
        size: null,
        color: null,
        price: null,
        stock: 0,
        sku: null,
        imageUrl: null
      }
    ];
    this.emit(next);
  }

  /**
   * Quita una variante.
   *
   * @param index posición de la variante.
   * @returns {void}
   */
  public removeVariant(index: number): void {
    this.emit(this.variants.filter((_, position) => position !== index));
  }

  /**
   * Actualiza un campo de una variante.
   *
   * @param index posición.
   * @param field campo a cambiar.
   * @param value nuevo valor.
   * @returns {void}
   */
  public update(index: number, field: keyof ProductVariant, value: string | number | null): void {
    const next = this.variants.map((variant, position) => {
      if (position !== index) return variant;
      const updated = { ...variant, [field]: value } as ProductVariant;
      if (field === 'size' || field === 'color') {
        updated.name = [updated.size, updated.color].filter(Boolean).join(' / ') || 'Versión';
      }
      return updated;
    });
    this.emit(next);
  }

  private emit(next: ProductVariant[]): void {
    this.variants = next;
    this.variantsChange.emit(next);
  }

  private newId(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }
    return `variant-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  }
}
