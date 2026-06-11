package com.vdt.afc_ops_service.service.generator;

import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceCodeGenerator {

    static final String DEVICE_CODE_FORMAT = "%s-DV-%03d";

    DeviceRepository deviceRepository;

    public String generate(Station station) {
        String prefix = station.getStationCode() + "-DV";
        int nextSequence = deviceRepository.findDeviceCodesByStationAndPrefix(station, prefix).stream()
                .map(deviceCode -> parseSequence(deviceCode, prefix))
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String deviceCode = String.format(DEVICE_CODE_FORMAT, station.getStationCode(), nextSequence);
        while (deviceRepository.existsByDeviceCode(deviceCode)) {
            deviceCode = String.format(DEVICE_CODE_FORMAT, station.getStationCode(), ++nextSequence);
        }
        return deviceCode;
    }

    private int parseSequence(String deviceCode, String prefix) {
        String expectedPrefix = prefix + "-";
        if (deviceCode == null || !deviceCode.startsWith(expectedPrefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(deviceCode.substring(expectedPrefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
