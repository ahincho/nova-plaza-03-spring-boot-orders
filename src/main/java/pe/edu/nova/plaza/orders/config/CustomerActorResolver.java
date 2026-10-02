package pe.edu.nova.plaza.orders.config;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.plaza.orders.controller.OrderController;

/**
 * El actor de cada comando y consulta: el cliente que el BFF autenticó y pasa en
 * {@value OrderController#CUSTOMER_HEADER} (ADR-053). Pedidos no valida tokens, así que el actor no sale de
 * Spring Security: reemplaza al de Nova, y la auditoría registra al cliente.
 */
@Component
public class CustomerActorResolver implements ActorResolver {

    @Override
    public Optional<String> currentActor() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        return Optional.ofNullable(attributes.getRequest().getHeader(OrderController.CUSTOMER_HEADER))
                .filter(customer -> !customer.isBlank());
    }
}
