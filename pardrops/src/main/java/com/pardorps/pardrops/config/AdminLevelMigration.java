package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.AdminLevel;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Garantiza que siempre exista un administrador principal.
 *
 * <p>Las cuentas administrativas anteriores a la incorporaciÃ³n de niveles
 * quedan como {@link AdminLevel#MANAGER}; la mÃ¡s antigua activa pasa a
 * {@link AdminLevel#OWNER} si todavÃ­a no hay propietario. Es idempotente.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AdminLevelMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminLevelMigration.class);

    private final UserRepository userRepository;

    /**
     * @param userRepository acceso a las cuentas.
     */
    public AdminLevelMigration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .sorted(Comparator.comparing(User::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (admins.isEmpty()) {
            return;
        }

        boolean hasOwner = admins.stream()
                .anyMatch(user -> user.getAdminLevel() == AdminLevel.OWNER
                        && user.getStatus() == UserStatus.ACTIVE);

        if (!hasOwner) {
            User candidate = admins.stream()
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .findFirst()
                    .orElse(admins.get(0));
            candidate.setAdminLevel(AdminLevel.OWNER);
            candidate.setStatus(UserStatus.ACTIVE);
            userRepository.save(candidate);
            log.info("Administrador principal asignado: {}", candidate.getEmail());
        }

        for (User admin : admins) {
            if (admin.getAdminLevel() == null) {
                admin.setAdminLevel(AdminLevel.MANAGER);
                userRepository.save(admin);
                log.info("Administrador {} marcado como MANAGER", admin.getEmail());
            }
        }
    }
}

