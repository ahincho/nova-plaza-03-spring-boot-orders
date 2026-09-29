package pe.edu.nova.plaza.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * El pedido que arma el BFF después de reservar el stock.
 *
 * <p>Los precios son los que devolvió la reserva del catálogo, nunca los del cliente.
 *
 * @param reservationId la reserva de stock en el catálogo
 * @param currency la moneda, en ISO 4217
 * @param items las líneas del pedido
 */
public record CreateOrderRequest(
        @NotNull UUID reservationId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotEmpty List<@Valid Item> items) {

    /**
     * Una línea del pedido.
     *
     * @param sku el código del producto
     * @param quantity las unidades
     * @param unitPrice el precio de una unidad, tomado de la reserva
     */
    public record Item(
            @NotBlank String sku,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitPrice) {}
}
