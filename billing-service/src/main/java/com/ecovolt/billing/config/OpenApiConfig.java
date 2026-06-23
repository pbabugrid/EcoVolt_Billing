package com.ecovolt.billing.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecoVoltOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EcoVolt Utility Billing API")
                        .description("Customer, meter, reading and invoice management for the EcoVolt utility billing platform.")
                        .version("v1")
                        .contact(new Contact().name("EcoVolt Engineering"))
                        .license(new License().name("Proprietary")));
    }
}
