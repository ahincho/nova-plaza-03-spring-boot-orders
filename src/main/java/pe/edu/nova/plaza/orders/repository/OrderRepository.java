package pe.edu.nova.plaza.orders.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.nova.plaza.orders.entity.Order;

/** Los pedidos guardados. */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Busca un pedido con sus líneas.
     *
     * @param id el identificador
     * @return el pedido, si existe
     */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    /**
     * Una página de los pedidos de un cliente, por keyset (ADR-054). Pagina solo los pedidos: sus líneas se cargan
     * después, por lotes, porque un fetch join haría que Hibernate aplicara el límite en memoria.
     *
     * @param customerId el cliente
     * @param position desde dónde sigue
     * @param limit cuántos pedidos trae
     * @param sort el orden, que termina en el id
     * @return la página
     */
    Window<Order> findByCustomerId(String customerId, ScrollPosition position, Limit limit, Sort sort);

    /**
     * Cuántos pedidos tiene un cliente.
     *
     * @param customerId el cliente
     * @return la cantidad
     */
    long countByCustomerId(String customerId);
}
