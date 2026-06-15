package com.vdt.afc_ops_service.dto.request.qr;

import com.vdt.afc_ops_service.validation.RequiredField;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateDynamicQrRequest {

    @RequiredField(fieldName = "cardId")
    @Size(max = 100, message = "INVALID_SEARCH_KEYWORD")
    String cardId;
}
