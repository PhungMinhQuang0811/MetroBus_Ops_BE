package com.vdt.afc_ops_service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AfcOpsServiceApplication {
    @Value("${app.timezone}")
    private String appTimezone;
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(appTimezone));
    }

    public static void main(String[] args) {
        SpringApplication.run(AfcOpsServiceApplication.class, args);
    }

}
