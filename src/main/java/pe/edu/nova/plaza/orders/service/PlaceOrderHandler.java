package pe.edu.nova.plaza.orders.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.edu.nova.java.libs.cqrs.CommandHandler;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/**
 * Crea el pedido de una compra. Una compra repetida no llega aquí: la responde la capacidad de idempotencia con
 * la respuesta que guardó (ADR-047).
 *
 * <p>La transacción la pone el bus (ADR-053), y se suma a la que abre la idempotencia alrededor del controlador:
 * el pedido y el registro de su respuesta se confirman en el mismo commit.
 */
@Service
public class PlaceOrderHandler implements CommandHandler<PlaceOrder, UUID> {

    private final OrderRepository orders;
    private final OrderEvents events;

    /**
     * Crea el handler.
     *
     * @param orders los pedidos guardados
     * @param events los eventos del pedido, que se escriben en la misma transacción
     */
    public PlaceOrderHandler(OrderRepository orders, OrderEvents events) {
        this.orders = orders;
        this.events = events;
    }

    @Override
    public UUID handle(PlaceOrder command) {
        List<Order.Line> lines = command.order().items().stream()
                .map(item -> new Order.Line(item.sku(), item.quantity(), item.unitPrice()))
                .toList();
        Order order = Order.place(
                command.customerId(),
                command.idempotencyKey(),
                command.order().reservationId(),
                command.order().currency(),
                lines);
        Order saved = orders.save(order);
        events.created(saved);
        return saved.getId();
    }
}
