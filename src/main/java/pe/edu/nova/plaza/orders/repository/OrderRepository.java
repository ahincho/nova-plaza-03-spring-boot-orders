package pe.edu.nova.plaza.orders.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
     * Lista los pedidos de un cliente, del más nuevo al más viejo.
     *
     * @param customerId el cliente
     * @return sus pedidos
     */
    @EntityGraph(attributePaths = "items")
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
