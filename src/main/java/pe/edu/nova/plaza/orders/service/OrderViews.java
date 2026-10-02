package pe.edu.nova.plaza.orders.service;

import java.util.List;
import pe.edu.nova.plaza.orders.dto.OrderResponse;
import pe.edu.nova.plaza.orders.entity.Order;

/**
 * La vista de lectura de un pedido. Se arma dentro de la consulta, en su transacción de solo lectura, así que la
 * entidad nunca sale del servicio.
 */
final class OrderViews {

    private OrderViews() {}

    static OrderResponse of(Order order) {
        List<OrderResponse.Item> items = order.getItems().stream()
                .map(item -> new OrderResponse.Item(item.getSku(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getCurrency(),
                order.getTotal(),
                order.getReservationId(),
                order.getCreatedAt(),
                items);
    }
}
