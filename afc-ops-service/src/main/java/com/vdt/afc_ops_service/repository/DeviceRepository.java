package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findAllByStationOrderByDeviceCodeAsc(Station station);
}
