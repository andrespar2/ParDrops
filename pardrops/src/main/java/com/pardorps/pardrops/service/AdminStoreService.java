package com.pardorps.pardrops.service;

import com.pardorps.pardrops.cache.CacheInvalidator;
import com.pardorps.pardrops.dto.BulkActionResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Acciones administrativas sobre tiendas.
 *
 * <p>La eliminación es lógica: la tienda pasa a {@link StoreStatus#DELETED} y
 * deja de aparecer en el marketplace y en los listados operativos. Los pedidos,
 * productos, saldos y el historial se conservan. Esto permite una acción en
 * masa rápida y reversible sin romper claves foráneas ni datos históricos.</p>
 */
@Service
public class AdminStoreService {

    private final StoreRepository storeRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final CacheInvalidator cacheInvalidator;
    private final ProductRankingService rankingService;

    public AdminStoreService(
            StoreRepository storeRepository,
            AuditLogService auditLogService,
            NotificationService notificationService,
            CacheInvalidator cacheInvalidator,
            ProductRankingService rankingService
    ) {
        this.storeRepository = storeRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.cacheInvalidator = cacheInvalidator;
        this.rankingService = rankingService;
    }

    /**
     * Elimina lógicamente una o varias tiendas.
     *
     * @param storeIds tiendas seleccionadas.
     * @param admin    administrador que ejecuta la acción.
     * @return resultado con cuántas se modificaron y cuántas ya estaban eliminadas.
     */
    @Transactional
    public BulkActionResponse deleteStores(List<Long> storeIds, User admin) {
        List<Long> ids = distinctIds(storeIds);
        List<Store> stores = storeRepository.findAllById(ids);
        if (stores.size() != ids.size()) {
            List<Long> found = stores.stream().map(Store::getId).toList();
            List<Long> missing = ids.stream().filter(id -> !found.contains(id)).toList();
            throw ApiException.notFound("No se encontraron las tiendas: " + missing);
        }

        int updated = 0;
        int skipped = 0;
        for (Store store : stores) {
            if (store.getStatus() == StoreStatus.DELETED) {
                skipped++;
                continue;
            }
            store.setStatus(StoreStatus.DELETED);
            storeRepository.save(store);
            updated++;
            auditLogService.record(admin, AuditAction.STORE_DELETED, "STORE",
                    String.valueOf(store.getId()),
                    "Tienda eliminada: " + store.getName());
            notifySeller(store, true);
        }

        if (updated > 0) {
            invalidateCatalog();
        }
        return BulkActionResponse.of(ids.size(), updated, skipped);
    }

    /**
     * Restaura una tienda eliminada.
     *
     * @param storeId tienda seleccionada.
     * @param admin   administrador que ejecuta la acción.
     * @return resultado de la restauración.
     */
    @Transactional
    public BulkActionResponse restoreStore(Long storeId, User admin) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> ApiException.notFound("Tienda no encontrada: " + storeId));
        if (store.getStatus() != StoreStatus.DELETED) {
            return BulkActionResponse.of(1, 0, 1);
        }

        boolean sellerActive = store.getSeller() != null
                && store.getSeller().getStatus() == UserStatus.ACTIVE;
        store.setStatus(sellerActive ? StoreStatus.ACTIVE : StoreStatus.SUSPENDED);
        storeRepository.save(store);
        auditLogService.record(admin, AuditAction.STORE_RESTORED, "STORE",
                String.valueOf(store.getId()), "Tienda restaurada: " + store.getName());
        notifySeller(store, false);
        invalidateCatalog();
        return BulkActionResponse.of(1, 1, 0);
    }

    private void notifySeller(Store store, boolean deleted) {
        if (store.getSeller() == null) {
            return;
        }
        notificationService.notify(
                store.getSeller(),
                NotificationType.ACCOUNT,
                deleted ? "Tu tienda fue eliminada" : "Tu tienda fue restaurada",
                deleted
                        ? "La administración eliminó la tienda \"" + store.getName()
                                + "\". Sus productos ya no se publican. Contacta con soporte si crees que es un error."
                        : "La administración restauró la tienda \"" + store.getName()
                                + "\". Revisa su estado y sus productos.",
                deleted ? null : "/vendedor");
    }

    private void invalidateCatalog() {
        cacheInvalidator.invalidateCatalog();
        rankingService.invalidate();
    }

    private List<Long> distinctIds(List<Long> ids) {
        List<Long> normalized = new ArrayList<>();
        for (Long id : ids) {
            if (id != null && !normalized.contains(id)) {
                normalized.add(id);
            }
        }
        if (normalized.isEmpty()) {
            throw ApiException.badRequest("Selecciona al menos una tienda");
        }
        return normalized;
    }
}
