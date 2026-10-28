package com.pardorps.pardrops.service;

import com.pardorps.pardrops.model.FundsStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.SellerBalance;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Mantiene el saldo retenido y disponible de los vendedores.
 *
 * <p>Los métodos son idempotentes: liberar dos veces la misma parte no duplica
 * el abono, y cancelar una parte ya liberada no la revierte.</p>
 */
@Service
public class SellerBalanceService {

    private static final Logger log = LoggerFactory.getLogger(SellerBalanceService.class);

    private final SellerBalanceRepository balanceRepository;
    private final OrderSubOrderRepository subOrderRepository;

    /**
     * @param balanceRepository  acceso a los saldos.
     * @param subOrderRepository acceso a las partes de pedido.
     */
    public SellerBalanceService(
            SellerBalanceRepository balanceRepository,
            OrderSubOrderRepository subOrderRepository
    ) {
        this.balanceRepository = balanceRepository;
        this.subOrderRepository = subOrderRepository;
    }

    /**
     * Retiene el neto de una venta recién creada.
     *
     * @param subOrder parte del pedido.
     */
    @Transactional
    public void hold(OrderSubOrder subOrder) {
        if (subOrder.getSeller() == null || subOrder.getSellerNetAmount() == null) {
            return;
        }
        if (subOrder.getFundsStatus() == null) {
            subOrder.setFundsStatus(FundsStatus.HELD);
        }
        if (subOrder.getFundsStatus() != FundsStatus.HELD) {
            return;
        }
        SellerBalance balance = getOrCreate(subOrder);
        balance.setPendingAmount(round(balance.getPendingAmount() + subOrder.getSellerNetAmount()));
        balance.setTotalSales(round(balance.getTotalSales() + nz(subOrder.getSubtotal())));
        balanceRepository.save(balance);
        subOrderRepository.save(subOrder);
        log.info("Saldo retenido al vendedor {}: {}", subOrder.getSeller().getId(),
                subOrder.getSellerNetAmount());
    }

    /**
     * Libera el neto al vendedor cuando su parte se entrega.
     *
     * @param subOrder parte entregada.
     */
    @Transactional
    public void release(OrderSubOrder subOrder) {
        if (subOrder.getSeller() == null || subOrder.getFundsStatus() == null) {
            return;
        }
        if (subOrder.getFundsStatus() != FundsStatus.HELD) {
            return;
        }
        SellerBalance balance = getOrCreate(subOrder);
        double net = nz(subOrder.getSellerNetAmount());
        balance.setPendingAmount(round(Math.max(0.0, balance.getPendingAmount() - net)));
        balance.setAvailableAmount(round(balance.getAvailableAmount() + net));
        balance.setCommissionTotal(round(balance.getCommissionTotal() + nz(subOrder.getCommissionAmount())));
        balanceRepository.save(balance);

        subOrder.setFundsStatus(FundsStatus.RELEASED);
        subOrder.setReleasedAt(Instant.now());
        subOrderRepository.save(subOrder);
        log.info("Saldo liberado al vendedor {}: {}", subOrder.getSeller().getId(), net);
    }

    /**
     * Anula la retención cuando la parte se cancela antes de entregarse.
     *
     * @param subOrder parte cancelada.
     */
    @Transactional
    public void cancel(OrderSubOrder subOrder) {
        if (subOrder.getSeller() == null || subOrder.getFundsStatus() != FundsStatus.HELD) {
            return;
        }
        SellerBalance balance = getOrCreate(subOrder);
        double net = nz(subOrder.getSellerNetAmount());
        balance.setPendingAmount(round(Math.max(0.0, balance.getPendingAmount() - net)));
        balance.setTotalSales(round(Math.max(0.0, balance.getTotalSales() - nz(subOrder.getSubtotal()))));
        balanceRepository.save(balance);

        subOrder.setFundsStatus(FundsStatus.CANCELLED);
        subOrderRepository.save(subOrder);
        log.info("Retención anulada al vendedor {}: {}", subOrder.getSeller().getId(), net);
    }

    /**
     * Obtiene el saldo del vendedor o lo crea si es la primera venta.
     *
     * @param subOrder parte con vendedor.
     * @return el saldo del vendedor.
     */
    private SellerBalance getOrCreate(OrderSubOrder subOrder) {
        return balanceRepository.findBySellerId(subOrder.getSeller().getId())
                .orElseGet(() -> {
                    SellerBalance balance = new SellerBalance();
                    balance.setSeller(subOrder.getSeller());
                    return balance;
                });
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }
}
