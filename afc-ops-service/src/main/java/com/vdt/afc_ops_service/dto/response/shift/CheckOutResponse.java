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
public class CheckOutResponse {

    private Long shiftId;
    private String status;
    private Integer totalTransactions;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
}