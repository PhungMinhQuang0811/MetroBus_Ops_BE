package com.vdt.authservice.modules.identity.security.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
@FieldDefaults(level = PRIVATE)
public class SecurityProperties {
    List<String> publicEndpoints = new ArrayList<>();
}
