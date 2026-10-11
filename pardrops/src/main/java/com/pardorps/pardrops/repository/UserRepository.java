package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a los usuarios.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param email correo a buscar.
     * @return el usuario si existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Comprueba si ya existe una cuenta con ese correo.
     *
     * @param email correo a comprobar.
     * @return {@code true} si ya está registrado.
     */
    boolean existsByEmail(String email);
}
