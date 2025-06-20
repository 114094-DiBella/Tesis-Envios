package tesis.tesisenvios.entitites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tesis.tesisenvios.dtos.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "provider")
    private String provider = "ANDREANI";

    @Column(name = "service_type")
    private String serviceType;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    // Dirección de envío (JSON)
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    // Datos del destinatario
    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    // Costos y tiempos
    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    // Dimensiones del paquete
    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "declared_value", precision = 10, scale = 2)
    private BigDecimal declaredValue;

    // Metadatos
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "external_data", columnDefinition = "TEXT")
    private String externalData; // JSON con datos del proveedor

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrackingEventEntity> trackingEvents = new ArrayList<>();
}

