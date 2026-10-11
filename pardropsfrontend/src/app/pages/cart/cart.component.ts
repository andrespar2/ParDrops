/**
 * @file src/app/pages/cart/cart.component.ts
 * @description Página del carrito de compras.
 * 
 * Muestra todos los ítems en el carrito, permite gestionar cantidades,
 * eliminar productos, y muestra un resumen con subtotal, impuestos,
 * envío y total. También incluye funcionalidad para "comprar".
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent {
  /** Servicio del carrito */
  private readonly cartService = inject(CartService);

  /** Referencia al carrito computado */
  public readonly cart = this.cartService.cart;

  /** Indica si se ha realizado una compra */
  public purchaseCompleted = false;

  /**
   * Incrementa la cantidad de un ítem.
   * @param itemId - ID del ítem.
   */
  public increaseQuantity(itemId: string): void {
    this.cartService.increaseQuantity(itemId);
  }

  /**
   * Decrementa la cantidad de un ítem.
   * @param itemId - ID del ítem.
   */
  public decreaseQuantity(itemId: string): void {
    this.cartService.decreaseQuantity(itemId);
  }

  /**
   * Actualiza la cantidad de un ítem.
   * @param itemId - ID del ítem.
   * @param event - Evento del input.
   */
  public updateQuantity(itemId: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const quantity = parseInt(input.value, 10);
    if (!isNaN(quantity)) {
      this.cartService.updateQuantity(itemId, quantity);
    }
  }

  /**
   * Elimina un ítem del carrito.
   * @param itemId - ID del ítem.
   */
  public removeItem(itemId: string): void {
    this.cartService.removeItem(itemId);
  }

  /**
   * Vacía el carrito.
   */
  public clearCart(): void {
    this.cartService.clearCart();
  }

  /**
   * Procesa la compra.
   */
  public checkout(): void {
    this.purchaseCompleted = true;
    this.cartService.clearCart();
    
    setTimeout(() => {
      this.purchaseCompleted = false;
    }, 5000);
  }

  /**
   * Formatea el precio con moneda.
   * @param price - Precio a formatear.
   */
  public formatPrice(price: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2
    }).format(price);
  }
}