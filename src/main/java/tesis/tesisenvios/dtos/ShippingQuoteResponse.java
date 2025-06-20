package tesis.tesisenvios.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingQuoteResponse {
    private String provider = "ANDREANI";
    private String serviceType;
    private String serviceName;
    private BigDecimal price;
    private Integer estimatedDays;
    private String description;
}