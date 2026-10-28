package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Busca una cuenta por su proveedor de autenticación e identificador
     * externo.
     *
     * <p>Para Google, {@code providerId} corresponde al valor {@code sub} del
     * token de identidad. Permite reconocer la cuenta aunque el correo cambie
     * en el proveedor.</p>
     *
     * @param authProvider proveedor de autenticación.
     * @param providerId   identificador de la cuenta en el proveedor.
     * @return la cuenta vinculada si existe.
     */
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

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

    /**
     * Busca cuentas aplicando los filtros del panel de administración.
     *
     * <p>Todos los filtros son opcionales: si llegan nulos o vacíos no se
     * aplican. La búsqueda por texto recorre correo, nombre y teléfono.</p>
     *
     * @param search texto de búsqueda (puede ser nulo).
     * @param role   filtra por rol (puede ser nulo).
     * @param status filtra por estado (puede ser nulo).
     * @param pageable paginación y orden.
     * @return página de cuentas que cumplen los filtros.
     */
    @Query("select u from User u where "
            + "(:search is null "
            + "   or lower(u.email) like lower(concat('%', :search, '%')) "
            + "   or lower(u.fullName) like lower(concat('%', :search, '%')) "
            + "   or lower(coalesce(u.phone, '')) like lower(concat('%', :search, '%'))) "
            + "and (:role is null or u.role = :role) "
            + "and (:status is null or u.status = :status)")
    Page<User> searchForAdmin(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("status") UserStatus status,
            Pageable pageable);

    /**
     * Cuenta las cuentas que tienen un rol concreto.
     *
     * @param role rol buscado.
     * @return número de cuentas.
     */
    long countByRole(Role role);

    /**
     * Cuenta las cuentas que tienen un estado concreto.
     *
     * @param status estado buscado.
     * @return número de cuentas.
     */
    long countByStatus(UserStatus status);
}
