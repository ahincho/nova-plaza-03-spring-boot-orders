package pe.edu.nova.plaza.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.edu.nova.plaza.orders.repository.OrderRepository;

/** La API de pedidos, contra un Postgres y un Vault reales. */
@SpringBootTest
@AutoConfigureMockMvc
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
    void repeatingAPurchaseReturnsTheSameOrder() throws Exception {
        String key = UUID.randomUUID().toString();

        MvcResult first = mvc.perform(place("customer-2", key)).andExpect(status().isCreated()).andReturn();
        MvcResult second = mvc.perform(place("customer-2", key)).andExpect(status().isOk()).andReturn();

        String id = idOf(first);
        assertThat(idOf(second)).isEqualTo(id);
        assertThat(orders.findByCustomerIdOrderByCreatedAtDesc("customer-2")).hasSize(1);
    }

    @Test
    void anOrderIsOnlyVisibleToItsCustomer() throws Exception {
        String id = idOf(mvc.perform(place("customer-3", UUID.randomUUID().toString())).andReturn());

        mvc.perform(get("/v1/orders/{id}", id).header("X-Customer-Id", "customer-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        mvc.perform(get("/v1/orders/{id}", id).header("X-Customer-Id", "someone-else"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCustomerListsTheirOrdersNewestFirst() throws Exception {
        String older = idOf(mvc.perform(place("customer-4", UUID.randomUUID().toString())).andReturn());
        String newer = idOf(mvc.perform(place("customer-4", UUID.randomUUID().toString())).andReturn());

        mvc.perform(get("/v1/orders").header("X-Customer-Id", "customer-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(newer))
                .andExpect(jsonPath("$.data[1].id").value(older));
    }

    private static org.springframework.test.web.servlet.RequestBuilder place(String customerId, String key) {
        return post("/v1/orders")
                .header("X-Customer-Id", customerId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ORDER.formatted(UUID.randomUUID()));
    }

    private static String idOf(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + 6;
        return body.substring(start, body.indexOf('"', start));
    }
}
