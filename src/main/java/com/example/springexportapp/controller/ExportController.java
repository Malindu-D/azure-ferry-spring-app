package com.example.springexportapp.controller;

import com.example.springexportapp.model.DataExportRequest;
import com.example.springexportapp.service.DataExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final DataExportService dataExportService;

    /**
     * Endpoint to receive export request from API service
     * Retrieves table data and sends via email
     */
    @PostMapping("/table")
    public ResponseEntity<Map<String, String>> exportTableData(@Valid @RequestBody DataExportRequest request) {
        log.info("Received export request for table: {}", request.getTableName());
        
        try {
            String messageId = dataExportService.exportAndSendTableData(request);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Data exported and email sent successfully",
                "messageId", messageId,
                "tableName", request.getTableName()
            ));
        } catch (Exception e) {
            log.error("Error processing export request", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to export and send data: " + e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Spring Export Email Application"
        ));
    }
}
