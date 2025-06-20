package tesis.tesisenvios.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackingEventResponse {
    private LocalDateTime eventDate;
    private String status;
    private String description;
    private String location;
}
