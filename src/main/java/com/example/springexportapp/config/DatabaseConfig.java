package com.example.springexportapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${database.config.file-path}")
    private String dbConfigPath;

    /**
     * Reads database configuration from external file
     * This allows reading DB config from the function app directory
     */
    @Bean
    public DataSource dataSource() {
        log.info("Loading database configuration from: {}", dbConfigPath);
        
        Properties dbProps = loadDatabaseProperties();
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(dbProps.getProperty("db.url"));
        dataSource.setUsername(dbProps.getProperty("db.username"));
        dataSource.setPassword(dbProps.getProperty("db.password"));
        dataSource.setDriverClassName(dbProps.getProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
        
        log.info("Database configured successfully: {}", dataSource.getUrl());
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Load database properties from external configuration file
     */
    private Properties loadDatabaseProperties() {
        Properties props = new Properties();
        
        try {
            // Try to load from properties file in the function app directory
            Path configPath = Paths.get(dbConfigPath, "database.properties");
            
            if (Files.exists(configPath)) {
                props.load(Files.newInputStream(configPath));
                log.info("Loaded database configuration from file: {}", configPath);
            } else {
                log.warn("Database config file not found at: {}. Using environment variables.", configPath);
                loadFromEnvironment(props);
            }
        } catch (IOException e) {
            log.error("Error reading database config file. Falling back to environment variables.", e);
            loadFromEnvironment(props);
        }
        
        return props;
    }

    /**
     * Fallback to environment variables if config file is not available
     */
    private void loadFromEnvironment(Properties props) {
        props.setProperty("db.url", System.getenv().getOrDefault("DB_URL", "jdbc:sqlserver://localhost:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false"));
        props.setProperty("db.username", System.getenv().getOrDefault("DB_USERNAME", "sqladmin"));
        props.setProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", "password"));
        props.setProperty("db.driver", System.getenv().getOrDefault("DB_DRIVER", "com.microsoft.sqlserver.jdbc.SQLServerDriver"));
    }
}
