package tesis.tesisenvios.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tesis.tesisenvios.dtos.CreateShipmentRequest;
import tesis.tesisenvios.dtos.QuoteRequest;
import tesis.tesisenvios.dtos.ShippingQuoteResponse;
import tesis.tesisenvios.dtos.TrackingEventResponse;
import tesis.tesisenvios.entitites.ShipmentEntity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@ConditionalOnProperty(name = "andreani.mock.enabled", havingValue = "false")
@Slf4j
public class AndreaniProviderService {

    @Value("${andreani.api.url}")
    private String andreaniApiUrl;

    @Value("${andreani.api.user}")
    private String andreaniUser;

    @Value("${andreani.api.password}")
    private String andreaniPassword;

    @Value("${andreani.api.client}")
    private String andreaniClient;

    @Value("${andreani.api.contract}")
    private String andreaniContract;

    @Value("${shop.address.street}")
    private String shopStreet;

    @Value("${shop.address.number}")
    private String shopNumber;

    @Value("${shop.address.city}")
    private String shopCity;

    @Value("${shop.address.province}")
    private String shopProvince;

    @Value("${shop.address.postal-code}")
    private String shopPostalCode;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String cachedToken;
    private LocalDateTime tokenExpiry;

    /**
     * Obtener token de autenticación
     */
    public String getAuthToken() {
        if (cachedToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        try {
            String url = andreaniApiUrl + "/auth";

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("usuario", andreaniUser);
            authRequest.put("password", andreaniPassword);
            authRequest.put("cliente", andreaniClient);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authRequest, headers);

            log.info("Solicitando token a Andreani...");
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cachedToken = (String) response.getBody().get("token");
                tokenExpiry = LocalDateTime.now().plusMinutes(50); // Token válido por ~1 hora, renovar antes

                log.info("Token obtenido exitosamente");
                return cachedToken;
            }

        } catch (Exception e) {
            log.error("Error obteniendo token de Andreani: {}", e.getMessage());
        }

