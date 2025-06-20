package tesis.tesisenvios.services;


import org.springframework.stereotype.Service;
import tesis.tesisenvios.dtos.CreateShipmentRequest;
import tesis.tesisenvios.dtos.QuoteRequest;
import tesis.tesisenvios.dtos.ShipmentResponse;
import tesis.tesisenvios.dtos.ShippingQuoteResponse;

import java.util.List;
@Service
public interface ShippingService {

    /**
     * Obtener cotizaciones de envío
     */
    List<ShippingQuoteResponse> getShippingQuotes(QuoteRequest request);

    /**
     * Crear un envío
     */
    ShipmentResponse createShipment(CreateShipmentRequest request);

    /**
     * Obtener envío por código de orden
     */
    ShipmentResponse getShipmentByOrderCode(String orderCode);

    /**
     * Obtener seguimiento por número de tracking
     */
    ShipmentResponse getShipmentByTrackingNumber(String trackingNumber);

    /**
     * Actualizar tracking de todos los envíos activos
     */
    void updateAllActiveShipments();

    /**
     * Cancelar un envío
     */
    boolean cancelShipment(String orderCode);
}