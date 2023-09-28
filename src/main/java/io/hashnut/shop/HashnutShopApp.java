package io.hashnut.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {"io.hashnut.shop"})
public class HashnutShopApp {
    public static void main(String[] args) {
        SpringApplication.run(HashnutShopApp.class, args);
    }
}