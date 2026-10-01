package pe.edu.nova.plaza.orders.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.nova.java.starters.idempotency.Idempotent;
import pe.edu.nova.plaza.orders.dto.CreateOrderRequest;
import pe.edu.nova.plaza.orders.dto.OrderResponse;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.service.OrderService;

/**
 * La API interna de pedidos. La llama solo el BFF, que ya validó el token y pasa el cliente en
 * {@value #CUSTOMER_HEADER}.
 */
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    /** El cliente que el BFF autenticó. */
    public static final String CUSTOMER_HEADER = "X-Customer-Id";

    /** La clave con la que el cliente puede repetir una compra sin duplicarla. */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final OrderService service;

    /**
     * Crea el controlador.
     *
     * @param service el servicio de pedidos
     */
    public OrderController(OrderService service) {
        this.service = service;
    }

    /**
     * Crea un pedido pendiente. La compra es idempotente (ADR-047): si se repite con la misma clave, el
     * cliente recibe la misma respuesta, con {@code Idempotent-Replayed: true}, y no se crea otro pedido. La
     * misma clave de otro cliente es otra compra, y con otro contenido es un 422.
     *
     * @param customerId el cliente
     * @param idempotencyKey la clave de la compra
     * @param request el pedido
     * @return el pedido, con 201
     */
    @Idempotent
    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @RequestHeader(CUSTOMER_HEADER) String customerId,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        Order order = service.place(customerId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    /**
     * Devuelve un pedido del cliente.
     *
     * @param customerId el cliente
     * @param id el pedido
     * @return el pedido; si no existe o es de otro cliente, el error de dominio responde un 404
     */
    @GetMapping("/{id}")
    public OrderResponse find(@RequestHeader(CUSTOMER_HEADER) String customerId, @PathVariable UUID id) {
        return toResponse(service.find(customerId, id));
    }

    /**
     * Lista los pedidos del cliente, del más nuevo al más viejo.
     *
     * @param customerId el cliente
     * @return sus pedidos
     */
    @GetMapping
    public List<OrderResponse> list(@RequestHeader(CUSTOMER_HEADER) String customerId) {
        return service.list(customerId).stream()
                .map(OrderController::toResponse)
                .toList();
    }

    private static OrderResponse toResponse(Order order) {
        List<OrderResponse.Item> items = order.getItems().stream()
                .map(item -> new OrderResponse.Item(item.getSku(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name(),
                order.getCurrency(),
                order.getTotal(),
                order.getReservationId(),
                order.getCreatedAt(),
                items);
    }
}
