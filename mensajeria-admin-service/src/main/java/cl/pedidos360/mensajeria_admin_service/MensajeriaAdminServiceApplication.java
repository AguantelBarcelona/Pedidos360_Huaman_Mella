package cl.pedidos360.mensajeria_admin_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MensajeriaAdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MensajeriaAdminServiceApplication.class, args);
    }
}
