package pe.edu.nova.plaza.orders.entity;

/** Los estados de un pedido. Nace pendiente y termina confirmado o cancelado. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
