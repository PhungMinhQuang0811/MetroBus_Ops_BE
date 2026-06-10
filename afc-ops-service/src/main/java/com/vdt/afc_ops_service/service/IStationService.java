package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.station.CreateStationRequest;
import com.vdt.afc_ops_service.dto.request.station.UpdateStationRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDetailResponse;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;

public interface IStationService {

    PageResponse<StationResponse> listStations(Long routeId, String keyword, String status, int page, int size);

    StationDetailResponse getStation(Long stationId);

    StationResponse createStation(CreateStationRequest request);

    StationResponse updateStation(Long stationId, UpdateStationRequest request);

    StationResponse enableStation(Long stationId);

    StationResponse disableStation(Long stationId);
}
