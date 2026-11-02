/**
 * @file src/app/core/config/commerce.config.ts
 * @description Configuración comercial y de formato para la tienda en Colombia.
 *
 * Centraliza la moneda, el locale, los impuestos, el envío y el formato de
 * importes para que todas las pantallas usen los mismos valores.
 */

/** Locale usado en fechas e importes. */
export const APP_LOCALE = 'es-CO';

/** Código ISO 4217 de la moneda de la tienda. */
export const CURRENCY_CODE = 'COP';

/** Porcentaje de IVA incluido en los precios publicados (Colombia). */
export const INCLUDED_TAX_RATE = 0.19;

/**
 * Calcula la porción de IVA incluida en un importe.
 *
 * Los vendedores publican el precio final; este valor solo sirve para
 * discriminar el impuesto en la factura y nunca se suma al total.
 *
 * @param amount importe con IVA incluido.
 * @returns la parte correspondiente al impuesto.
 */
export function includedTax(amount: number): number {
  return Math.round(amount - amount / (1 + INCLUDED_TAX_RATE));
}

/** Coste de envío cuando no se alcanza el mínimo. */
export const SHIPPING_COST = 25_900;

/** Subtotal a partir del cual el envío es gratuito. */
export const FREE_SHIPPING_THRESHOLD = 400_000;

/** País propuesto por defecto en el checkout. */
export const DEFAULT_COUNTRY = 'Colombia';

/** Formateador de importes en pesos colombianos. */
const currencyFormatter = new Intl.NumberFormat(APP_LOCALE, {
  style: 'currency',
  currency: CURRENCY_CODE,
  minimumFractionDigits: 0,
  maximumFractionDigits: 0
});

/**
 * Formatea un importe en pesos colombianos.
 *
 * @param value importe a formatear.
 * @returns el importe con el símbolo y los separadores de Colombia.
 */
export function formatCurrency(value: number): string {
  return currencyFormatter.format(value);
}

/**
 * Formatea una fecha ISO en español de Colombia.
 *
 * @param isoDate fecha en formato ISO.
 * @returns la fecha con día, mes y año.
 */
export function formatDate(isoDate: string): string {
  return new Date(isoDate).toLocaleDateString(APP_LOCALE, {
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  });
}

/**
 * Formatea una fecha ISO con hora en español de Colombia.
 *
 * @param isoDate fecha en formato ISO.
 * @returns la fecha con día, mes, año y hora.
 */
export function formatDateTime(isoDate: string): string {
  return new Date(isoDate).toLocaleString(APP_LOCALE, {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
