/**
 * @file src/app/core/models/order.model.spec.ts
 * @description Pruebas de las etiquetas y transiciones de los estados de pedido.
 */

import { OrderStatus, allowedOrderTransitions, orderStatusLabel } from './order.model';

describe('modelo de pedidos', () => {
  const STATUSES: OrderStatus[] = [
    'PENDING',
    'PAID',
    'PREPARED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
  ];

  it('etiqueta todos los estados en español', () => {
    STATUSES.forEach(status => {
      expect(orderStatusLabel(status)).toBeTruthy();
    });

    expect(orderStatusLabel('PREPARED')).toBe('Preparado');
    expect(orderStatusLabel('SHIPPED')).toBe('Enviado');
    expect(orderStatusLabel('DELIVERED')).toBe('Entregado');
  });

  it('permite avanzar desde el estado pendiente', () => {
    const transitions = allowedOrderTransitions('PENDING');

    expect(transitions).toContain('PREPARED');
    expect(transitions).toContain('SHIPPED');
    expect(transitions).toContain('CANCELLED');
    expect(transitions).not.toContain('PENDING');
  });

  it('encadena preparado, enviado y entregado', () => {
    expect(allowedOrderTransitions('PREPARED')).toEqual(['SHIPPED', 'CANCELLED']);
    expect(allowedOrderTransitions('SHIPPED')).toEqual(['DELIVERED', 'CANCELLED']);
  });

  it('no permite cambios desde los estados finales', () => {
    expect(allowedOrderTransitions('DELIVERED')).toEqual([]);
    expect(allowedOrderTransitions('CANCELLED')).toEqual([]);
  });
});