        throw new RuntimeException("No se pudo obtener token de Andreani");
    }

    /**
     * Obtener cotizaciones
     */
    public List<ShippingQuoteResponse> getQuotes(QuoteRequest request) {
        try {
            String url = andreaniApiUrl + "/cotizaciones";
            String token = getAuthToken();

            // Construir request para Andreani
            Map<String, Object> andreaniRequest = buildCotizacionRequest(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(andreaniRequest, headers);

            log.info("Solicitando cotización a Andreani para {}",
                    request.getDestinationAddress().getCity());

            ResponseEntity<List> response = restTemplate.postForEntity(url, entity, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> andreaniQuotes = response.getBody();

                return andreaniQuotes.stream()
                        .map(this::mapToShippingQuote)
                        .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
            }

        } catch (Exception e) {
            log.error("Error obteniendo cotizaciones de Andreani: {}", e.getMessage());
        }

        // Si falla, retornar cotización por defecto
        return getDefaultQuotes();
    }

    /**
     * Crear envío en Andreani
     */
    public String createShipment(ShipmentEntity shipment, CreateShipmentRequest request) {
        try {
            String url = andreaniApiUrl + "/ordenes";
            String token = getAuthToken();

            // Construir request para Andreani
            Map<String, Object> andreaniRequest = buildOrdenRequest(shipment, request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(andreaniRequest, headers);

            log.info("Creando orden en Andreani para: {}", request.getOrderCode());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // Extraer número de envío
                List<Map<String, Object>> bultos = (List<Map<String, Object>>) responseBody.get("bultos");
                if (bultos != null && !bultos.isEmpty()) {
                    String numeroEnvio = (String) bultos.get(0).get("numeroDeEnvio");

                    log.info("Orden creada exitosamente en Andreani: {}", numeroEnvio);
                    return numeroEnvio;
                }
            }

        } catch (Exception e) {
            log.error("Error creando orden en Andreani: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Obtener eventos de tracking
     */
    public List<TrackingEventResponse> getTrackingEvents(String trackingNumber) {
        try {
            String url = andreaniApiUrl + "/trazabilidad/" + trackingNumber;
            String token = getAuthToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> eventos = (List<Map<String, Object>>) responseBody.get("eventos");

                if (eventos != null) {
                    return eventos.stream()
                            .map(this::mapToTrackingEvent)
                            .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
                }
            }

        } catch (Exception e) {
            log.error("Error obteniendo tracking de Andreani para {}: {}", trackingNumber, e.getMessage());
        }

        return new ArrayList<>();
    }

    // ================================
    // MÉTODOS PRIVADOS DE MAPEO
    // ================================

    private Map<String, Object> buildCotizacionRequest(QuoteRequest request) {
        Map<String, Object> cotizacion = new HashMap<>();
        cotizacion.put("contrato", andreaniContract);

        // Origen (tu tienda)
        Map<String, Object> origen = new HashMap<>();
        Map<String, Object> origenPostal = new HashMap<>();
        origenPostal.put("codigoPostal", shopPostalCode);
        origenPostal.put("calle", shopStreet);
        origenPostal.put("numero", shopNumber);
        origenPostal.put("localidad", shopCity);
        origenPostal.put("region", mapProvinceToRegion(shopProvince));
        origenPostal.put("pais", "Argentina");
        origen.put("postal", origenPostal);
        cotizacion.put("origen", origen);

        // Destino
        Map<String, Object> destino = new HashMap<>();
        Map<String, Object> destinoPostal = new HashMap<>();
        destinoPostal.put("codigoPostal", request.getDestinationAddress().getPostalCode());
        destinoPostal.put("calle", request.getDestinationAddress().getStreet());
        destinoPostal.put("numero", request.getDestinationAddress().getStreetNumber());
        destinoPostal.put("localidad", request.getDestinationAddress().getCity());
        destinoPostal.put("region", mapProvinceToRegion(request.getDestinationAddress().getProvince()));
        destinoPostal.put("pais", "Argentina");
        destino.put("postal", destinoPostal);
        cotizacion.put("destino", destino);

        // Paquetes
        List<Map<String, Object>> paquetes = new ArrayList<>();
        Map<String, Object> paquete = new HashMap<>();
        paquete.put("pesoKilogramos", request.getWeightKg().doubleValue());
        paquete.put("volumenCentimetrosCubicos", calculateVolume(request.getWeightKg()));
        paquete.put("categoria", "Productos varios");
        if (request.getDeclaredValue() != null) {
            paquete.put("valorDeclaradoConIva", request.getDeclaredValue().doubleValue());
        }
        paquetes.add(paquete);
        cotizacion.put("paquetes", paquetes);

        return cotizacion;
    }

    private Map<String, Object> buildOrdenRequest(ShipmentEntity shipment, CreateShipmentRequest request) {
        Map<String, Object> orden = new HashMap<>();
        orden.put("contrato", andreaniContract);

        // Usar la misma estructura que cotización
        QuoteRequest quoteReq = new QuoteRequest();
        quoteReq.setDestinationAddress(request.getShippingAddress());
        quoteReq.setWeightKg(request.getWeightKg());
        quoteReq.setDeclaredValue(request.getDeclaredValue());

        Map<String, Object> cotizacionData = buildCotizacionRequest(quoteReq);
        orden.put("origen", cotizacionData.get("origen"));
        orden.put("destino", cotizacionData.get("destino"));
        orden.put("paquetes", cotizacionData.get("paquetes"));

        // Datos adicionales para la orden
        orden.put("remitente", "Tu Tienda Online");
        orden.put("destinatario", request.getRecipientName());

        if (request.getRecipientPhone() != null) {
            orden.put("telefono", request.getRecipientPhone());
        }

        if (request.getRecipientEmail() != null) {
            orden.put("email", request.getRecipientEmail());
        }

        return orden;
    }

    private ShippingQuoteResponse mapToShippingQuote(Map<String, Object> andreaniQuote) {
        ShippingQuoteResponse quote = new ShippingQuoteResponse();
        quote.setProvider("ANDREANI");
        quote.setServiceType((String) andreaniQuote.get("modalidad"));
        quote.setServiceName("Andreani - " + andreaniQuote.get("modalidad"));

        Object tarifa = andreaniQuote.get("tarifaConIva");
        if (tarifa instanceof Number) {
            quote.setPrice(BigDecimal.valueOf(((Number) tarifa).doubleValue()));
        }

        String plazo = (String) andreaniQuote.get("plazoEntrega");
        quote.setEstimatedDays(parsePlazoEntrega(plazo));
        quote.setDescription("Envío con Andreani - " + plazo);

        return quote;
    }

    private TrackingEventResponse mapToTrackingEvent(Map<String, Object> andreaniEvent) {
        TrackingEventResponse event = new TrackingEventResponse();

        String fecha = (String) andreaniEvent.get("fecha");
        if (fecha != null) {
            try {
                event.setEventDate(LocalDateTime.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception e) {
                event.setEventDate(LocalDateTime.now());
            }
        }

        event.setStatus((String) andreaniEvent.get("estado"));
        event.setDescription((String) andreaniEvent.get("motivo"));
        event.setLocation((String) andreaniEvent.get("sucursal"));

        return event;
    }

    private List<ShippingQuoteResponse> getDefaultQuotes() {
        // Cotización por defecto si falla Andreani
        ShippingQuoteResponse defaultQuote = new ShippingQuoteResponse();
        defaultQuote.setProvider("ANDREANI");
        defaultQuote.setServiceType("STANDARD");
        defaultQuote.setServiceName("Andreani Standard");
        defaultQuote.setPrice(BigDecimal.valueOf(1500.0));
        defaultQuote.setEstimatedDays(3);
        defaultQuote.setDescription("Envío estándar con Andreani (cotización estimada)");

        return Arrays.asList(defaultQuote);
    }

    private String mapProvinceToRegion(String province) {
        // Mapeo de provincias argentinas a códigos de región
        Map<String, String> provinceMap = new HashMap<>();
        provinceMap.put("Buenos Aires", "AR-B");
        provinceMap.put("CABA", "AR-C");
        provinceMap.put("Córdoba", "AR-X");
        provinceMap.put("Santa Fe", "AR-S");
        provinceMap.put("Mendoza", "AR-M");
        provinceMap.put("Tucumán", "AR-T");
        provinceMap.put("Entre Ríos", "AR-E");
        provinceMap.put("Salta", "AR-A");
        provinceMap.put("Misiones", "AR-N");
        provinceMap.put("Chaco", "AR-H");
        provinceMap.put("Corrientes", "AR-W");
        provinceMap.put("Santiago del Estero", "AR-G");
        provinceMap.put("San Juan", "AR-J");
        provinceMap.put("Jujuy", "AR-Y");
        provinceMap.put("Río Negro", "AR-R");
        provinceMap.put("Formosa", "AR-P");
        provinceMap.put("Neuquén", "AR-Q");
        provinceMap.put("Chubut", "AR-U");
        provinceMap.put("San Luis", "AR-D");
        provinceMap.put("Catamarca", "AR-K");
        provinceMap.put("La Rioja", "AR-F");
        provinceMap.put("La Pampa", "AR-L");
        provinceMap.put("Santa Cruz", "AR-Z");
        provinceMap.put("Tierra del Fuego", "AR-V");

        return provinceMap.getOrDefault(province, "AR-B");
    }

    private Double calculateVolume(BigDecimal weight) {
        // Estimación: 1kg ≈ 2000 cm³ (ajustar según tus productos)
        return weight.doubleValue() * 2000.0;
    }

    private Integer parsePlazoEntrega(String plazo) {
        if (plazo == null) return 3;

        plazo = plazo.toLowerCase();
        if (plazo.contains("24") || plazo.contains("1")) return 1;
        if (plazo.contains("48") || plazo.contains("2")) return 2;
        if (plazo.contains("3")) return 3;
        if (plazo.contains("4")) return 4;
        if (plazo.contains("5")) return 5;

        return 3; // Default
    }
}