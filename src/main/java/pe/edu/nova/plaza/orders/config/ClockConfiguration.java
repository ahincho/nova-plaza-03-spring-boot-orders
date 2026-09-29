package pe.edu.nova.plaza.orders.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El reloj del servicio, como bean para que una prueba pueda fijarlo. */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    /**
     * El reloj del sistema, en UTC.
     *
     * @return el reloj
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
