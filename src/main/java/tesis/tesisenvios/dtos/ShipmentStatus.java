package tesis.tesisenvios.dtos;


public enum ShipmentStatus {
    PENDING,           // Pendiente de crear
    QUOTE_REQUESTED,   // Cotización solicitada
    QUOTED,           // Cotizado
    CREATED,          // Creado en el proveedor
    PICKED_UP,        // Retirado
    IN_TRANSIT,       // En tránsito
    OUT_FOR_DELIVERY, // Salió para entrega
    DELIVERED,        // Entregado
    FAILED_DELIVERY,  // Falló la entrega
    RETURNED,         // Devuelto
    CANCELLED,        // Cancelado
    ERROR             // Error
}