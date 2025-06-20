package tesis.tesisenvios.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingAddressResponse {
    private String street;
    private String streetNumber;
    private String apartment;
    private String city;
    private String province;
    private String postalCode;
    private String additionalInfo;
}
