package io.hashnut.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"io.hashnut.shop"})
public class HashnutShopApp {
    public static void main(String[] args) {
        SpringApplication.run(HashnutShopApp.class, args);
    }
}