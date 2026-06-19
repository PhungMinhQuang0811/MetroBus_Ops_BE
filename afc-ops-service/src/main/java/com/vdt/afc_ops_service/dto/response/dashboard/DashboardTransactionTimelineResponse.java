package com.vdt.afc_ops_service.dto.response.dashboard;

import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardTransactionTimelineItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DashboardTransactionTimelineResponse {
    String bucket;
    List<DashboardTransactionTimelineItemResponse> items;
}
