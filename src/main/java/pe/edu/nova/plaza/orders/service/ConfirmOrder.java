package pe.edu.nova.plaza.orders.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import pe.edu.nova.java.libs.cqrs.Command;

/**
 * Confirmar el pedido de una compra que terminó (ADR-043). Repetirlo no cambia nada.
 *
 * @param customerId el cliente dueño del pedido
 * @param id el pedido
 */
public record ConfirmOrder(
        @NotBlank String customerId, @NotNull UUID id) implements Command<UUID> {}
