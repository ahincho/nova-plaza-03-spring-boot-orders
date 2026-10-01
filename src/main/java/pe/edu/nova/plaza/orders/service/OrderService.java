package pe.edu.nova.plaza.orders.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.plaza.orders.dto.CreateOrderRequest;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/** Crea y consulta pedidos. */
@Service
public class OrderService {

    /** El código del error de dominio de un pedido que no existe para el cliente. */
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

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
     * Crea el pedido de una compra. Una compra repetida no llega aquí: la responde la capacidad de idempotencia
     * con la respuesta que guardó (ADR-047).
     *
     * @param customerId el cliente que compra
     * @param idempotencyKey la clave de la compra, que el pedido guarda para saber de qué compra salió
     * @param request el pedido
     * @return el pedido guardado
     */
    @Transactional
    public Order place(String customerId, String idempotencyKey, CreateOrderRequest request) {
        return orders.save(newOrder(customerId, idempotencyKey, request));
    }

    /**
     * Busca un pedido de un cliente. El pedido de otro cliente no existe para él, así que responde lo mismo
     * que uno que no existe: no se le confirma a nadie que un pedido ajeno existe.
     *
     * @param customerId el cliente que pregunta
     * @param id el pedido
     * @return el pedido
     * @throws DomainError {@code ORDER_NOT_FOUND}, si no existe o es de otro cliente
     */
    @Transactional(readOnly = true)
    public Order find(String customerId, UUID id) {
        return orders.findWithItemsById(id)
                .filter(order -> order.getCustomerId().equals(customerId))
                .orElseThrow(() -> DomainError.notFound(ORDER_NOT_FOUND, "El pedido " + id + " no existe"));
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
        return Order.place(
                customerId, idempotencyKey, request.reservationId(), request.currency(), lines, clock.instant());
    }
}
