package com.example.springexportapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportData {
    
    private String tableName;
    private List<String> columnNames;
    private List<Map<String, Object>> rows;
    private int totalRows;
}
