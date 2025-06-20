package tesis.tesisenvios.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tesis.tesisenvios.dtos.CreateShipmentRequest;
import tesis.tesisenvios.dtos.QuoteRequest;
import tesis.tesisenvios.dtos.ShippingQuoteResponse;
import tesis.tesisenvios.dtos.TrackingEventResponse;
import tesis.tesisenvios.entitites.ShipmentEntity;
import tesis.tesisenvios.services.AndreaniProviderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@ConditionalOnProperty(name = "andreani.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockAndreaniService extends AndreaniProviderService {

    private final Random random = new Random();

    @Override
    public String getAuthToken() {
        log.info("🔧 MOCK: Generando token simulado");
        return "mock-token-" + System.currentTimeMillis();
    }

    @Override
    public List<ShippingQuoteResponse> getQuotes(QuoteRequest request) {
        log.info("🔧 MOCK: Generando cotizaciones para {}, {}",
                request.getDestinationAddress().getCity(),
                request.getDestinationAddress().getPostalCode());

        // Simular diferentes opciones de envío
        ShippingQuoteResponse standard = new ShippingQuoteResponse();
        standard.setProvider("ANDREANI");
        standard.setServiceType("STANDARD");
        standard.setServiceName("Andreani Standard");
        standard.setPrice(calculateMockPrice(request, 1.0));
        standard.setEstimatedDays(calculateMockDays(request.getDestinationAddress().getProvince(), 3));
        standard.setDescription("Envío estándar a domicilio");

        ShippingQuoteResponse express = new ShippingQuoteResponse();
        express.setProvider("ANDREANI");
        express.setServiceType("EXPRESS");
        express.setServiceName("Andreani Express");
        express.setPrice(calculateMockPrice(request, 1.5));
        express.setEstimatedDays(calculateMockDays(request.getDestinationAddress().getProvince(), 1));
        express.setDescription("Envío express 24/48hs");

        ShippingQuoteResponse sucursal = new ShippingQuoteResponse();
        sucursal.setProvider("ANDREANI");
        sucursal.setServiceType("SUCURSAL");
        sucursal.setServiceName("Andreani Sucursal");
        sucursal.setPrice(calculateMockPrice(request, 0.8));
        sucursal.setEstimatedDays(calculateMockDays(request.getDestinationAddress().getProvince(), 2));
        sucursal.setDescription("Retiro en sucursal Andreani");

        return Arrays.asList(standard, express, sucursal);
    }

    @Override
    public String createShipment(ShipmentEntity shipment, CreateShipmentRequest request) {
        log.info("🔧 MOCK: Creando envío para orden: {}", request.getOrderCode());

        // Simular posible falla (5% de chance)
        if (random.nextInt(100) < 5) {
            log.warn("🔧 MOCK: Simulando falla en creación de envío");
            return null;
        }

        // Generar número de tracking simulado
        String trackingNumber = generateMockTrackingNumber();
        log.info("🔧 MOCK: Envío creado con tracking: {}", trackingNumber);

        return trackingNumber;
    }

    @Override
    public List<TrackingEventResponse> getTrackingEvents(String trackingNumber) {
        log.info("🔧 MOCK: Obteniendo eventos para tracking: {}", trackingNumber);

        // Simular eventos de tracking basados en el tiempo
        List<TrackingEventResponse> events = Arrays.asList(
                createMockEvent(
                        LocalDateTime.now().minusDays(2),
                        "CREADO",
                        "Orden creada en el sistema",
                        "Centro de Distribución Córdoba"
                ),
                createMockEvent(
                        LocalDateTime.now().minusDays(1),
                        "RETIRADO",
                        "Paquete retirado del origen",
                        "Centro de Distribución Córdoba"
                ),
                createMockEvent(
                        LocalDateTime.now().minusHours(12),
                        "EN_TRANSITO",
                        "En tránsito hacia destino",
                        "Centro de Distribución Buenos Aires"
                ),
                createMockEvent(
                        LocalDateTime.now().minusHours(2),
                        "SALIDA_REPARTO",
                        "Salió para entrega",
                        "Base de Reparto Zona Norte"
                )
        );

        // Simular entrega (30% de chance)
        if (random.nextInt(100) < 30) {
            events.add(createMockEvent(
                    LocalDateTime.now().minusMinutes(30),
                    "ENTREGADO",
                    "Paquete entregado al destinatario",
                    "Domicilio del destinatario"
            ));
        }

        return events;
    }

    // ================================
    // MÉTODOS AUXILIARES
    // ================================

    private BigDecimal calculateMockPrice(QuoteRequest request, double multiplier) {
        // Precio base por peso
        BigDecimal basePrice = request.getWeightKg().multiply(BigDecimal.valueOf(800));

        // Ajuste por provincia (simulado)
        double provinceFactor = getProvinceFactor(request.getDestinationAddress().getProvince());

        // Precio final
        BigDecimal finalPrice = basePrice
                .multiply(BigDecimal.valueOf(provinceFactor))
                .multiply(BigDecimal.valueOf(multiplier));

        // Redondear a centenas
        return finalPrice.setScale(0, BigDecimal.ROUND_UP);
    }

    private Integer calculateMockDays(String province, int baseDays) {
        // Ajustar días según provincia
        if ("Córdoba".equals(province) || "Buenos Aires".equals(province)) {
            return baseDays;
        } else if ("CABA".equals(province) || "Santa Fe".equals(province)) {
            return baseDays + 1;
        } else {
            return baseDays + 2;
        }
    }

    private double getProvinceFactor(String province) {
        switch (province.toLowerCase()) {
            case "córdoba":
            case "cordoba":
                return 1.0;
            case "buenos aires":
            case "caba":
                return 1.2;
            case "santa fe":
            case "mendoza":
                return 1.3;
            default:
                return 1.5;
        }
    }

    private String generateMockTrackingNumber() {
        // Formato similar a Andreani: AND + 10 dígitos
        return "AND" + String.format("%010d", random.nextInt(1000000000));
    }

    private TrackingEventResponse createMockEvent(LocalDateTime date, String status,
                                                  String description, String location) {
        TrackingEventResponse event = new TrackingEventResponse();
        event.setEventDate(date);
        event.setStatus(status);
        event.setDescription(description);
        event.setLocation(location);
        return event;
    }
}