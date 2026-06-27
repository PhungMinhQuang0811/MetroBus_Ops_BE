package com.vdt.afc_ops_service.dto.response.transaction;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitBatchResponse {
    int total;
    int success;
    int failed;
    @Builder.Default
    List<String> errors = new ArrayList<>();
}