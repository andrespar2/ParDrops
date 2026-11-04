package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminPaymentsResponse;
import com.pardorps.pardrops.model.FundsStatus;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.PaymentStatus;
import com.pardorps.pardrops.model.PlatformRevenueType;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.PlatformRevenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta de transacciones de Wompi para el panel de administración.
 *
 * <p>Muestra los pedidos con referencia de pago, su desglose y la distribución
 * por vendedor, además del total recaudado por el cargo fijo de servicio.</p>
 */
@Service
public class AdminPaymentsService {

    private final OrderRepository orderRepository;
    private final PlatformRevenueRepository revenueRepository;

    /**
     * @param orderRepository    acceso a los pedidos.
     * @param revenueRepository  ingresos propios de la plataforma.
     */
    public AdminPaymentsService(
            OrderRepository orderRepository,
            PlatformRevenueRepository revenueRepository
    ) {
        this.orderRepository = orderRepository;
        this.revenueRepository = revenueRepository;
    }

    /**
     * Construye el listado de transacciones y sus totales.
     *
     * @return el resumen de pagos.
     */
    @Transactional(readOnly = true)
    public AdminPaymentsResponse summary() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> order.getPaymentReference() != null)
                .toList();

        long approved = 0;
        long pending = 0;
        long rejected = 0;
        double commissions = 0.0;
        List<AdminPaymentsResponse.AdminPaymentTransaction> transactions = new ArrayList<>();

        for (Order order : orders) {
            PaymentStatus status = order.getPaymentStatus();
            if (status == PaymentStatus.APPROVED) {
                approved++;
            } else if (status == null || status == PaymentStatus.PENDING) {
                pending++;
            } else {
                rejected++;
            }

            double orderCommission = 0.0;
            List<AdminPaymentsResponse.SellerShare> sellers = new ArrayList<>();
            for (OrderSubOrder subOrder : order.getSubOrders()) {
                double commission = nz(subOrder.getCommissionAmount());
                if (status == PaymentStatus.APPROVED) {
                    orderCommission += commission;
                }
                sellers.add(new AdminPaymentsResponse.SellerShare(
                        subOrder.getStoreName(),
                        subOrder.getSeller() != null ? subOrder.getSeller().getFullName() : null,
                        subOrder.getSubtotal(),
                        commission,
                        subOrder.getSellerNetAmount(),
                        subOrder.getFundsStatus() == null
                                ? FundsStatus.HELD.name()
                                : subOrder.getFundsStatus().name()
                ));
            }
            commissions += orderCommission;

            transactions.add(new AdminPaymentsResponse.AdminPaymentTransaction(
                    order.getId(),
                    order.getPaymentReference(),
                    order.getWompiTransactionId(),
                    status == null ? null : status.name(),
                    order.getStatus() == null ? null : order.getStatus().name(),
                    order.getPaymentMethod(),
                    order.getPaymentEnvironment(),
                    order.getCustomerName(),
                    order.getCustomerEmail(),
                    order.getSubtotal(),
                    order.getShipping(),
                    order.getServiceFee(),
                    order.getTotal(),
                    orderCommission,
                    order.getCreatedAt(),
                    order.getPaidAt(),
                    sellers
            ));
        }

        return new AdminPaymentsResponse(
                orders.size(),
                approved,
                pending,
                rejected,
                nz(revenueRepository.sumByType(PlatformRevenueType.SERVICE_FEE)),
                round(commissions),
                transactions
        );
    }

    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
