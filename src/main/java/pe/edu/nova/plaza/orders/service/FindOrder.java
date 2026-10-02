package pe.edu.nova.plaza.orders.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.plaza.orders.dto.OrderResponse;

/**
 * Un pedido de un cliente, en la forma en que lo ve el BFF (ADR-053).
 *
 * @param customerId el cliente que pregunta
 * @param id el pedido
 */
public record FindOrder(
        @NotBlank String customerId, @NotNull UUID id) implements Query<OrderResponse> {}
