package com.vdt.afc_ops_service.dto.response.shift;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {

    private Long shiftId;
    private String accountId;
    private Long stationId;
    private String stationName;
    private String status;
    private LocalDateTime checkedInAt;
}