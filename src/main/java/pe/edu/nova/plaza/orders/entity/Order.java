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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import pe.edu.nova.java.starters.persistence.AuditableEntity;

/**
 * Un pedido de un cliente.
 *
 * <p>Se crea pendiente, con los precios que devolvió la reserva del catálogo, y guarda la clave de
 * idempotencia de la compra para saber de qué compra salió. La que evita un segundo pedido es la capacidad de
 * idempotencia de Nova (ADR-047); la clave del pedido es única por cliente, como la de la capacidad.
 *
 * <p>La auditoría y la versión las hereda de {@link AuditableEntity} (ADR-054): el momento de la compra es su
 * {@code createdAt}, con el reloj del servicio, y quien compró es su {@code createdBy}, el mismo cliente que audita
 * el bus.
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "orders_customer_idempotency_key",
                        columnNames = {"customer_id", "idempotency_key"}))
public class Order extends AuditableEntity {

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

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    // Una página de pedidos carga las líneas de todos en lotes, y no con un fetch join que rompería el límite.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @BatchSize(size = 100)
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
     * @return el pedido, todavía sin guardar
     */
    public static Order place(
            String customerId, String idempotencyKey, UUID reservationId, String currency, List<Line> lines) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.idempotencyKey = idempotencyKey;
        order.reservationId = reservationId;
        order.currency = currency;
        order.status = OrderStatus.PENDING;
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
