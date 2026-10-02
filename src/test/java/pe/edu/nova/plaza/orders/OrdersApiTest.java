package pe.edu.nova.plaza.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.edu.nova.plaza.orders.entity.Order;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/** La API de pedidos, contra un Postgres y un Vault reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class OrdersApiTest {

    private static final String ORDER = """
            {
              "reservationId": "%s",
              "currency": "PEN",
              "items": [
                {"sku": "MUG-001", "quantity": 2, "unitPrice": 25.50},
                {"sku": "TEE-002", "quantity": 1, "unitPrice": 49.90}
              ]
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OrderRepository orders;

    @BeforeAll
    static void infrastructure() {
        PlazaInfrastructure.start();
    }

    @Test
    void anOrderIsPlacedPendingWithTheTotalOfItsLines() throws Exception {
        mvc.perform(place("customer-1", UUID.randomUUID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.customerId").value("customer-1"))
                .andExpect(jsonPath("$.data.total").value(100.90))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void repeatingAPurchaseReplaysTheSameResponseWithoutASecondOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        String order = newOrder();

        MvcResult first = mvc.perform(place("customer-2", key, order))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn();
        MvcResult second = mvc.perform(place("customer-2", key, order))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn();

        assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        assertThat(orders.countByCustomerId("customer-2")).isEqualTo(1);
    }

    @Test
    void theKeyOfAnotherCustomerIsAnotherPurchaseAndNeverTheirOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        String order = newOrder();

        String mine = idOf(mvc.perform(place("customer-5", key, order)).andReturn());
        MvcResult theirs = mvc.perform(place("customer-6", key, order))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.data.customerId").value("customer-6"))
                .andReturn();

        assertThat(idOf(theirs)).isNotEqualTo(mine);
    }

    @Test
    void theSameKeyWithAnotherOrderIs422() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(place("customer-7", key, newOrder())).andExpect(status().isCreated());

        mvc.perform(place("customer-7", key, newOrder()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].code").value("IDEMPOTENCY_KEY_REUSED"));
        assertThat(orders.countByCustomerId("customer-7")).isEqualTo(1);
    }

    @Test
    void aPurchaseWithoutKeyIs400() throws Exception {
        mvc.perform(post("/v1/orders")
                        .header("X-Customer-Id", "customer-8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newOrder()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("IDEMPOTENCY_KEY_REQUIRED"));
        assertThat(orders.countByCustomerId("customer-8")).isZero();
    }

    @Test
    void anOrderIsOnlyVisibleToItsCustomer() throws Exception {
        String id = idOf(
                mvc.perform(place("customer-3", UUID.randomUUID().toString())).andReturn());

        mvc.perform(get("/v1/orders/{id}", id).header("X-Customer-Id", "customer-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        // El pedido ajeno responde lo mismo que uno que no existe, con el error de dominio de ADR-031.
        mvc.perform(get("/v1/orders/{id}", id).header("X-Customer-Id", "someone-else"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.errors[0].message").value("El pedido " + id + " no existe"))
                .andExpect(jsonPath("$.metadata.traceId").isNotEmpty());
    }

    @Test
    void anInvalidOrderIs400WithItsFields() throws Exception {
        String invalid = """
                {"reservationId": "%s", "currency": "soles", "items": []}
                """.formatted(UUID.randomUUID());

        mvc.perform(place("customer-9", UUID.randomUUID().toString(), invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].code", everyItem(is("BAD_REQUEST"))))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("currency", "items")));
        assertThat(orders.countByCustomerId("customer-9")).isZero();
    }

    @Test
    void eachCommandAndQueryIsAuditedWithTheCustomerAsItsActor(CapturedOutput output) throws Exception {
        String id = idOf(
                mvc.perform(place("customer-10", UUID.randomUUID().toString())).andReturn());
        mvc.perform(get("/v1/orders/{id}", id).header("X-Customer-Id", "customer-10"))
                .andExpect(status().isOk());
        mvc.perform(get("/v1/orders/{id}", id).header("X-Customer-Id", "customer-11"))
                .andExpect(status().isNotFound());

        // La auditoría de ADR-053: el tipo, el actor y el resultado, nunca el contenido del pedido
        assertThat(output.getOut())
                .contains("COMMAND PlaceOrder by customer-10: SUCCEEDED")
                .contains("QUERY FindOrder by customer-10: SUCCEEDED")
                .contains("QUERY FindOrder by customer-11: FAILED ORDER_NOT_FOUND")
                .doesNotContain("MUG-001");
    }

    @Test
    void aCustomerListsTheirOrdersNewestFirst() throws Exception {
        String older = idOf(
                mvc.perform(place("customer-4", UUID.randomUUID().toString())).andReturn());
        String newer = idOf(
                mvc.perform(place("customer-4", UUID.randomUUID().toString())).andReturn());

        mvc.perform(get("/v1/orders").header("X-Customer-Id", "customer-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(newer))
                .andExpect(jsonPath("$.data.items[1].id").value(older))
                .andExpect(jsonPath("$.data.items[0].items.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
    }

    @Test
    void aCustomerScrollsTheirOrdersPageByPageWithoutRepeatsOrGaps() throws Exception {
        Set<String> placed = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            placed.add(idOf(mvc.perform(place("customer-12", UUID.randomUUID().toString()))
                    .andReturn()));
        }

        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var request =
                    get("/v1/orders").header("X-Customer-Id", "customer-12").param("limit", "2");
            if (cursor != null) {
                request.param("cursor", cursor);
            }
            String body = mvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            seen.addAll(JsonPath.read(body, "$.data.items[*].id"));
            cursor = JsonPath.read(body, "$.data.nextCursor");
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3);
        assertThat(seen).hasSize(5).doesNotHaveDuplicates();
        assertThat(Set.copyOf(seen)).isEqualTo(placed);
    }

    @Test
    void aBadCursorOrLimitIs400OnItsField() throws Exception {
        mvc.perform(get("/v1/orders").header("X-Customer-Id", "customer-13").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cursor"));
        mvc.perform(get("/v1/orders").header("X-Customer-Id", "customer-13").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    void anOrderRecordsWhoPlacedItAndWhen() throws Exception {
        Instant before = Instant.now().minusSeconds(1);
        String id = idOf(
                mvc.perform(place("customer-14", UUID.randomUUID().toString())).andReturn());

        Order order = orders.findById(UUID.fromString(id)).orElseThrow();
        assertThat(order.getCreatedBy()).isEqualTo("customer-14");
        assertThat(order.getUpdatedBy()).isEqualTo("customer-14");
        assertThat(order.getCreatedAt()).isAfter(before).isEqualTo(order.getUpdatedAt());
        assertThat(order.getVersion()).isZero();
    }

    private static org.springframework.test.web.servlet.RequestBuilder place(String customerId, String key) {
        return place(customerId, key, newOrder());
    }

    private static org.springframework.test.web.servlet.RequestBuilder place(
            String customerId, String key, String order) {
        return post("/v1/orders")
                .header("X-Customer-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(order);
    }

    /** Un pedido con su propia reserva: dos llamadas dan dos pedidos distintos. */
    private static String newOrder() {
        return ORDER.formatted(UUID.randomUUID());
    }

    private static String idOf(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + 6;
        return body.substring(start, body.indexOf('"', start));
    }
}
