package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderItem;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductVariant;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migra los datos creados antes de adoptar el peso colombiano como moneda.
 *
 * <p>Los productos y pedidos antiguos se guardaron en EUR. Al arrancar, esta
 * migración convierte sus importes a COP y actualiza la moneda. Es idempotente:
 * los productos y pedidos que ya están en COP no se modifican.</p>
 */
@Component
public class CurrencyMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CurrencyMigration.class);

    /** Tasa aproximada usada para convertir los importes históricos. */
    private static final double EUR_TO_COP = 4_300.0;

    /** IVA vigente en Colombia. */
    private static final double TAX_RATE = 0.19;

    /** Coste de envío en COP cuando el pedido no tenía envío gratuito. */
    private static final double SHIPPING_COST = 25_900.0;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    /**
     * @param productRepository acceso al catálogo.
     * @param orderRepository   acceso a los pedidos.
     */
    public CurrencyMigration(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Convierte los productos y pedidos que todavía están en EUR.
     *
     * @param args argumentos de línea de comandos (no se usan).
     */
    @Override
    @Transactional
    public void run(String... args) {
        int migratedProducts = migrateProducts();
        int migratedOrders = migrateOrders();

        if (migratedProducts > 0 || migratedOrders > 0) {
            log.info("Datos migrados de EUR a COP: {} productos y {} pedidos.",
                    migratedProducts, migratedOrders);
        }
    }

    /**
     * Convierte los precios de los productos que siguen en EUR.
     *
     * @return número de productos migrados.
     */
    private int migrateProducts() {
        int migrated = 0;

        for (Product product : productRepository.findAll()) {
            if (!"EUR".equalsIgnoreCase(product.getCurrency())) {
                continue;
            }

            product.setPrice(roundToHundred(product.getPrice() * EUR_TO_COP));
            if (product.getOriginalPrice() != null) {
                product.setOriginalPrice(roundToHundred(product.getOriginalPrice() * EUR_TO_COP));
            }
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    if (variant.getPrice() != null) {
                        variant.setPrice(roundToHundred(variant.getPrice() * EUR_TO_COP));
                    }
                }
            }
            product.setCurrency("COP");
            productRepository.save(product);
            migrated++;
        }

        return migrated;
    }

    /**
     * Convierte los pedidos históricos que no tienen moneda o siguen en EUR.
     *
     * @return número de pedidos migrados.
     */
    private int migrateOrders() {
        int migrated = 0;

        for (Order order : orderRepository.findAll()) {
            if (order.getCurrency() != null && !"EUR".equalsIgnoreCase(order.getCurrency())) {
                continue;
            }

            double subtotal = 0.0;
            for (OrderItem item : order.getItems()) {
                double unitPrice = roundToHundred(item.getUnitPrice() * EUR_TO_COP);
                double lineSubtotal = roundToHundred(unitPrice * item.getQuantity());
                item.setUnitPrice(unitPrice);
                item.setSubtotal(lineSubtotal);
                subtotal += lineSubtotal;
            }

            subtotal = roundToHundred(subtotal);
            double tax = roundToHundred(subtotal * TAX_RATE);
            double shipping = order.getShipping() != null && order.getShipping() == 0.0
                    ? 0.0
                    : SHIPPING_COST;

            order.setSubtotal(subtotal);
            order.setTax(tax);
            order.setShipping(shipping);
            order.setTotal(roundToHundred(subtotal + tax + shipping));
            order.setCurrency("COP");
            orderRepository.save(order);
            migrated++;
        }

        return migrated;
    }

    /**
     * Redondea un importe a la centena más cercana, como es habitual en COP.
     *
     * @param value importe a redondear.
     * @return el importe redondeado.
     */
    private double roundToHundred(double value) {
        return Math.round(value / 100.0) * 100.0;
    }
}
