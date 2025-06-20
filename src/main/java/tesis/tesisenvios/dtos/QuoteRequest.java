package tesis.tesisenvios.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuoteRequest {
    @NotNull
    private ShippingAddressRequest originAddress;

    @NotNull
    private ShippingAddressRequest destinationAddress;

    private BigDecimal weightKg = BigDecimal.valueOf(1.0);

    private BigDecimal declaredValue;
}