/**
 * @file src/app/pages/checkout/checkout.component.ts
 * @description Página de finalización de compra (checkout).
 *
 * Recoge los datos del cliente necesarios para el pedido (nombre, correo,
 * teléfono y dirección), muestra el resumen del carrito y registra el pedido en
 * la API. Si hay sesión iniciada, el pedido se guarda en el historial del
 * usuario; si se compra como invitado, se registra sin asociarse a una cuenta.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';
import { CreateOrderItemRequest, Order } from '../../core/models/order.model';
import { DEFAULT_COUNTRY, formatCurrency } from '../../core/config/commerce.config';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent {
  private readonly fb = inject(FormBuilder);
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly authService = inject(AuthService);

  /** Carrito actual (para el resumen del pedido). */
  public readonly cart = this.cartService.cart;

  /** Indica si el usuario actual está autenticado. */
  public readonly isAuthenticated = this.authService.isAuthenticated;

  /** Indica si hay una petición de compra en curso. */
  public readonly submitting = signal<boolean>(false);

  /** Mensaje de error de la compra (si lo hubiera). */
  public readonly errorMessage = signal<string | null>(null);

  /** Pedido creado con éxito (para la pantalla de confirmación). */
  public readonly createdOrder = signal<Order | null>(null);

  /** Indica si la compra se completó. */
  public readonly isCompleted = computed(() => this.createdOrder() !== null);

  /**
   * Formulario con los datos del cliente. Se rellena parcialmente con los datos
   * del usuario autenticado, si lo hay.
   */
  public readonly form = this.fb.nonNullable.group({
    customerName: [this.authService.user()?.fullName ?? '', [Validators.required, Validators.maxLength(120)]],
    customerEmail: [this.authService.user()?.email ?? '', [Validators.required, Validators.email]],
    customerPhone: [this.authService.user()?.phone ?? '', [Validators.required, Validators.maxLength(30)]],
    addressStreet: ['', [Validators.required, Validators.maxLength(250)]],
    addressCity: ['', [Validators.required, Validators.maxLength(120)]],
    addressPostalCode: ['', [Validators.required, Validators.maxLength(20)]],
    addressCountry: [DEFAULT_COUNTRY, [Validators.required, Validators.maxLength(120)]],
    notes: ['', [Validators.maxLength(500)]]
  });

  /**
   * Registra el pedido a partir del carrito y el formulario.
   *
   * @returns {void}
   */
  public onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.cart().isEmpty) {
      this.errorMessage.set('Tu carrito está vacío.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const items: CreateOrderItemRequest[] = this.cart().items.map(item => ({
      productId: item.product.id,
      productSlug: item.product.slug,
      variantName: item.selectedVariant?.name ?? null,
      quantity: item.quantity
    }));

    this.orderService.createOrder({ ...this.form.getRawValue(), items }).subscribe({
      next: order => {
        this.createdOrder.set(order);
        this.cartService.clearCart();
        this.submitting.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo completar la compra. Inténtalo de nuevo.'
        );
        this.submitting.set(false);
      }
    });
  }

  /**
   * Formatea un importe como moneda.
   *
   * @param price importe a formatear.
   * @returns el importe formateado en pesos colombianos.
   */
  public formatPrice(price: number): string {
    return formatCurrency(price);
  }
}
