package tesis.tesisenvios.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tesis.tesisenvios.dtos.ShipmentStatus;
import tesis.tesisenvios.entitites.ShipmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<tesis.tesisenvios.entitites.ShipmentEntity, String> {

    Optional<tesis.tesisenvios.entitites.ShipmentEntity> findByOrderCode(String orderCode);
    Optional<tesis.tesisenvios.entitites.ShipmentEntity> findByTrackingNumber(String trackingNumber);
    List<tesis.tesisenvios.entitites.ShipmentEntity> findByStatusIn(List<ShipmentStatus> statuses);
    List<tesis.tesisenvios.entitites.ShipmentEntity> findByRecipientEmail(String email);
}