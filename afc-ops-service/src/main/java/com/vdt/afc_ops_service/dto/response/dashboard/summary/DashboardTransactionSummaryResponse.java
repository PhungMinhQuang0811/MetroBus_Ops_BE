package com.vdt.afc_ops_service.dto.response.dashboard.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DashboardTransactionSummaryResponse {
    long total;
    long openGate;
    long deny;
    long acceptedForForwarding;
    double denyRate;
}
