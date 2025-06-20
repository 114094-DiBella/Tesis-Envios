package tesis.tesisenvios.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingAddressRequest {
    @NotBlank
    private String street;

    @NotBlank
    private String streetNumber;

    private String apartment;

    @NotBlank
    private String city;

    @NotBlank
    private String province;

    @NotBlank
    private String postalCode;

    private String additionalInfo;
}