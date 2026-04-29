package com.byteentropy.reconciliation_core.repository;

import com.byteentropy.event_core.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReconciliationRepository extends JpaRepository<PaymentEntity, String> {
    // Find everything stuck in UNCERTAIN status
    List<PaymentEntity> findByStatus(String status);
}
