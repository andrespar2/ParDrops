package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateWithdrawalRequest;
import com.pardorps.pardrops.dto.LedgerEntryResponse;
import com.pardorps.pardrops.dto.SellerFinanceResponse;
import com.pardorps.pardrops.dto.WithdrawalResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.LedgerEntryType;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.SellerBalance;
import com.pardorps.pardrops.model.SellerLedgerEntry;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.WithdrawalMethod;
import com.pardorps.pardrops.model.WithdrawalRequest;
import com.pardorps.pardrops.model.WithdrawalStatus;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import com.pardorps.pardrops.repository.SellerLedgerEntryRepository;
import com.pardorps.pardrops.repository.UserRepository;
import com.pardorps.pardrops.repository.WithdrawalRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Retiros del saldo de los vendedores y su historial financiero.
 *
 * <p>El dinero se reserva al solicitar el retiro, se descuenta al completarlo y
 * se libera si se rechaza. Cada cambio queda en el ledger.</p>
 */
@Service
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRepository;
    private final SellerBalanceRepository balanceRepository;
    private final SellerLedgerEntryRepository ledgerRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AdminAlertService adminAlertService;
    private final double minAmount;
    private final double bankFee;

    /**
     * @param withdrawalRepository acceso a las solicitudes.
     * @param balanceRepository    acceso a los saldos.
     * @param ledgerRepository     acceso al ledger.
     * @param userRepository       acceso a las cuentas.
     * @param authService          usuario autenticado.
     * @param notificationService  avisos internos.
     * @param minAmount            valor mínimo de retiro.
     * @param bankFee              comisión por transferencia bancaria.
     */
    public WithdrawalService(
            WithdrawalRequestRepository withdrawalRepository,
            SellerBalanceRepository balanceRepository,
            SellerLedgerEntryRepository ledgerRepository,
            UserRepository userRepository,
            AuthService authService,
            AdminAlertService adminAlertService,
            @Value("${app.withdrawals.min-amount:50000}") double minAmount,
            @Value("${app.withdrawals.bank-fee:0}") double bankFee
    ) {
        this.withdrawalRepository = withdrawalRepository;
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.adminAlertService = adminAlertService;
        this.minAmount = minAmount;
        this.bankFee = bankFee;
    }

    /**
     * Resumen financiero del vendedor autenticado.
     *
     * @return saldo, movimientos y solicitudes.
     */
    @Transactional(readOnly = true)
    public SellerFinanceResponse finance() {
        User seller = requireSeller();
        SellerBalance balance = balance(seller);
        List<LedgerEntryResponse> movements = ledgerRepository
                .findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream()
                .map(this::toResponse)
                .toList();
        List<WithdrawalResponse> withdrawals = withdrawalRepository
                .findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream()
                .map(this::toResponse)
                .toList();

        return new SellerFinanceResponse(
                nz(balance.getAvailableAmount()),
                nz(balance.getPendingAmount()),
                nz(balance.getReservedAmount()),
                nz(balance.getWithdrawnTotal()),
                nz(balance.getTotalSales()),
                nz(balance.getCommissionTotal()),
                movements,
                withdrawals,
                minAmount,
                bankFee
        );
    }

    /**
     * Crea una solicitud de retiro y reserva el dinero.
     *
     * @param request datos del retiro.
     * @return la solicitud creada.
     */
    @Transactional
    public WithdrawalResponse create(CreateWithdrawalRequest request) {
        User seller = requireSeller();
        SellerBalance balance = balance(seller);

        double amount = round(request.amount());
        if (amount < minAmount) {
            throw ApiException.badRequest("El valor mínimo de retiro es " + (long) minAmount);
        }
        if (amount > nz(balance.getAvailableAmount())) {
            throw ApiException.badRequest("No tienes saldo disponible suficiente");
        }
        if (withdrawalRepository.existsBySellerIdAndAmountAndStatusIn(
                seller.getId(), amount,
                List.of(WithdrawalStatus.PENDING, WithdrawalStatus.REVIEW, WithdrawalStatus.PROCESSING))) {
            throw ApiException.conflict("Ya tienes una solicitud por ese valor en curso");
        }

        validateMethod(request);

        double fee = request.method() == WithdrawalMethod.BANK_ACCOUNT ? round(bankFee) : 0.0;

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setSeller(seller);
        withdrawal.setAmount(amount);
        withdrawal.setFee(fee);
        withdrawal.setNetAmount(round(amount - fee));
        withdrawal.setMethod(request.method());
        withdrawal.setBreBKey(trim(request.breBKey()));
        withdrawal.setBankName(trim(request.bankName()));
        withdrawal.setAccountType(trim(request.accountType()));
        withdrawal.setAccountNumber(trim(request.accountNumber()));
        withdrawal.setAccountHolder(trim(request.accountHolder()));
        withdrawal.setNotes(trim(request.notes()));
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);

        balance.setAvailableAmount(round(nz(balance.getAvailableAmount()) - amount));
        balance.setReservedAmount(round(nz(balance.getReservedAmount()) + amount));
        balanceRepository.save(balance);

        ledger(balance, LedgerEntryType.WITHDRAWAL_RESERVED, "RET-" + saved.getId(),
                "Retiro solicitado", 0.0, fee, balance.getAvailableAmount());

        adminAlertService.notifyUser(seller, NotificationType.ACCOUNT, "Solicitud de retiro registrada",
                "Tu solicitud por " + amount + " quedó reservada y está pendiente de revisión.",
                "/vendedor/saldo");
        adminAlertService.notifyAdmins("Nueva solicitud de retiro",
                seller.getFullName() + " solicitó un retiro por " + amount + ".", "/admin/retiros");

        return toResponse(saved);
    }

    /**
     * Solicitudes del vendedor autenticado.
     *
     * @return solicitudes ordenadas por fecha.
     */
    @Transactional(readOnly = true)
    public List<WithdrawalResponse> mine() {
        User seller = requireSeller();
        return withdrawalRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Todas las solicitudes para administración.
     *
     * @return solicitudes ordenadas por fecha.
     */
    @Transactional(readOnly = true)
    public List<WithdrawalResponse> all() {
        requireStaff();
        return withdrawalRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cambia el estado de una solicitud sin mover dinero.
     *
     * @param id     identificador.
     * @param status nuevo estado.
     * @param notes  observaciones.
     * @return la solicitud actualizada.
     */
    @Transactional
    public WithdrawalResponse updateStatus(Long id, WithdrawalStatus status, String notes) {
        User admin = requireStaff();
        WithdrawalRequest withdrawal = find(id);
        ensureOpen(withdrawal);

        if (status == WithdrawalStatus.COMPLETED || status == WithdrawalStatus.REJECTED) {
            throw ApiException.badRequest("Usa las acciones de completar o rechazar");
        }

        withdrawal.setStatus(status);
        withdrawal.setNotes(trim(notes));
        if (admin != null) {
            withdrawal.setResolvedBy(admin);
        }
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);

        adminAlertService.notifyUser(withdrawal.getSeller(), NotificationType.ACCOUNT,
                "Solicitud de retiro en " + status.name().toLowerCase(),
                "Tu solicitud de retiro cambió de estado.", "/vendedor/saldo");
        return toResponse(saved);
    }

    /**
     * Completa una solicitud y descuenta definitivamente el dinero.
     *
     * @param id        identificador.
     * @param reference referencia de la transferencia.
     * @return la solicitud completada.
     */
    @Transactional
    public WithdrawalResponse complete(Long id, String reference) {
        User admin = requireStaff();
        if (reference == null || reference.isBlank()) {
            throw ApiException.badRequest("Indica la referencia de la transferencia");
        }
        WithdrawalRequest withdrawal = find(id);
        ensureOpen(withdrawal);

        SellerBalance balance = balance(withdrawal.getSeller());
        double amount = nz(withdrawal.getAmount());
        if (amount > nz(balance.getReservedAmount())) {
            throw ApiException.badRequest("El saldo reservado no coincide con la solicitud");
        }

        balance.setReservedAmount(round(nz(balance.getReservedAmount()) - amount));
        balance.setWithdrawnTotal(round(nz(balance.getWithdrawnTotal()) + amount));
        balanceRepository.save(balance);

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setTransferReference(reference.trim());
        withdrawal.setResolvedBy(admin);
        withdrawal.setResolvedAt(Instant.now());
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);

        ledger(balance, LedgerEntryType.WITHDRAWAL_COMPLETED, "RET-" + saved.getId(),
                "Retiro completado", -amount, nz(withdrawal.getFee()), balance.getAvailableAmount());

        adminAlertService.notifyUser(withdrawal.getSeller(), NotificationType.ACCOUNT,
                "Retiro completado",
                "Transferimos " + withdrawal.getNetAmount() + " con referencia " + saved.getTransferReference() + ".",
                "/vendedor/saldo");
        return toResponse(saved);
    }

    /**
     * Rechaza una solicitud y devuelve el dinero al saldo disponible.
     *
     * @param id    identificador.
     * @param notes motivo del rechazo.
     * @return la solicitud rechazada.
     */
    @Transactional
    public WithdrawalResponse reject(Long id, String notes) {
        User admin = requireStaff();
        WithdrawalRequest withdrawal = find(id);
        ensureOpen(withdrawal);

        SellerBalance balance = balance(withdrawal.getSeller());
        double amount = nz(withdrawal.getAmount());
        balance.setReservedAmount(round(Math.max(0.0, nz(balance.getReservedAmount()) - amount)));
        balance.setAvailableAmount(round(nz(balance.getAvailableAmount()) + amount));
        balanceRepository.save(balance);

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setNotes(trim(notes));
        withdrawal.setResolvedBy(admin);
        withdrawal.setResolvedAt(Instant.now());
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);

        ledger(balance, LedgerEntryType.WITHDRAWAL_RELEASED, "RET-" + saved.getId(),
                "Retiro rechazado", amount, 0.0, balance.getAvailableAmount());

        adminAlertService.notifyUser(withdrawal.getSeller(), NotificationType.ACCOUNT,
                "Solicitud de retiro rechazada",
                "El dinero volvió a tu saldo disponible." + (notes == null ? "" : " Motivo: " + notes),
                "/vendedor/saldo");
        return toResponse(saved);
    }

    /** Valida los datos del método de retiro. */
    private void validateMethod(CreateWithdrawalRequest request) {
        if (request.method() == WithdrawalMethod.BRE_B) {
            if (request.breBKey() == null || request.breBKey().isBlank()) {
                throw ApiException.badRequest("Indica tu llave Bre-B");
            }
        } else {
            if (request.bankName() == null || request.bankName().isBlank()
                    || request.accountNumber() == null || request.accountNumber().isBlank()
                    || request.accountHolder() == null || request.accountHolder().isBlank()) {
                throw ApiException.badRequest("Completa los datos de la cuenta bancaria");
            }
        }
    }

    /** Exige vendedor autenticado. */
    private User requireSeller() {
        User user = authService.getCurrentUser();
        if (user.getRole() != Role.SELLER) {
            throw ApiException.forbidden("Solo los vendedores pueden gestionar retiros");
        }
        return user;
    }

    /** Exige personal administrativo. */
    private User requireStaff() {
        User user = authService.getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Solo la administración puede gestionar retiros");
        }
        return user;
    }

    /** Obtiene el saldo o lo crea. */
    private SellerBalance balance(User seller) {
        return balanceRepository.findBySellerId(seller.getId()).orElseGet(() -> {
            SellerBalance created = new SellerBalance();
            created.setSeller(seller);
            return created;
        });
    }

    /** Busca una solicitud. */
    private WithdrawalRequest find(Long id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La solicitud no existe"));
    }

    /** Impide operar sobre solicitudes ya cerradas. */
    private void ensureOpen(WithdrawalRequest withdrawal) {
        if (withdrawal.getStatus() == WithdrawalStatus.COMPLETED
                || withdrawal.getStatus() == WithdrawalStatus.REJECTED) {
            throw ApiException.conflict("La solicitud ya está cerrada");
        }
    }

    /** Registra un movimiento. */
    private void ledger(SellerBalance balance, LedgerEntryType type, String reference,
                        String description, double amount, double commission, double balanceAfter) {
        SellerLedgerEntry entry = new SellerLedgerEntry();
        entry.setSeller(balance.getSeller());
        entry.setType(type);
        entry.setReference(reference);
        entry.setDescription(description);
        entry.setAmount(round(amount));
        entry.setCommission(round(commission));
        entry.setBalanceAfter(round(balanceAfter));
        ledgerRepository.save(entry);
    }

    /** Convierte el movimiento en su respuesta. */
    private LedgerEntryResponse toResponse(SellerLedgerEntry entry) {
        return new LedgerEntryResponse(entry.getId(), entry.getType(), entry.getReference(),
                entry.getDescription(), entry.getAmount(), entry.getCommission(),
                entry.getBalanceAfter(), entry.getCreatedAt());
    }

    /** Convierte la solicitud en su respuesta, ocultando datos sensibles. */
    private WithdrawalResponse toResponse(WithdrawalRequest withdrawal) {
        return new WithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getSeller().getFullName(),
                withdrawal.getAmount(),
                withdrawal.getFee(),
                withdrawal.getNetAmount(),
                withdrawal.getMethod(),
                mask(withdrawal.getBreBKey()),
                withdrawal.getBankName(),
                withdrawal.getAccountType(),
                mask(withdrawal.getAccountNumber()),
                withdrawal.getAccountHolder(),
                withdrawal.getStatus(),
                withdrawal.getNotes(),
                withdrawal.getTransferReference(),
                withdrawal.getCreatedAt(),
                withdrawal.getResolvedAt()
        );
    }

    /** Oculta parcialmente un dato sensible. */
    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    /** Normaliza un texto opcional. */
    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Redondea a dos decimales. */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Valor no nulo. */
    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }
}
