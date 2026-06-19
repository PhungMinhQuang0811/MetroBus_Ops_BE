package com.vdt.afc_ops_service.dto.response.dashboard;

import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardBatchSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardControlSyncSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardDeviceSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardIncidentSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardTransactionSummaryResponse;
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
public class DashboardSummaryResponse {
    DashboardDeviceSummaryResponse deviceSummary;
    DashboardTransactionSummaryResponse transactionSummary;
    DashboardIncidentSummaryResponse incidentSummary;
    DashboardBatchSummaryResponse batchSummary;
    DashboardControlSyncSummaryResponse controlSyncSummary;
}
