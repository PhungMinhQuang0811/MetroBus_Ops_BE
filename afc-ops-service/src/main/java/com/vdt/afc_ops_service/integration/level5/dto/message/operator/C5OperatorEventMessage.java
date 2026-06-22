package com.vdt.afc_ops_service.integration.level5.dto.message.operator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class C5OperatorEventMessage {

    private UUID operatorId;
    private String code;
    private String name;
    private String status;
    private String mode;
}
