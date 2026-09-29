package pe.edu.nova.plaza.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** El servicio de pedidos de Plaza. */
@SpringBootApplication
public class OrdersApplication {

    /**
     * Arranca el servicio.
     *
     * @param args los argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
