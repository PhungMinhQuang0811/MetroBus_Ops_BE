package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationCodeGeneratorTest {

    @Mock
    StationRepository stationRepository;

    @Test
    void generate_IgnoresMalformedCodesAndSkipsExistingCode() {
        Route route = Route.builder().id(1L).routeCode("METRO-001").build();
        StationCodeGenerator generator = new StationCodeGenerator(stationRepository);
        when(stationRepository.findStationCodesByRouteAndPrefix(route, "METRO-001-ST"))
                .thenReturn(Arrays.asList(null, "METRO-001-XX-999", "METRO-001-ST-ABC", "METRO-001-ST-002"));
        when(stationRepository.existsByRouteAndStationCode(route, "METRO-001-ST-003")).thenReturn(true);
        when(stationRepository.existsByRouteAndStationCode(route, "METRO-001-ST-004")).thenReturn(false);

        String stationCode = generator.generate(route);

        assertEquals("METRO-001-ST-004", stationCode);
    }
}
