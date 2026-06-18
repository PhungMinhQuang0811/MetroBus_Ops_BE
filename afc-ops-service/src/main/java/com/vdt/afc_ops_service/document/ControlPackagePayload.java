package com.vdt.afc_ops_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "control_package_payloads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlPackagePayload {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("control_package_id")
    private Long controlPackageId;

    @Field("package_type")
    private String packageType;

    @Field("source_type")
    private String sourceType;

    private Long version;

    private Map<String, Object> payload;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
