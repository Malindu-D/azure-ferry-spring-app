package com.example.springexportapp.service;

import com.example.springexportapp.model.DataExportRequest;
import com.example.springexportapp.model.ExportData;
import com.example.springexportapp.repository.ExportDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

    private final ExportDataRepository exportDataRepository;
    private final EmailService emailService;

    /**
     * Main service method: Export table data and send via email
     */
    public String exportAndSendTableData(DataExportRequest request) {
        log.info("Starting export process for table: {}", request.getTableName());
        
        // Step 1: Get table data from database
        ExportData data = exportDataRepository.getTableData(request.getTableName());
        
        if (data.getTotalRows() == 0) {
            log.warn("Table {} is empty or does not exist", request.getTableName());
            throw new RuntimeException("Table is empty or does not exist: " + request.getTableName());
        }
        
        // Step 2: Send data via email
        String messageId = emailService.sendTableDataEmail(request, data);
        
        log.info("Export and send completed successfully. Total rows: {}, Message ID: {}", 
                 data.getTotalRows(), messageId);
        
        return messageId;
    }
}
