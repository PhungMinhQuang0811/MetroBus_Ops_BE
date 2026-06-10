package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.repository.RouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteCodeGeneratorTest {

    @Mock
    RouteRepository routeRepository;

    @Test
    void generate_IgnoresMalformedCodesAndSkipsExistingCode() {
        Operator operator = Operator.builder().id(1L).build();
        RouteCodeGenerator generator = new RouteCodeGenerator(routeRepository);
        when(routeRepository.findRouteCodesByOperatorAndPrefix(operator, PredefinedTransportType.METRO))
                .thenReturn(Arrays.asList(null, "BUS-999", "METRO-ABC", "METRO-002"));
        when(routeRepository.existsByOperatorAndRouteCode(operator, "METRO-003")).thenReturn(true);
        when(routeRepository.existsByOperatorAndRouteCode(operator, "METRO-004")).thenReturn(false);

        String routeCode = generator.generate(operator, PredefinedTransportType.METRO);

        assertEquals("METRO-004", routeCode);
    }
}
