package tesis.tesisenvios.services.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tesis.tesisenvios.dtos.*;
import tesis.tesisenvios.entitites.ShipmentEntity;
import tesis.tesisenvios.entitites.TrackingEventEntity;
import tesis.tesisenvios.repositories.ShipmentRepository;
import tesis.tesisenvios.repositories.TrackingEventRepository;
import tesis.tesisenvios.services.AndreaniProviderService;
import tesis.tesisenvios.services.ShippingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ShippingServiceImpl implements ShippingService {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private AndreaniProviderService andreaniProviderService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ShippingQuoteResponse> getShippingQuotes(QuoteRequest request) {
        try {
            log.info("Obteniendo cotizaciones para envío a {}, {}",
                    request.getDestinationAddress().getCity(),
                    request.getDestinationAddress().getPostalCode());

            return andreaniProviderService.getQuotes(request);

        } catch (Exception e) {
            log.error("Error obteniendo cotizaciones: {}", e.getMessage());
            return List.of(); // Retornar lista vacía en caso de error
        }
    }

    @Override
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        try {
            log.info("Creando envío para orden: {}", request.getOrderCode());

            // Verificar si ya existe
            Optional<ShipmentEntity> existing = shipmentRepository.findByOrderCode(request.getOrderCode());
            if (existing.isPresent()) {
                log.warn("Ya existe envío para orden: {}", request.getOrderCode());
                return modelMapper.map(existing.get(), ShipmentResponse.class);
            }

            // Crear entidad
            ShipmentEntity shipment = new ShipmentEntity();
            shipment.setOrderCode(request.getOrderCode());
            shipment.setRecipientName(request.getRecipientName());
            shipment.setRecipientEmail(request.getRecipientEmail());
            shipment.setRecipientPhone(request.getRecipientPhone());
            shipment.setServiceType(request.getServiceType());
            shipment.setWeightKg(request.getWeightKg());
            shipment.setDeclaredValue(request.getDeclaredValue());
            shipment.setStatus(ShipmentStatus.PENDING);

            // Convertir dirección a JSON
            String addressJson = objectMapper.writeValueAsString(request.getShippingAddress());
            shipment.setShippingAddress(addressJson);

            // Guardar temporalmente
            shipment = shipmentRepository.save(shipment);

            // Crear en Andreani
            String trackingNumber = andreaniProviderService.createShipment(shipment, request);

            if (trackingNumber != null) {
                shipment.setTrackingNumber(trackingNumber);
                shipment.setStatus(ShipmentStatus.CREATED);

                // Obtener cotización para el costo
                List<ShippingQuoteResponse> quotes = getShippingQuotes(buildQuoteRequest(request));
                if (!quotes.isEmpty()) {
                    shipment.setShippingCost(quotes.get(0).getPrice());
                    shipment.setEstimatedDeliveryDate(
                            LocalDateTime.now().plusDays(quotes.get(0).getEstimatedDays())
                    );
                }

                shipment = shipmentRepository.save(shipment);

                log.info("Envío creado exitosamente: {} -> {}",
                        request.getOrderCode(), trackingNumber);
            } else {
                shipment.setStatus(ShipmentStatus.ERROR);
                shipment = shipmentRepository.save(shipment);
                log.error("Error creando envío en Andreani para orden: {}", request.getOrderCode());
            }

            return modelMapper.map(shipment, ShipmentResponse.class);

        } catch (Exception e) {
            log.error("Error creando envío para orden {}: {}", request.getOrderCode(), e.getMessage());
            throw new RuntimeException("Error creando envío: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderCode(String orderCode) {
        Optional<ShipmentEntity> shipment = shipmentRepository.findByOrderCode(orderCode);

        if (shipment.isPresent()) {
            ShipmentResponse response = modelMapper.map(shipment.get(), ShipmentResponse.class);

            // Agregar eventos de tracking
            List<TrackingEventEntity> events = trackingEventRepository
                    .findByShipmentIdOrderByEventDateDesc(shipment.get().getId());

            response.setTrackingEvents(events.stream()
                    .map(event -> modelMapper.map(event, TrackingEventResponse.class))
                    .collect(Collectors.toList()));

            return response;
        }

        throw new RuntimeException("Envío no encontrado para orden: " + orderCode);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Optional<ShipmentEntity> shipment = shipmentRepository.findByTrackingNumber(trackingNumber);

        if (shipment.isPresent()) {
            // Actualizar tracking antes de devolver
            updateShipmentTracking(shipment.get());

            return getShipmentByOrderCode(shipment.get().getOrderCode());
        }

        throw new RuntimeException("Envío no encontrado con tracking: " + trackingNumber);
    }

    @Override
    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    public void updateAllActiveShipments() {
        log.info("Iniciando actualización de envíos activos");

        List<ShipmentStatus> activeStatuses = Arrays.asList(
                ShipmentStatus.CREATED,
                ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY
        );

        List<ShipmentEntity> activeShipments = shipmentRepository.findByStatusIn(activeStatuses);

        log.info("Actualizando {} envíos activos", activeShipments.size());

        for (ShipmentEntity shipment : activeShipments) {
            try {
                updateShipmentTracking(shipment);
                Thread.sleep(1000); // Evitar rate limiting
            } catch (Exception e) {
                log.error("Error actualizando envío {}: {}",
                        shipment.getTrackingNumber(), e.getMessage());
            }
        }

        log.info("Actualización de envíos completada");
    }

    @Override
    public boolean cancelShipment(String orderCode) {
        try {
            Optional<ShipmentEntity> shipmentOpt = shipmentRepository.findByOrderCode(orderCode);

            if (shipmentOpt.isPresent()) {
                ShipmentEntity shipment = shipmentOpt.get();

                // Solo se puede cancelar si no está en tránsito
                if (shipment.getStatus() == ShipmentStatus.PENDING ||
                        shipment.getStatus() == ShipmentStatus.CREATED) {

                    shipment.setStatus(ShipmentStatus.CANCELLED);
                    shipmentRepository.save(shipment);

                    log.info("Envío cancelado: {}", orderCode);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Error cancelando envío {}: {}", orderCode, e.getMessage());
            return false;
        }
    }

    private void updateShipmentTracking(ShipmentEntity shipment) {
        if (shipment.getTrackingNumber() == null) return;

        try {
            List<TrackingEventResponse> newEvents = andreaniProviderService
                    .getTrackingEvents(shipment.getTrackingNumber());

            for (TrackingEventResponse eventResponse : newEvents) {
                // Verificar si el evento ya existe
                boolean eventExists = shipment.getTrackingEvents().stream()
                        .anyMatch(existing ->
                                existing.getEventDate().equals(eventResponse.getEventDate()) &&
                                        existing.getStatus().equals(eventResponse.getStatus())
                        );

                if (!eventExists) {
                    TrackingEventEntity newEvent = new TrackingEventEntity();
                    newEvent.setShipment(shipment);
                    newEvent.setEventDate(eventResponse.getEventDate());
                    newEvent.setStatus(eventResponse.getStatus());
                    newEvent.setDescription(eventResponse.getDescription());
                    newEvent.setLocation(eventResponse.getLocation());

                    trackingEventRepository.save(newEvent);

                    // Actualizar estado del envío si es necesario
                    updateShipmentStatus(shipment, eventResponse.getStatus());
                }
            }

        } catch (Exception e) {
            log.error("Error actualizando tracking para {}: {}",
                    shipment.getTrackingNumber(), e.getMessage());
        }
    }

    private void updateShipmentStatus(ShipmentEntity shipment, String providerStatus) {
        ShipmentStatus newStatus = mapProviderStatusToShipmentStatus(providerStatus);

        if (newStatus != shipment.getStatus()) {
            shipment.setStatus(newStatus);

            if (newStatus == ShipmentStatus.DELIVERED) {
                shipment.setActualDeliveryDate(LocalDateTime.now());
            }

            shipmentRepository.save(shipment);

            log.info("Estado actualizado para envío {}: {} -> {}",
                    shipment.getTrackingNumber(), shipment.getStatus(), newStatus);
        }
    }

    private ShipmentStatus mapProviderStatusToShipmentStatus(String providerStatus) {
        // Mapear estados de Andreani a nuestros estados
        switch (providerStatus.toLowerCase()) {
            case "entregado":
            case "delivered":
                return ShipmentStatus.DELIVERED;
            case "en reparto":
            case "out_for_delivery":
                return ShipmentStatus.OUT_FOR_DELIVERY;
            case "en transito":
            case "in_transit":
                return ShipmentStatus.IN_TRANSIT;
            case "retirado":
            case "picked_up":
                return ShipmentStatus.PICKED_UP;
            case "devuelto":
            case "returned":
                return ShipmentStatus.RETURNED;
            default:
                return ShipmentStatus.IN_TRANSIT;
        }
    }

    private QuoteRequest buildQuoteRequest(CreateShipmentRequest request) {
        QuoteRequest quoteRequest = new QuoteRequest();

        // Origen (tu tienda) - deberías configurar esto
        ShippingAddressRequest origin = new ShippingAddressRequest();
        origin.setStreet("Tu Calle");
        origin.setStreetNumber("123");
        origin.setCity("Córdoba");
        origin.setProvince("Córdoba");
        origin.setPostalCode("5000");

        quoteRequest.setOriginAddress(origin);
        quoteRequest.setDestinationAddress(request.getShippingAddress());
        quoteRequest.setWeightKg(request.getWeightKg());
        quoteRequest.setDeclaredValue(request.getDeclaredValue());

        return quoteRequest;
    }
}