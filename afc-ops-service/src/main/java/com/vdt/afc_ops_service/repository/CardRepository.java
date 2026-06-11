package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findAllByExternalUserId(String externalUserId);
}
