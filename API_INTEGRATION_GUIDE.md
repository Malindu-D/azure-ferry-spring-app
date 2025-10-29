# API Service Integration Guide

This document explains how your **test-api-service-app** should integrate with the Spring Export Email Application.

---

## Architecture Overview

```
Client/User
    ↓
API Service App (test-api-service-app)
    ↓ HTTP POST
Spring Export App (test-spring-export-app)
    ↓
Database ← Read config from test-function-app
    ↓
Azure Communication Services → Send Email
```

---

## Integration Methods

### Method 1: Direct REST Call (Recommended)

#### Java Spring Example

```java
package com.example.apiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExportForwardingService {

    @Value("${spring.export.app.url}")
    private String springExportAppUrl; // e.g., https://your-app.azuremicroservices.io

    private final RestTemplate restTemplate;

    public ExportForwardingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Forward export request to Spring Export App
     */
    public Map<String, Object> requestTableExport(String tableName, String recipientEmail,
                                                   String subject, List<String> ccEmails) {
        String endpoint = springExportAppUrl + "/api/export/table";

        log.info("Forwarding export request to: {}", endpoint);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tableName", tableName);
        requestBody.put("recipientEmail", recipientEmail);
        requestBody.put("subject", subject);
        requestBody.put("ccEmails", ccEmails);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Make POST request
            ResponseEntity<Map> response = restTemplate.postForEntity(
                endpoint,
                request,
                Map.class
            );

            log.info("Export request successful. Status: {}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to forward export request", e);
            throw new RuntimeException("Export service unavailable: " + e.getMessage());
        }
    }
}
```

#### Controller Example

```java
package com.example.apiservice.controller;

import com.example.apiservice.service.ExportForwardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final ExportForwardingService exportForwardingService;

    /**
     * Endpoint that receives request and forwards to Spring Export App
     */
    @PostMapping("/request-export")
    public ResponseEntity<Map<String, Object>> requestExport(
            @RequestParam String tableName,
            @RequestParam String recipientEmail,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) List<String> ccEmails) {

        Map<String, Object> result = exportForwardingService.requestTableExport(
            tableName,
            recipientEmail,
            subject != null ? subject : "Data Export: " + tableName,
            ccEmails
        );

        return ResponseEntity.ok(result);
    }
}
```

#### Configuration (application.yml)

```yaml
spring:
  export:
    app:
      # Local development
      url: http://localhost:8080
      # Production (Azure Spring Apps)
      # url: https://spring-export-app.azuremicroservices.io
```

---

### Method 2: Using WebClient (Reactive)

```java
package com.example.apiservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReactiveExportService {

    private final WebClient webClient;

    public ReactiveExportService(@Value("${spring.export.app.url}") String baseUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public Mono<Map<String, Object>> requestTableExport(String tableName, String recipientEmail) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tableName", tableName);
        requestBody.put("recipientEmail", recipientEmail);

        return webClient.post()
            .uri("/api/export/table")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class);
    }
}
```

---

### Method 3: Using Feign Client

```java
package com.example.apiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "spring-export-service",
    url = "${spring.export.app.url}"
)
public interface ExportServiceClient {

    @PostMapping("/api/export/table")
    Map<String, Object> exportTable(@RequestBody ExportRequest request);

    // DTO class
    record ExportRequest(
        String tableName,
        String recipientEmail,
        String subject,
        java.util.List<String> ccEmails
    ) {}
}
```

---

## Request/Response Examples

### Request to API Service App

```http
POST http://localhost:8081/api/v1/request-export
Content-Type: application/x-www-form-urlencoded

tableName=employees&recipientEmail=manager@company.com&subject=Employee Report
```

Or JSON:

```http
POST http://localhost:8081/api/v1/request-export
Content-Type: application/json

{
  "tableName": "employees",
  "recipientEmail": "manager@company.com",
  "subject": "Employee Report",
  "ccEmails": ["hr@company.com"]
}
```

### API Service → Spring Export App

```http
POST https://spring-export-app.azuremicroservices.io/api/export/table
Content-Type: application/json

{
  "tableName": "employees",
  "recipientEmail": "manager@company.com",
  "subject": "Employee Report",
  "ccEmails": ["hr@company.com"]
}
```

### Response from Spring Export App

```json
{
  "status": "success",
  "message": "Data exported and email sent successfully",
  "messageId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "tableName": "employees"
}
```

