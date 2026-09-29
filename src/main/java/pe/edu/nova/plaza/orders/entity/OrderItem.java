package pe.edu.nova.plaza.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Una línea del pedido, con el precio congelado al momento de la compra. */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Para JPA. */
    protected OrderItem() {
    }

    OrderItem(Order order, String sku, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * El código del producto.
     *
     * @return el SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * Las unidades compradas.
     *
     * @return la cantidad
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * El precio de una unidad al momento de la compra.
     *
     * @return el precio unitario
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * El precio de la línea.
     *
     * @return el precio unitario por la cantidad
     */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
