package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.WithdrawalCompleteRequest;
import com.pardorps.pardrops.dto.WithdrawalRejectRequest;
import com.pardorps.pardrops.dto.WithdrawalResponse;
import com.pardorps.pardrops.dto.WithdrawalStatusRequest;
import com.pardorps.pardrops.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestión administrativa de solicitudes de retiro. */
@RestController
@RequestMapping("/api/admin/withdrawals")
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;

    /**
     * @param withdrawalService servicio financiero.
     */
    public AdminWithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    /** Todas las solicitudes. */
    @GetMapping
    public List<WithdrawalResponse> list() {
        return withdrawalService.all();
    }

    /** Cambia el estado (revisión, proceso...). */
    @PatchMapping("/{id}/status")
    public WithdrawalResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalStatusRequest request
    ) {
        return withdrawalService.updateStatus(id, request.status(), request.notes());
    }

    /** Completa la solicitud con la referencia de la transferencia. */
    @PatchMapping("/{id}/complete")
    public WithdrawalResponse complete(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalCompleteRequest request
    ) {
        return withdrawalService.complete(id, request.reference());
    }

    /** Rechaza la solicitud y libera el dinero. */
    @PatchMapping("/{id}/reject")
    public WithdrawalResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalRejectRequest request
    ) {
        return withdrawalService.reject(id, request.notes());
    }
}
