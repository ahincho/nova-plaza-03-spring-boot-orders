package pe.edu.nova.plaza.orders.service;

import org.springframework.stereotype.Service;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.java.libs.cqrs.QueryHandler;
import pe.edu.nova.plaza.orders.dto.OrderResponse;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/**
 * Responde un pedido de un cliente. El pedido de otro cliente no existe para él, así que responde lo mismo que
 * uno que no existe: no se le confirma a nadie que un pedido ajeno existe.
 */
@Service
public class FindOrderHandler implements QueryHandler<FindOrder, OrderResponse> {

    /** El código del error de dominio de un pedido que no existe para el cliente. */
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";

    private final OrderRepository orders;

    /**
     * Crea el handler.
     *
     * @param orders los pedidos guardados
     */
    public FindOrderHandler(OrderRepository orders) {
        this.orders = orders;
    }

    /**
     * Busca el pedido.
     *
     * @param query el cliente y el pedido
     * @return el pedido
     * @throws DomainError {@code ORDER_NOT_FOUND}, si no existe o es de otro cliente
     */
    @Override
    public OrderResponse handle(FindOrder query) {
        return orders.findWithItemsById(query.id())
                .filter(order -> order.getCustomerId().equals(query.customerId()))
                .map(OrderViews::of)
                .orElseThrow(() -> DomainError.notFound(ORDER_NOT_FOUND, "El pedido " + query.id() + " no existe"));
    }
}
