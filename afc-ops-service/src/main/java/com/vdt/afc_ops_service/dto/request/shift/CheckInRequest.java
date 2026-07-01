package com.vdt.afc_ops_service.dto.request.shift;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {

    @NotNull(message = "stationId is required")
    private Long stationId;
}