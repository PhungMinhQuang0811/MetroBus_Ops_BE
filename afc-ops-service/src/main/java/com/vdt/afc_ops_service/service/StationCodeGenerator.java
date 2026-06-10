package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.repository.StationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StationCodeGenerator {

    static final String STATION_CODE_FORMAT = "%s-ST-%03d";

    StationRepository stationRepository;

    public String generate(Route route) {
        String prefix = route.getRouteCode() + "-ST";
        int nextSequence = stationRepository.findStationCodesByRouteAndPrefix(route, prefix).stream()
                .map(stationCode -> parseSequence(stationCode, prefix))
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String stationCode = String.format(STATION_CODE_FORMAT, route.getRouteCode(), nextSequence);
        while (stationRepository.existsByRouteAndStationCode(route, stationCode)) {
            stationCode = String.format(STATION_CODE_FORMAT, route.getRouteCode(), ++nextSequence);
        }
        return stationCode;
    }

    private int parseSequence(String stationCode, String prefix) {
        String expectedPrefix = prefix + "-";
        if (stationCode == null || !stationCode.startsWith(expectedPrefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(stationCode.substring(expectedPrefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
