package pe.edu.nova.plaza.orders.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import pe.edu.nova.java.libs.cqrs.Command;

/**
 * Cancelar el pedido de una compra que falló después de crearlo: la compensación del BFF (ADR-043). Repetirlo no cambia nada.
 *
 * @param customerId el cliente dueño del pedido
 * @param id el pedido
 */
public record CancelOrder(
        @NotBlank String customerId, @NotNull UUID id) implements Command<UUID> {}
