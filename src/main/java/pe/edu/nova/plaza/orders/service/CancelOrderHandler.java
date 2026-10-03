package pe.edu.nova.plaza.orders.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.java.libs.cqrs.CommandHandler;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/**
 * Cancela un pedido del cliente. La transacción y el flush los pone el bus, así que un cambio que pisa
 * otro es un 409 (ADR-053 y ADR-054).
 */
@Service
public class CancelOrderHandler implements CommandHandler<CancelOrder, UUID> {

    private final OrderRepository orders;
    private final OrderEvents events;

    /**
     * Crea el handler.
     *
     * @param orders los pedidos guardados
     * @param events los eventos del pedido, que se escriben en la misma transacción
     */
    public CancelOrderHandler(OrderRepository orders, OrderEvents events) {
        this.orders = orders;
        this.events = events;
    }

    /**
     * Busca el pedido y lo cancela.
     *
     * @param command el cliente y el pedido
     * @return el pedido
     * @throws DomainError {@code ORDER_NOT_FOUND}, si no existe o es de otro cliente
     */
    @Override
    public UUID handle(CancelOrder command) {
        Order order = orders.findById(command.id())
                .filter(found -> found.getCustomerId().equals(command.customerId()))
                .orElseThrow(() -> DomainError.notFound(
                        FindOrderHandler.ORDER_NOT_FOUND, "El pedido " + command.id() + " no existe"));
        if (order.cancel()) {
            events.cancelled(order);
        }
        return order.getId();
    }
}
