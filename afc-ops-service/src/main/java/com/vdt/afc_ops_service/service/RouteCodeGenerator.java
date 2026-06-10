package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.repository.RouteRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RouteCodeGenerator {

    static final String ROUTE_CODE_FORMAT = "%s-%03d";

    RouteRepository routeRepository;

    public String generate(Operator operator, String transportType) {
        int nextSequence = routeRepository.findRouteCodesByOperatorAndPrefix(operator, transportType).stream()
                .map(routeCode -> parseSequence(routeCode, transportType))
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String routeCode = String.format(ROUTE_CODE_FORMAT, transportType, nextSequence);
        while (routeRepository.existsByOperatorAndRouteCode(operator, routeCode)) {
            routeCode = String.format(ROUTE_CODE_FORMAT, transportType, ++nextSequence);
        }
        return routeCode;
    }

    private int parseSequence(String routeCode, String prefix) {
        String expectedPrefix = prefix + "-";
        if (routeCode == null || !routeCode.startsWith(expectedPrefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(routeCode.substring(expectedPrefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
