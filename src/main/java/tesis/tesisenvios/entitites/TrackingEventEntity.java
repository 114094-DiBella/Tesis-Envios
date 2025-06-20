package tesis.tesisenvios.entitites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_events")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackingEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private ShipmentEntity shipment;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "location")
    private String location;

    @Column(name = "provider_event_id")
    private String providerEventId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}