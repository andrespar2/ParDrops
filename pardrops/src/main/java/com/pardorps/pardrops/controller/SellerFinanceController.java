package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CreateWithdrawalRequest;
import com.pardorps.pardrops.dto.SellerFinanceResponse;
import com.pardorps.pardrops.dto.WithdrawalResponse;
import com.pardorps.pardrops.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Saldo, movimientos y retiros del vendedor autenticado. */
@RestController
@RequestMapping("/api/seller")
public class SellerFinanceController {

    private final WithdrawalService withdrawalService;

    /**
     * @param withdrawalService servicio financiero.
     */
    public SellerFinanceController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    /** Resumen financiero del vendedor. */
    @GetMapping("/finance")
    public SellerFinanceResponse finance() {
        return withdrawalService.finance();
    }

    /** Solicitudes de retiro del vendedor. */
    @GetMapping("/withdrawals")
    public List<WithdrawalResponse> mine() {
        return withdrawalService.mine();
    }

    /** Crea una solicitud de retiro. */
    @PostMapping("/withdrawals")
    public ResponseEntity<WithdrawalResponse> create(
            @Valid @RequestBody CreateWithdrawalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(withdrawalService.create(request));
    }
}
