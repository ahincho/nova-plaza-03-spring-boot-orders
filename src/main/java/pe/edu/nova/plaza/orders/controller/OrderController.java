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
import pe.edu.nova.java.libs.cqrs.CommandBus;
import pe.edu.nova.java.libs.cqrs.QueryBus;
import pe.edu.nova.java.starters.idempotency.Idempotent;
import pe.edu.nova.plaza.orders.dto.CreateOrderRequest;
import pe.edu.nova.plaza.orders.dto.OrderResponse;
import pe.edu.nova.plaza.orders.service.FindOrder;
import pe.edu.nova.plaza.orders.service.ListOrders;
import pe.edu.nova.plaza.orders.service.PlaceOrder;

/**
 * La API interna de pedidos. La llama solo el BFF, que ya validó el token y pasa el cliente en
 * {@value #CUSTOMER_HEADER}.
 *
 * <p>No tiene lógica: cada operación es un comando o una consulta que entrega a su bus (ADR-053), y el bus le
 * pone la auditoría, la validación y la transacción.
 */
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    /** El cliente que el BFF autenticó. */
    public static final String CUSTOMER_HEADER = "X-Customer-Id";

    /** La clave con la que el cliente puede repetir una compra sin duplicarla. */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final CommandBus commands;
    private final QueryBus queries;

    /**
     * Crea el controlador.
     *
     * @param commands el bus de comandos
     * @param queries el bus de consultas
     */
    public OrderController(CommandBus commands, QueryBus queries) {
        this.commands = commands;
        this.queries = queries;
    }

    /**
     * Crea un pedido pendiente. La compra es idempotente (ADR-047): si se repite con la misma clave, el
     * cliente recibe la misma respuesta, con {@code Idempotent-Replayed: true}, y no se crea otro pedido. La
     * misma clave de otro cliente es otra compra, y con otro contenido es un 422.
     *
     * <p>El comando devuelve solo el identificador, y la vista sale de la consulta, dentro de la misma
     * transacción que la idempotencia abrió para la compra.
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
        UUID id = commands.execute(new PlaceOrder(customerId, idempotencyKey, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(queries.execute(new FindOrder(customerId, id)));
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
        return queries.execute(new FindOrder(customerId, id));
    }

    /**
     * Lista los pedidos del cliente, del más nuevo al más viejo.
     *
     * @param customerId el cliente
     * @return sus pedidos
     */
    @GetMapping
    public List<OrderResponse> list(@RequestHeader(CUSTOMER_HEADER) String customerId) {
        return queries.execute(new ListOrders(customerId));
    }
}
