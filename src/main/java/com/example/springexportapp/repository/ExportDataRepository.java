package com.example.springexportapp.repository;

import com.example.springexportapp.model.ExportData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSetMetaData;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExportDataRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Retrieves entire table data with column names
     */
    public ExportData getTableData(String tableName) {
        log.info("Fetching data from table: {}", tableName);
        
        // Sanitize table name to prevent SQL injection
        String sanitizedTableName = sanitizeTableName(tableName);
        
        String query = String.format("SELECT * FROM %s", sanitizedTableName);
        
        ExportData exportData = new ExportData();
        exportData.setTableName(tableName);
        
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        
        jdbcTemplate.query(query, rs -> {
            // Get column names from metadata (only once)
            if (columnNames.isEmpty()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }
                exportData.setColumnNames(columnNames);
            }
            
            // Get row data
            Map<String, Object> row = new LinkedHashMap<>();
            for (String columnName : columnNames) {
                row.put(columnName, rs.getObject(columnName));
            }
            rows.add(row);
        });
        
        exportData.setRows(rows);
        exportData.setTotalRows(rows.size());
        
        log.info("Retrieved {} rows from table {}", rows.size(), tableName);
        return exportData;
    }

    /**
     * Get list of all tables in the database
     */
    public List<String> getAllTableNames() {
        String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        return jdbcTemplate.queryForList(query, String.class);
    }

    /**
     * Basic sanitization for table name
     * In production, use a whitelist approach
     */
    private String sanitizeTableName(String tableName) {
        // Remove any characters that are not alphanumeric or underscore
        String sanitized = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        
        if (!sanitized.equals(tableName)) {
            log.warn("Table name was sanitized from '{}' to '{}'", tableName, sanitized);
        }
        
        return sanitized;
    }
}
