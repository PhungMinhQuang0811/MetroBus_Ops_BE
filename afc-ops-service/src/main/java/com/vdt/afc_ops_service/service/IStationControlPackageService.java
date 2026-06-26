package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.entity.Station;

public interface IStationControlPackageService {
    void createOrUpdateStationContext(Station station);
}