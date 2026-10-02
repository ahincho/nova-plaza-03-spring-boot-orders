package pe.edu.nova.plaza.orders.service;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.plaza.orders.dto.OrderResponse;

/**
 * Los pedidos de un cliente, del más nuevo al más viejo (ADR-053).
 *
 * @param customerId el cliente
 */
public record ListOrders(@NotBlank String customerId) implements Query<List<OrderResponse>> {}
