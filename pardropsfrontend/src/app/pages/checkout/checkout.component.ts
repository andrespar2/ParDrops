/**
 * @file src/app/pages/checkout/checkout.component.ts
 * @description Página de finalización de compra con Wompi.
 *
 * Envía el carrito al backend, que recalcula los precios desde la base de
 * datos, aplica el cargo de servicio y firma la operación. Después abre el
 * widget de Wompi. La compra solo se considera realizada en la pantalla de
 * resultado cuando el backend confirma la transacción.
 *
 * @author ParDrops E-commerce
 * @version 2.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CartService } from '../../core/services/cart.service';
import { CheckoutService } from '../../core/services/checkout.service';
import { WompiService } from '../../core/services/wompi.service';
import { AuthService } from '../../core/services/auth.service';
import { CheckoutSession } from '../../core/models/checkout.model';
import { CreateOrderItemRequest, CreateOrderRequest } from '../../core/models/order.model';
import { DEFAULT_COUNTRY, SERVICE_FEE_COP, formatCurrency } from '../../core/config/commerce.config';

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
  private readonly checkoutService = inject(CheckoutService);
  private readonly wompiService = inject(WompiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /** Carrito actual (para el resumen del pedido). */
  public readonly cart = this.cartService.cart;

  /** Indica si el usuario actual está autenticado. */
  public readonly isAuthenticated = this.authService.isAuthenticated;

  /** Indica si hay una petición de compra en curso. */
  public readonly submitting = signal<boolean>(false);

  /** Mensaje de error de la compra (si lo hubiera). */
  public readonly errorMessage = signal<string | null>(null);

  /** Datos del checkout calculados por el backend (precios definitivos). */
  public readonly session = signal<CheckoutSession | null>(null);

  /** Indica si el backend cambió algún precio respecto al carrito local. */
  public readonly priceChanged = computed(() => {
    const current = this.session();
    const localTotal = this.cart().total + SERVICE_FEE_COP;
    return current !== null && Math.abs(current.total - localTotal) > 0.5;
  });

  /** Cargo de servicio mostrado antes de confirmar el pedido. */
  public readonly serviceFee = SERVICE_FEE_COP;

  /** Total que se muestra en el resumen. */
  public readonly displayTotal = computed(
    () => this.session()?.total ?? (this.cart().total + SERVICE_FEE_COP)
  );

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
   * Crea el pedido pendiente y abre el widget de Wompi.
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
      variantId: item.selectedVariant?.id ?? null,
      variantName: item.selectedVariant?.name ?? null,
      quantity: item.quantity
    }));

    const request: CreateOrderRequest = { ...this.form.getRawValue(), items };

    this.checkoutService.createSession(request).subscribe({
      next: session => {
        this.session.set(session);
        this.cartService.syncPrices(session.items ?? []);
        void this.openWidget(session);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo iniciar el pago. Inténtalo de nuevo.'
        );
        this.submitting.set(false);
      }
    });
  }

  /**
   * Abre el widget y lleva al comprador a la pantalla de resultado.
   *
   * @param session datos firmados del checkout.
   * @returns {Promise<void>}
   */
  private async openWidget(session: CheckoutSession): Promise<void> {
    try {
      const result = await this.wompiService.open({
        currency: session.currency,
        amountInCents: session.amountInCents,
        reference: session.reference,
        publicKey: session.publicKey,
        signature: { integrity: session.integritySignature },
        redirectUrl: session.redirectUrl,
        customerData: {
          email: session.customerEmail,
          fullName: session.customerName,
          phoneNumber: session.customerPhone,
          phoneNumberPrefix: '+57'
        }
      });

      const transactionId = result.transaction?.id;
      await this.router.navigate(['/compra', session.reference], {
        queryParams: transactionId ? { id: transactionId } : {}
      });
    } catch (error) {
      console.error('No se pudo abrir el widget de Wompi', error);
      this.errorMessage.set(
        error instanceof Error
          ? error.message
          : 'No se pudo abrir el pago de Wompi. Inténtalo de nuevo.'
      );
      this.submitting.set(false);
    }
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
