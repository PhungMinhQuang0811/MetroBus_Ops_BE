package com.vdt.afc_ops_service.service.generator;

import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.repository.BatchRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BatchCodeGenerator {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    static final String BATCH_CODE_FORMAT = "%s-%04d";

    BatchRepository batchRepository;

    public String generate(Operator operator, LocalDate referenceDate) {
        String prefix = operator.getOperatorCode() + "-" + referenceDate.format(DATE_FORMAT);
        int nextSequence = batchRepository.findBatchCodesByOperatorAndPrefix(operator.getId(), prefix).stream()
                .map(batchCode -> parseSequence(batchCode, prefix))
                .max(Integer::compareTo)
                .orElse(0) + 1;

        String batchCode = String.format(BATCH_CODE_FORMAT, prefix, nextSequence);
        while (batchRepository.existsByBatchCode(batchCode)) {
            batchCode = String.format(BATCH_CODE_FORMAT, prefix, ++nextSequence);
        }
        return batchCode;
    }

    private int parseSequence(String batchCode, String prefix) {
        String expectedPrefix = prefix + "-";
        if (batchCode == null || !batchCode.startsWith(expectedPrefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(batchCode.substring(expectedPrefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
