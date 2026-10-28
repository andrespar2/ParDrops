package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.EmailTemplate;
import com.pardorps.pardrops.model.EmailTemplateCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las plantillas de correo.
 */
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    /**
     * Busca una plantilla por su código.
     *
     * @param code código del correo.
     * @return la plantilla si existe.
     */
    Optional<EmailTemplate> findByCode(EmailTemplateCode code);

    /**
     * Comprueba si existe una plantilla con ese código.
     *
     * @param code código del correo.
     * @return {@code true} si ya existe.
     */
    boolean existsByCode(EmailTemplateCode code);

    /**
     * Devuelve todas las plantillas ordenadas por código.
     *
     * @return listado de plantillas.
     */
    List<EmailTemplate> findAllByOrderByCodeAsc();
}
