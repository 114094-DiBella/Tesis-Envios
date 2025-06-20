package tesis.tesisenvios.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateShipmentRequest {
    @NotBlank
    private String orderCode;

    @NotNull
    private ShippingAddressRequest shippingAddress;

    @NotBlank
    private String recipientName;

    @NotBlank
    private String recipientEmail;

    private String recipientPhone;

    private String serviceType = "STANDARD";

    private BigDecimal declaredValue;

    private BigDecimal weightKg = BigDecimal.valueOf(1.0);
}