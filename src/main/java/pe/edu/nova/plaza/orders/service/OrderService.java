package pe.edu.nova.plaza.orders.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.nova.plaza.orders.dto.CreateOrderRequest;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/** Crea y consulta pedidos. */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final Clock clock;

    /**
     * Crea el servicio.
     *
     * @param orders los pedidos guardados
     * @param clock el reloj con el que se fecha cada pedido
     */
    public OrderService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    /**
     * Crea el pedido de una compra, o devuelve el que ya existe si la compra se repite.
     *
     * @param customerId el cliente que compra
     * @param idempotencyKey la clave de la compra
     * @param request el pedido
     * @return el pedido, y si se creó ahora
     */
    @Transactional
    public Placement place(String customerId, String idempotencyKey, CreateOrderRequest request) {
        return orders.findByIdempotencyKey(idempotencyKey)
                .map(existing -> new Placement(existing, false))
                .orElseGet(() -> new Placement(orders.save(newOrder(customerId, idempotencyKey, request)), true));
    }

    /**
     * Busca un pedido de un cliente. El pedido de otro cliente no existe para él.
     *
     * @param customerId el cliente que pregunta
     * @param id el pedido
     * @return el pedido, si existe y es suyo
     */
    @Transactional(readOnly = true)
    public Optional<Order> find(String customerId, UUID id) {
        return orders.findWithItemsById(id).filter(order -> order.getCustomerId().equals(customerId));
    }

    /**
     * Lista los pedidos de un cliente, del más nuevo al más viejo.
     *
     * @param customerId el cliente
     * @return sus pedidos
     */
    @Transactional(readOnly = true)
    public List<Order> list(String customerId) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    private Order newOrder(String customerId, String idempotencyKey, CreateOrderRequest request) {
        List<Order.Line> lines = request.items().stream()
                .map(item -> new Order.Line(item.sku(), item.quantity(), item.unitPrice()))
                .toList();
        return Order.place(customerId, idempotencyKey, request.reservationId(), request.currency(), lines,
                clock.instant());
    }

    /**
     * El resultado de crear un pedido.
     *
     * @param order el pedido
     * @param created si se creó ahora o ya existía por la misma compra
     */
    public record Placement(Order order, boolean created) {
    }
}
