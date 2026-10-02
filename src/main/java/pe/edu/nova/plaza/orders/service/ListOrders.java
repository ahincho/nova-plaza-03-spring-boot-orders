package pe.edu.nova.plaza.orders.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.persistence.CursorPage;
import pe.edu.nova.java.libs.persistence.CursorRequest;
import pe.edu.nova.plaza.orders.dto.OrderResponse;

/**
 * Una página de los pedidos de un cliente, del más nuevo al más viejo (ADR-053 y ADR-054).
 *
 * @param customerId el cliente
 * @param page cuántos pedidos y desde qué cursor
 */
public record ListOrders(
        @NotBlank String customerId, @NotNull CursorRequest page) implements Query<CursorPage<OrderResponse>> {}
