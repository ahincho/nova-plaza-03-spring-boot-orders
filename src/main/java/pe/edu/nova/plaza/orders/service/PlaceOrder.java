package pe.edu.nova.plaza.orders.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.plaza.orders.dto.CreateOrderRequest;

/**
 * Crear el pedido de una compra (ADR-053). Devuelve el identificador del pedido; su vista se pide después con
 * {@link FindOrder}.
 *
 * @param customerId el cliente que compra
 * @param idempotencyKey la clave de la compra, que el pedido guarda para saber de qué compra salió
 * @param order el pedido que armó el BFF
 */
public record PlaceOrder(
        @NotBlank String customerId,
        @NotBlank String idempotencyKey,
        @NotNull @Valid CreateOrderRequest order) implements Command<UUID> {}
