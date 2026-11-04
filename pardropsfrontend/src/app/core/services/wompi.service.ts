/**
 * @file src/app/core/services/wompi.service.ts
 * @description Carga e invoca el widget de checkout de Wompi.
 *
 * El widget se carga bajo demanda desde Wompi y recibe únicamente la llave
 * pública, la referencia, el monto y la firma de integridad calculada por el
 * backend. El frontend nunca ve llaves privadas ni secretos.
 */

import { Injectable } from '@angular/core';

/** Script oficial del widget de Wompi. */
const WIDGET_SCRIPT_URL = 'https://checkout.wompi.co/widget.js';

/** Opciones del widget de Wompi. */
export interface WompiCheckoutOptions {
  currency: string;
  amountInCents: number;
  reference: string;
  publicKey: string;
  signature: { integrity: string };
  redirectUrl?: string;
  customerData?: {
    email?: string;
    fullName?: string;
    phoneNumber?: string;
    phoneNumberPrefix?: string;
  };
  shippingAddress?: {
    addressLine1?: string;
    city?: string;
    phoneNumber?: string;
    region?: string;
    country?: string;
  };
}

/** Resultado entregado por el callback del widget. */
export interface WompiCheckoutResult {
  transaction?: {
    id?: string;
    status?: string;
    reference?: string;
    amount_in_cents?: number;
  };
}

/** Tipo del constructor global que expone el widget. */
interface WompiWidgetConstructor {
  new (options: WompiCheckoutOptions): {
    open(callback: (result: WompiCheckoutResult) => void): void;
  };
}

declare global {
  interface Window {
    WidgetCheckout?: WompiWidgetConstructor;
  }
}

@Injectable({
  providedIn: 'root'
})
export class WompiService {
  /** Promesa de carga del script (para no insertarlo dos veces). */
  private scriptPromise: Promise<WompiWidgetConstructor> | null = null;

  /**
   * Abre el widget de Wompi con los datos del backend.
   *
   * @param options datos firmados del checkout.
   * @returns el resultado que Wompi entrega al cerrar el widget.
   */
  public async open(options: WompiCheckoutOptions): Promise<WompiCheckoutResult> {
    const WidgetCheckout = await this.loadWidget();

    return new Promise<WompiCheckoutResult>(resolve => {
      const checkout = new WidgetCheckout({
        ...options,
        redirectUrl: options.redirectUrl
      });

      let settled = false;
      const finish = (result: WompiCheckoutResult): void => {
        if (settled) {
          return;
        }
        settled = true;
        resolve(result);
      };

      checkout.open(result => finish(result ?? {}));
    });
  }

  /**
   * Carga el script de Wompi una sola vez.
   *
   * @returns el constructor global del widget.
   */
  private loadWidget(): Promise<WompiWidgetConstructor> {
    if (typeof window === 'undefined') {
      return Promise.reject(new Error('El widget de Wompi solo está disponible en el navegador.'));
    }
    if (window.WidgetCheckout) {
      return Promise.resolve(window.WidgetCheckout);
    }
    if (this.scriptPromise) {
      return this.scriptPromise;
    }

    this.scriptPromise = new Promise<WompiWidgetConstructor>((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>(
        `script[src="${WIDGET_SCRIPT_URL}"]`
      );
      const script = existing ?? document.createElement('script');
      script.src = WIDGET_SCRIPT_URL;
      script.async = true;
      script.onload = () => {
        if (window.WidgetCheckout) {
          resolve(window.WidgetCheckout);
        } else {
          reject(new Error('El widget de Wompi no se inicializó.'));
        }
      };
      script.onerror = () => reject(new Error('No se pudo cargar el widget de Wompi.'));
      if (!existing) {
        document.head.appendChild(script);
      }
    });

    return this.scriptPromise;
  }
}
