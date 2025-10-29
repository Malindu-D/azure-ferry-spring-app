package com.example.springexportapp.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataExportRequest {
    
    @NotBlank(message = "Table name is required")
    private String tableName;
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;
    
    private String subject; // Optional: custom email subject
    
    private List<String> ccEmails; // Optional: CC recipients
    
    private String exportFormat; // Optional: CSV, JSON, etc. Default is HTML table
}
