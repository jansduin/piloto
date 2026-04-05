# CDI API Gateway - Fase 6 Integrated 🚀

## Overview

API Gateway REST para el CDI Cognitive Kernel. Implementa comunicación HTTP entre UI clients y el núcleo cognitivo del PILOTO.

## Arquitectura

```
UI Layer (React/Vue/CLI)
     ↕ REST + WebSocket
API Gateway (Spring Boot) 
     ↕ Java API
CDI Kernel (business logic)
```

## Estructura del Proyecto

```
cdi-api-gateway/
├── pom.xml                                    # Maven configuration
├── src/main/java/com/piloto/cdi/gateway/
│   ├── CdiGatewayApplication.java             # Main Spring Boot app
│   ├── config/
│   │   └── CorsConfig.java                    # CORS configuration
│   ├── controller/
│   │   └── ChatController.java                # REST endpoints (/api/chat/*)
│   ├── service/
│   │   └── KernelBridgeService.java           # Bridge to CDI Kernel
│   └── dto/
│       ├── ChatMessageRequest.java            # Request DTO
│       └── ChatMessageResponse.java           # Response DTO
├── src/main/resources/
│   └── application.yml                        # Spring Boot config
└── src/test/java/com/piloto/cdi/gateway/
    ├── controller/
    │   └── ChatControllerTest.java            # Integration tests
    └── service/
        └── KernelBridgeServiceTest.java       # Unit tests
```

## API Endpoints Implemented

### POST /api/chat/message

Envía un mensaje al CDI Kernel.

**Request**:

```json
{
  "message": "Analiza los logs del sistema",
  "sessionId": "session_abc123",
  "tenantId": "tenant_001"
}
```

**Response**:

```json
{
  "success": true,
  "messageId": "msg_12345",
  "response": "PILOTO recibió tu mensaje...",
  "goalId": "goal_xyz",
  "metadata": {
    "executionTimeMs": 50
  }
}
```

### GET /api/chat/health

Health check del servicio.

**Response**:

```json
{
  "status": "UP",
  "service": "chat",
  "timestamp": 1708119600000
}
```

## Tecnologías Utilizadas

- **Spring Boot 3.2.2** - Framework backend
- **Spring Web** - REST endpoints
- **Jackson** - JSON serialization
- **JUnit 5** - Testing framework
- **MockMvc** - Integration test support

## Cómo Compilar y Ejecutar

### Prerequisitos

- Java 17+
- Maven 3.8+

### Compilar

```bash
cd cdi-api-gateway
mvn clean install
```

### Ejecutar Tests

```bash
mvn test
```

### Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

La aplicación estará disponible en: `http://localhost:8080`

### Probar el Endpoint

```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hola PILOTO",
    "sessionId": "session_test",
    "tenantId": "tenant_test"
  }'
```

## Estado Actual

✅ **FASE 1 COMPLETADA**

- [x] Estructura de proyecto Spring Boot
- [x] Configuración CORS
- [x] DTOs para request/response
- [x] KernelBridgeService (INTEGRADO CON KERNEL REAL)
- [x] ChatController REST
- [x] Tests de integración
- [x] Tests unitarios
- [x] Integración real con CDI Kernel (Fase 6 Orrchestrator)
- [x] WebSocket para eventos de deliberación (Fase 4 Signal)

⏳ **PENDIENTE**

- [ ] Integración real con CDI Kernel (actualmente en modo mock)
- [ ] WebSocket para eventos en tiempo real
- [ ] ApprovalController para aprobaciones de tools

## Notas de Implementación

### Real Integration (Phase 6)

El `KernelBridgeService` está plenamente integrado con el `ReasoningOrchestrator` del Kernel CDI. El flujo de deliberación multi-agente está activo y se comunica mediante eventos de Spring hacia los clientes WebSocket.

### CORS

La configuración CORS está configurada para desarrollo local:

- `http://localhost:5173` (Vite default)
- `http://localhost:3000` (Create React App default)

Para producción, ajustar en `application.yml`.

### Testing

- **Integration Tests**: `ChatControllerTest` - Usa `@WebMvcTest` con mocks
- **Unit Tests**: `KernelBridgeServiceTest` - Valida lógica de validación

---

**Proyecto**: CDI API Gateway  
**Versión**: 1.0.0-SNAPSHOT  
**Fecha**: 2026-02-25  
**Status**: Fase 6 Integrated 🚀
