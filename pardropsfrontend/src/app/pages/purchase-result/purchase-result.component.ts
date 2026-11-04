/**
 * @file src/app/pages/purchase-result/purchase-result.component.ts
 * @description Pantalla de resultado de una compra con Wompi.
 *
 * La compra solo se muestra como realizada cuando el backend confirma que
 * Wompi aprobó la transacción. La URL de retorno del navegador no se usa como
 * prueba: se consulta el estado real y, mientras esté pendiente, se sondea de
 * forma controlada.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CheckoutService } from '../../core/services/checkout.service';
import { WompiService } from '../../core/services/wompi.service';
import { CartService } from '../../core/services/cart.service';
import { CheckoutSession, CheckoutStatus } from '../../core/models/checkout.model';
import { Order, orderStatusLabel } from '../../core/models/order.model';
import { formatCurrency, formatDateTime } from '../../core/config/commerce.config';

/** Máximo de consultas automáticas mientras el pago está pendiente. */
const MAX_POLLS = 10;

/** Intervalo entre consultas (milisegundos). */
const POLL_INTERVAL_MS = 4000;

@Component({
  selector: 'app-purchase-result',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './purchase-result.component.html',
  styleUrls: ['./purchase-result.component.scss']
})
export class PurchaseResultComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly checkoutService = inject(CheckoutService);
  private readonly wompiService = inject(WompiService);
  private readonly cartService = inject(CartService);

  /** Referencia del pago consultada. */
  public readonly reference = signal<string>('');

  /** Estado actual del pago. */
  public readonly status = signal<CheckoutStatus | null>(null);

  /** Indica si la consulta está en curso. */
  public readonly loading = signal<boolean>(true);

  /** Error de consulta o de reintento. */
  public readonly errorMessage = signal<string | null>(null);

  /** Indica si se está reintentando el pago. */
  public readonly retrying = signal<boolean>(false);

  /** Número de consultas automáticas realizadas. */
  private polls = 0;

  /** Temporizador del sondeo. */
  private timer: ReturnType<typeof setTimeout> | null = null;

  /** Evita vaciar el carrito más de una vez. */
  private cartCleared = false;

  /** Pedido confirmado (solo si el pago está aprobado y verificado). */
  public readonly approved = computed(() => {
    const current = this.status();
    return current?.verifiedPayment === true && current.paymentStatus === 'APPROVED';
  });

  /** Indica si el pago sigue en proceso. */
  public readonly pending = computed(() => {
    const current = this.status();
    return current !== null
      && current.paymentStatus !== 'APPROVED'
      && !current.canRetry;
  });

  /** Indica si el pago fue rechazado o anulado. */
  public readonly rejected = computed(() => this.status()?.canRetry === true);

  /** Pedido del resultado. */
  public readonly order = computed<Order | null>(() => this.status()?.order ?? null);

  ngOnInit(): void {
    this.reference.set(this.route.snapshot.paramMap.get('reference') ?? '');
    const transactionId = this.route.snapshot.queryParamMap.get('id');
    this.loadStatus(transactionId);
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  /**
   * Consulta el estado real del pago.
   *
   * @param transactionId identificador informado por Wompi (opcional).
   * @returns {void}
   */
  public loadStatus(transactionId?: string | null): void {
    const reference = this.reference();
    if (!reference) {
      this.errorMessage.set('No encontramos la referencia del pago.');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.checkoutService.getStatus(reference, transactionId).subscribe({
      next: result => {
        this.status.set(result);
        this.loading.set(false);

        if (this.approved()) {
          this.clearTimer();
          if (!this.cartCleared) {
            this.cartService.clearCart();
            this.cartCleared = true;
          }
          return;
        }

        if (this.pending() && this.polls < MAX_POLLS) {
          this.schedulePoll();
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(
          error.error?.message || 'No se pudo consultar el estado del pago.'
        );
      }
    });
  }

  /**
   * Vuelve a consultar manualmente el estado.
   *
   * @returns {void}
   */
  public refresh(): void {
    this.polls = 0;
    this.clearTimer();
    this.loadStatus(this.route.snapshot.queryParamMap.get('id'));
  }

  /**
   * Reintenta el pago generando una nueva referencia para el mismo pedido.
   *
   * @returns {void}
   */
  public retryPayment(): void {
    const reference = this.reference();
    if (!reference || this.retrying()) {
      return;
    }

    this.retrying.set(true);
    this.errorMessage.set(null);

    this.checkoutService.retry(reference).subscribe({
      next: session => {
        void this.openWidget(session);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(
          error.error?.message || 'No se pudo reintentar el pago.'
        );
        this.retrying.set(false);
      }
    });
  }

  /**
   * Abre de nuevo el widget de Wompi y navega al nuevo resultado.
   *
   * @param session datos firmados del nuevo intento.
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

      this.retrying.set(false);
      const transactionId = result.transaction?.id;
      await this.router.navigate(['/compra', session.reference], {
        queryParams: transactionId ? { id: transactionId } : {}
      });
      this.reference.set(session.reference);
      this.polls = 0;
      this.loadStatus(transactionId);
    } catch (error) {
      console.error('No se pudo abrir el widget de Wompi', error);
      this.errorMessage.set(
        error instanceof Error
          ? error.message
          : 'No se pudo abrir el pago de Wompi. Inténtalo de nuevo.'
      );
      this.retrying.set(false);
    }
  }

  /**
   * Programa la siguiente consulta automática.
   *
   * @returns {void}
   */
  private schedulePoll(): void {
    this.clearTimer();
    this.timer = setTimeout(() => {
      this.polls += 1;
      this.loadStatus(this.route.snapshot.queryParamMap.get('id'));
    }, POLL_INTERVAL_MS);
  }

  /** Cancela el sondeo pendiente. */
  private clearTimer(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  /**
   * Formatea un importe como moneda.
   *
   * @param value importe.
   * @returns el importe en pesos colombianos.
   */
  public formatPrice(value: number | null | undefined): string {
    return formatCurrency(value ?? 0);
  }

  /**
   * Formatea una fecha con hora.
   *
   * @param value fecha ISO.
   * @returns la fecha en español de Colombia.
   */
  public formatDate(value: string | null | undefined): string {
    return value ? formatDateTime(value) : '';
  }

  /**
   * Devuelve la etiqueta del estado del pedido.
   *
   * @param status estado del pedido.
   * @returns la etiqueta en español.
   */
  public orderStatusLabel(status: Order['status']): string {
    return orderStatusLabel(status);
  }
}
