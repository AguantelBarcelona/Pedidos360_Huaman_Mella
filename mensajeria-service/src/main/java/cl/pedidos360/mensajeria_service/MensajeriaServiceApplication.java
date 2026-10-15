
package cl.pedidos360.mensajeria_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MensajeriaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MensajeriaServiceApplication.class, args);
    }
}