---

## Error Handling

### API Service Error Handler

```java
package com.example.apiservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleClientError(HttpClientErrorException e) {
        log.error("Client error from Spring Export App: {}", e.getMessage());
        return ResponseEntity
            .status(e.getStatusCode())
            .body(Map.of(
                "error", "Invalid request",
                "message", e.getMessage()
            ));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleServerError(HttpServerErrorException e) {
        log.error("Server error from Spring Export App: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "error", "Export service error",
                "message", "The export service is temporarily unavailable"
            ));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleConnectionError(ResourceAccessException e) {
        log.error("Connection error to Spring Export App: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "error", "Service unavailable",
                "message", "Cannot connect to export service"
            ));
    }
}
```

---

## Complete Example: API Service App Structure

```
test-api-service-app/
├── src/main/java/com/example/apiservice/
│   ├── ApiServiceApplication.java
│   ├── controller/
│   │   └── ApiController.java              # Receives client requests
│   ├── service/
│   │   └── ExportForwardingService.java    # Forwards to Spring Export App
│   ├── config/
│   │   └── RestTemplateConfig.java         # Configure RestTemplate
│   └── exception/
│       └── GlobalExceptionHandler.java     # Error handling
└── src/main/resources/
    └── application.yml                      # Configuration
```

### RestTemplate Configuration

```java
package com.example.apiservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }
}
```

---

## Testing Integration

### Unit Test Example

```java
package com.example.apiservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportForwardingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExportForwardingService service;

    @Test
    void testRequestTableExport_Success() {
        // Arrange
        Map<String, Object> expectedResponse = Map.of(
            "status", "success",
            "messageId", "test-message-id"
        );

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        Map<String, Object> result = service.requestTableExport(
            "employees",
            "test@example.com",
            "Test Subject",
            List.of()
        );

        // Assert
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }
}
```

---

## Production Deployment

### Both Apps in Azure Spring Apps

```powershell
# Deploy API Service App
az spring app deploy `
  --name api-service-app `
  --resource-group <your-rg> `
  --service <your-spring-apps> `
  --artifact-path target/api-service-app.jar

# Get Spring Export App URL
$EXPORT_APP_URL = az spring app show `
  --name spring-export-app `
  --resource-group <your-rg> `
  --service <your-spring-apps> `
  --query "properties.url" -o tsv

# Configure API Service to call Export App
az spring app update `
  --name api-service-app `
  --resource-group <your-rg> `
  --service <your-spring-apps> `
  --env SPRING_EXPORT_APP_URL="https://$EXPORT_APP_URL"
```

### Service-to-Service Communication

Both apps in same Azure Spring Apps instance can communicate via:

- Internal URLs: `http://spring-export-app:8080/api/export/table`
- External URLs: `https://spring-export-app.azuremicroservices.io/api/export/table`

---

## Monitoring & Logging

### Add Correlation IDs

```java
@PostMapping("/request-export")
public ResponseEntity<Map<String, Object>> requestExport(...) {
    String correlationId = UUID.randomUUID().toString();
    MDC.put("correlationId", correlationId);

    log.info("Received export request - CorrelationId: {}", correlationId);

    // Add to request headers
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Correlation-ID", correlationId);

    // ... forward request
}
```

---

## Quick Start Checklist

- [ ] Add dependency: `spring-boot-starter-web` to API service app
- [ ] Configure Spring Export App URL in `application.yml`
- [ ] Create `ExportForwardingService`
- [ ] Add REST endpoint in controller
- [ ] Configure RestTemplate bean
- [ ] Add error handling
- [ ] Test locally
- [ ] Deploy both apps to Azure Spring Apps
- [ ] Configure environment variables
- [ ] Test end-to-end flow

---

## Troubleshooting

### Issue: Connection refused

**Solution**: Check if Spring Export App is running and URL is correct

### Issue: 404 Not Found

**Solution**: Verify endpoint path: `/api/export/table`

### Issue: Timeout

**Solution**: Increase timeout in RestTemplate configuration

---

## Summary

Your **test-api-service-app** acts as a middleware that:

1. Receives requests from clients
2. Forwards to **test-spring-export-app**
3. Returns response to client

The Spring Export App handles:

1. Database connection (using config from test-function-app)
2. Data retrieval
3. Email sending
