package tesis.tesisenvios.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentResponse {
    private String id;
    private String orderCode;
    private String trackingNumber;
    private String provider;
    private String serviceType;
    private String status;
    private ShippingAddressResponse shippingAddress;
    private String recipientName;
    private String recipientEmail;
    private BigDecimal shippingCost;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private LocalDateTime createdAt;
    private List<TrackingEventResponse> trackingEvents;
}