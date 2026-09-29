package pe.edu.nova.plaza.orders.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Un pedido de un cliente.
 *
 * <p>Se crea pendiente, con los precios que devolvió la reserva del catálogo, y guarda la clave de
 * idempotencia de la compra para que repetirla no cree un segundo pedido.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    /** Para JPA. */
    protected Order() {}

    /**
     * Crea un pedido pendiente.
     *
     * @param customerId el cliente que compra
     * @param idempotencyKey la clave de la compra
     * @param reservationId la reserva de stock en el catálogo
     * @param currency la moneda, en ISO 4217
     * @param lines las líneas, con el precio de la reserva
     * @param now el momento de la compra
     * @return el pedido, todavía sin guardar
     */
    public static Order place(
            String customerId,
            String idempotencyKey,
            UUID reservationId,
            String currency,
            List<Line> lines,
            Instant now) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.idempotencyKey = idempotencyKey;
        order.reservationId = reservationId;
        order.currency = currency;
        order.status = OrderStatus.PENDING;
        order.createdAt = now;
        lines.forEach(line -> order.items.add(new OrderItem(order, line.sku(), line.quantity(), line.unitPrice())));
        order.total = order.items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return order;
    }

    /**
     * El identificador del pedido.
     *
     * @return el id
     */
    public UUID getId() {
        return id;
    }

    /**
     * El cliente que compró.
     *
     * @return el id del cliente
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * El estado del pedido.
     *
     * @return el estado
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * La moneda del pedido.
     *
     * @return el código ISO 4217
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * El total del pedido.
     *
     * @return la suma de las líneas
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * La reserva de stock del catálogo.
     *
     * @return el id de la reserva
     */
    public UUID getReservationId() {
        return reservationId;
    }

    /**
     * El momento de la compra.
     *
     * @return el instante de creación
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Las líneas del pedido.
     *
     * @return las líneas, de solo lectura
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Una línea al crear el pedido.
     *
     * @param sku el código del producto
     * @param quantity las unidades
     * @param unitPrice el precio de una unidad
     */
    public record Line(String sku, int quantity, BigDecimal unitPrice) {}
}
