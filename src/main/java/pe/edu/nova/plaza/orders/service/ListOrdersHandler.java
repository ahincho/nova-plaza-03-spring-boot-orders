package pe.edu.nova.plaza.orders.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import pe.edu.nova.java.libs.cqrs.QueryHandler;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.starters.persistence.CursorPages;
import pe.edu.nova.java.starters.persistence.CursorSort;
import pe.edu.nova.plaza.orders.dto.OrderResponse;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/**
 * Responde una página de los pedidos de un cliente, del más nuevo al más viejo, para un scroll infinito (ADR-054).
 * El id desempata dos pedidos del mismo instante, así que ninguno se repite ni se salta entre páginas.
 */
@Service
public class ListOrdersHandler implements QueryHandler<ListOrders, CursorPage<OrderResponse>> {

    /** Del más nuevo al más viejo; el nombre viaja en el cursor y lo ata a este orden. */
    static final CursorSort NEWEST =
            CursorSort.of("newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

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
    public CursorPage<OrderResponse> handle(ListOrders query) {
        Window<Order> window = orders.findByCustomerId(
                query.customerId(),
                CursorPages.position(query.page(), NEWEST),
                CursorPages.limit(query.page()),
                NEWEST.sort());
        return CursorPages.page(window, NEWEST, OrderViews::of);
    }
}
