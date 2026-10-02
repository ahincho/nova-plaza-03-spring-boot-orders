package pe.edu.nova.plaza.orders.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.edu.nova.java.libs.cqrs.QueryHandler;
import pe.edu.nova.plaza.orders.dto.OrderResponse;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/** Responde los pedidos de un cliente, del más nuevo al más viejo. */
@Service
public class ListOrdersHandler implements QueryHandler<ListOrders, List<OrderResponse>> {

    private final OrderRepository orders;

    /**
     * Crea el handler.
     *
     * @param orders los pedidos guardados
     */
    public ListOrdersHandler(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public List<OrderResponse> handle(ListOrders query) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(query.customerId()).stream()
                .map(OrderViews::of)
                .toList();
    }
}
