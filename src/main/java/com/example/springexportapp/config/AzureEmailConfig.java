package com.example.springexportapp.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AzureEmailConfig {

    @Value("${azure.communication.email.endpoint}")
    private String endpoint;

    @Value("${azure.communication.email.connection-string:}")
    private String connectionString;

    @Value("${azure.communication.email.sender-address}")
    private String senderAddress;

    /**
     * Configure Azure Communication Services Email Client
     * Uses Managed Identity in production, connection string for local dev
     */
    @Bean
    public EmailClient emailClient() {
        log.info("Configuring Azure Communication Services Email Client");
        
        EmailClientBuilder builder = new EmailClientBuilder();
        
        // Use connection string if provided (local development)
        if (connectionString != null && !connectionString.isEmpty()) {
            log.info("Using connection string for Email Client");
            builder.connectionString(connectionString);
        } else {
            // Use Managed Identity for production (recommended)
            log.info("Using Managed Identity for Email Client with endpoint: {}", endpoint);
            builder.endpoint(endpoint)
                   .credential(new DefaultAzureCredentialBuilder().build());
        }
        
        return builder.buildClient();
    }

    @Bean
    public String senderEmailAddress() {
        return senderAddress;
    }
}
