package pe.edu.nova.plaza.orders.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.edu.nova.java.libs.outbox.Outbox;
import pe.edu.nova.plaza.orders.entity.Order;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los eventos de un pedido (ADR-048). Se escriben en el outbox dentro de la transacción del comando, así que salen
 * si y solo si el cambio se confirmó; Debezium los publica en {@code plaza.orders}, con el id del pedido como clave.
 *
 * <p>Ninguno lleva el cliente: ni el ranking del catálogo ni la auditoría lo necesitan.
 */
@Component
public class OrderEvents {

    /** El tipo de agregado, que elige el tópico. */
    static final String AGGREGATE = "orders";

    /** El tipo del evento de un pedido creado. */
    static final String CREATED = "pe.edu.nova.plaza.order.created.v1";

    /** El tipo del evento de un pedido confirmado. */
    static final String CONFIRMED = "pe.edu.nova.plaza.order.confirmed.v1";

    /** El tipo del evento de un pedido cancelado. */
    static final String CANCELLED = "pe.edu.nova.plaza.order.cancelled.v1";

    private final Outbox outbox;
    private final JsonMapper json;
    private final Clock clock;

    /**
     * Crea los eventos.
     *
     * @param outbox el outbox de Nova
     * @param json   el mapper del servicio
     * @param clock  el reloj del servicio
     */
    public OrderEvents(Outbox outbox, JsonMapper json, Clock clock) {
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
    }

    /**
     * El pedido se creó.
     *
     * @param order el pedido
     */
    void created(Order order) {
        append(order.getId(), CREATED, Snapshot.of(order, clock.instant()));
    }

    /**
     * El pedido se confirmó: es el que alimenta el ranking de lo más vendido.
     *
     * @param order el pedido
     */
    void confirmed(Order order) {
        append(order.getId(), CONFIRMED, Snapshot.of(order, clock.instant()));
    }

    /**
     * El pedido se canceló.
     *
     * @param order el pedido
     */
    void cancelled(Order order) {
        append(order.getId(), CANCELLED, new Cancelled(order.getId(), clock.instant()));
    }

    private void append(UUID orderId, String type, Object payload) {
        outbox.append(AGGREGATE, orderId.toString(), type, json.writeValueAsString(payload));
    }

    /**
     * El pedido creado o confirmado, con sus líneas.
     *
     * @param orderId    el pedido
     * @param currency   la moneda
     * @param total      el total
     * @param items      las líneas
     * @param occurredAt cuándo pasó
     */
    record Snapshot(UUID orderId, String currency, BigDecimal total, List<Item> items, Instant occurredAt) {

        static Snapshot of(Order order, Instant occurredAt) {
            List<Item> items = order.getItems().stream()
                    .map(item -> new Item(item.getSku(), item.getQuantity(), item.getUnitPrice()))
                    .toList();
            return new Snapshot(order.getId(), order.getCurrency(), order.getTotal(), items, occurredAt);
        }
    }

    /**
     * Una línea del pedido.
     *
     * @param sku       el producto
     * @param quantity  las unidades
     * @param unitPrice el precio de una unidad
     */
    record Item(String sku, int quantity, BigDecimal unitPrice) {}

    /**
     * El pedido cancelado.
     *
     * @param orderId    el pedido
     * @param occurredAt cuándo pasó
     */
    record Cancelled(UUID orderId, Instant occurredAt) {}
}
