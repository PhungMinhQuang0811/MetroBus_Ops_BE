package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.entity.Operator;

public interface IMediaAccessRulePackageService {
    void refreshAndPublishForOperator(Operator operator);
}