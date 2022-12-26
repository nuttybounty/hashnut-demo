package io.hashnut.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class HashnutShopApp extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(HashnutShopApp.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        application.listeners();
        return application.sources(applicationClass);
    }

    private static Class<HashnutShopApp> applicationClass = HashnutShopApp.class;

}