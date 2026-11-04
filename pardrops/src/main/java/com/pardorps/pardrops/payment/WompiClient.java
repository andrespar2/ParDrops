package com.pardorps.pardrops.payment;

/**
 * Cliente de consulta de transacciones de Wompi.
 *
 * <p>La verificación remota es la fuente de verdad: la URL de retorno y el
 * cuerpo de un evento nunca confirman un pago por sí solos.</p>
 */
public interface WompiClient {

    /**
     * Consulta una transacción directamente a Wompi usando la llave privada.
     *
     * @param transactionId identificador de la transacción.
     * @return los datos de la transacción.
     * @throws WompiClientException si la consulta falla.
     */
    WompiTransaction fetchTransaction(String transactionId);
}
