package tesis.tesisenvios.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tesis.tesisenvios.entitites.TrackingEventEntity;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEventEntity, Long> {
    List<TrackingEventEntity> findByShipmentIdOrderByEventDateDesc(String shipmentId);
}