package com.vdt.afc_ops_service.repository;

import com.vdt.afc_ops_service.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    Optional<Card> findByCardUid(String cardUid);

    @Query("SELECT c FROM Card c WHERE c.status IN :statuses")
    List<Card> findByStatusIn(@Param("statuses") List<String> statuses);
}
