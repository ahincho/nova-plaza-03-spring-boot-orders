package pe.edu.nova.plaza.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Un pedido, como lo ve el BFF.
 *
 * @param id el identificador
 * @param customerId el cliente que compró
 * @param status el estado
 * @param currency la moneda
 * @param total la suma de las líneas
 * @param reservationId la reserva de stock en el catálogo
 * @param createdAt el momento de la compra
 * @param items las líneas
 */
public record OrderResponse(
        UUID id,
        String customerId,
        String status,
        String currency,
        BigDecimal total,
        UUID reservationId,
        Instant createdAt,
        List<Item> items) {

    /**
     * Una línea del pedido.
     *
     * @param sku el código del producto
     * @param quantity las unidades
     * @param unitPrice el precio de una unidad al momento de la compra
     */
    public record Item(String sku, int quantity, BigDecimal unitPrice) {
    }
}
