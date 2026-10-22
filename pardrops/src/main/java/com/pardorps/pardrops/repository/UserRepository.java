package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Devuelve todos los usuarios con un rol concreto.
     *
     * @param role rol buscado.
     * @return listado de usuarios.
     */
    List<User> findByRole(Role role);

    /**
     * Devuelve todos los usuarios con un estado concreto.
     *
     * @param status estado buscado.
     * @return listado de usuarios.
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Busca usuarios cuyo correo, nombre o teléfono contengan el texto.
     *
     * <p>La búsqueda no distingue mayúsculas de minúsculas y se usa desde el
     * panel de administración para filtrar la lista de cuentas.</p>
     *
     * @param term texto de búsqueda.
     * @return listado de usuarios que coinciden con el filtro.
     */
    @Query("select u from User u where "
            + "lower(u.email) like lower(concat('%', :term, '%')) "
            + "or lower(u.fullName) like lower(concat('%', :term, '%')) "
            + "or lower(coalesce(u.phone, '')) like lower(concat('%', :term, '%')) "
            + "order by u.fullName asc")
    List<User> search(@Param("term") String term);
}
