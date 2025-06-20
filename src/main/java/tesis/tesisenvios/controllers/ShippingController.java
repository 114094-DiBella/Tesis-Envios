package tesis.tesisenvios.controllers;


import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import tesis.tesisenvios.dtos.CreateShipmentRequest;
import tesis.tesisenvios.dtos.QuoteRequest;
import tesis.tesisenvios.dtos.ShipmentResponse;
import tesis.tesisenvios.dtos.ShippingQuoteResponse;
import tesis.tesisenvios.services.ShippingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    /**
     * Obtener cotizaciones de envío
     */
    @PostMapping("/quotes")
    public ResponseEntity<List<ShippingQuoteResponse>> getShippingQuotes(@Valid @RequestBody QuoteRequest request) {
        try {
            List<ShippingQuoteResponse> quotes = shippingService.getShippingQuotes(request);
            return ResponseEntity.ok(quotes);
        } catch (Exception e) {
            log.error("Error obteniendo cotizaciones: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Crear un envío
     */
    @PostMapping("/create")
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        try {
            ShipmentResponse shipment = shippingService.createShipment(request);
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            log.error("Error creando envío: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Obtener envío por código de orden
     */
    @GetMapping("/order/{orderCode}")
    public ResponseEntity<ShipmentResponse> getShipmentByOrder(@PathVariable String orderCode) {
        try {
            ShipmentResponse shipment = shippingService.getShipmentByOrderCode(orderCode);
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            log.error("Error obteniendo envío por orden {}: {}", orderCode, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener seguimiento por número de tracking
     */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> trackShipment(@PathVariable String trackingNumber) {
        try {
            ShipmentResponse shipment = shippingService.getShipmentByTrackingNumber(trackingNumber);
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            log.error("Error obteniendo tracking {}: {}", trackingNumber, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancelar un envío
     */
    @DELETE("/order/{orderCode}")
    public ResponseEntity<Map<String, Object>> cancelShipment(@PathVariable String orderCode) {
        try {
            boolean cancelled = shippingService.cancelShipment(orderCode);

            if (cancelled) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Envío cancelado exitosamente"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No se pudo cancelar el envío"
                ));
            }
        } catch (Exception e) {
            log.error("Error cancelando envío {}: {}", orderCode, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error interno del servidor"
            ));
        }
    }

    /**
     * Actualizar tracking manualmente
     */
    @PostMapping("/update-tracking")
    public ResponseEntity<Map<String, String>> updateTracking() {
        try {
            shippingService.updateAllActiveShipments();
            return ResponseEntity.ok(Map.of(
                    "success", "true",
                    "message", "Tracking actualizado"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", "false",
                    "message", "Error actualizando tracking"
            ));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "shipping-service",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}