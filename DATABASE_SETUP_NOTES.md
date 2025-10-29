# Database Setup - Important Notes

## ✅ What Changed

I've updated the Spring application to use **Azure SQL Server** instead of PostgreSQL, matching your existing Function App setup.

---

## Your Existing Setup

Based on your Function App (`test-fuction-app`), you have:

1. **Database Type**: Azure SQL Database (Microsoft SQL Server)
2. **Table**: `People` with columns:
   - `Id` (INT, PRIMARY KEY, IDENTITY)
   - `Name` (NVARCHAR)
   - `Age` (INT)
   - `CreatedDate` (DATETIME)
3. **Function App**: Receives messages from Service Bus and inserts into `People` table
4. **API App**: Receives data from static web app and sends to Service Bus

---

## How This Spring App Fits In

This Spring Export App will:

1. **Connect to the SAME Azure SQL Database** as your Function App
2. **Read data from the `People` table**
3. **Export the entire table** and send it via email using Azure Communication Services

### Architecture Flow:

```
Static Web App → API Service App → Service Bus → Function App → SQL Database (People table)
                                                                         ↑
                                                    Spring Export App ←──┘
                                                            ↓
                                                    Email via Azure Communication Services
```

---

## Changes Made to Spring App

### 1. Updated `pom.xml`

- ✅ Removed PostgreSQL driver
- ✅ Added SQL Server driver (`com.microsoft.sqlserver:mssql-jdbc`)

### 2. Updated `DatabaseConfig.java`

- ✅ Changed default driver to `com.microsoft.sqlserver.jdbc.SQLServerDriver`
- ✅ Changed default connection string format to SQL Server
- ✅ Updated fallback environment variables

### 3. Updated `database.properties.example`

- ✅ Changed from PostgreSQL format to SQL Server format
- ✅ Added instructions to use SAME credentials as Function App

### 4. Updated `AZURE_PORTAL_GUIDE.md`

- ✅ Removed PostgreSQL database creation section
- ✅ Added section explaining to use existing SQL Database
- ✅ Updated environment variable examples with SQL Server format
- ✅ Updated firewall configuration for SQL Server

### 5. Updated `ENV_VARIABLES_GUIDE.md`

- ✅ Changed all database examples to SQL Server format
- ✅ Added notes to use SAME credentials as Function App

---

## Required Configuration

### Option 1: Using Database Properties File (Recommended for Local Testing)

Create a file at: `C:\Users\malin\OneDrive\Desktop\test-fuction-app\database.properties`

```properties
# Use the SAME values as your Function App's SqlConnectionString
db.url=jdbc:sqlserver://your-server.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false
db.username=your-sql-admin-username
db.password=your-sql-admin-password
db.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

**How to get these values:**

1. Open your Function App in Azure Portal
2. Go to **Configuration** → **Application settings**
3. Find `SqlConnectionString`
4. Extract the values:
   - **Server**: Look for `Server=tcp:XXXXX.database.windows.net`
   - **Database**: Look for `Initial Catalog=XXXXX` or `database=XXXXX`
   - **Username**: Look for `User ID=XXXXX`
   - **Password**: Look for `Password=XXXXX`

### Option 2: Using Environment Variables (For Azure Spring Apps)

Set these environment variables in Azure Spring Apps:

```bash
DB_URL=jdbc:sqlserver://your-server.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false
DB_USERNAME=your-sql-admin-username
DB_PASSWORD=your-sql-admin-password
DB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

---

## Testing the Connection

### Local Testing

1. Create the `database.properties` file with your SQL Server credentials
2. Make sure your SQL Server firewall allows your local IP
3. Run the application:
   ```powershell
   mvn spring-boot:run
   ```
4. Test the health endpoint:
   ```powershell
   curl http://localhost:8080/api/export/health
   ```

### Test Export

```powershell
$body = @{
    tableName = "People"
    recipientEmail = "your-email@example.com"
    subject = "People Table Export"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/export/table" -Method POST -Body $body -ContentType "application/json"
```

---

## Next Steps

1. **Get your SQL Server credentials** from your Function App's configuration
2. **Create `database.properties`** file with those credentials
3. **Update your SQL Server firewall** to allow your IP address
4. **Test locally** using the commands above
5. **Deploy to Azure Spring Apps** following the AZURE_PORTAL_GUIDE.md

---

## Important Notes

- ✅ **Same Database**: This app connects to the SAME database as your Function App
- ✅ **Same Table**: It reads from the `People` table that your Function App writes to
- ✅ **No Data Modification**: This app only READS data, it doesn't modify or delete anything
- ✅ **Security**: Make sure to use Azure Key Vault for passwords in production
- ✅ **Firewall**: Ensure Azure SQL Server allows connections from Azure Spring Apps

---

## Troubleshooting

### Connection Failed

Check:

1. SQL Server firewall allows your IP / Azure services
2. Database credentials are correct
3. Database name matches exactly (case-sensitive in connection string)
4. Network connectivity to Azure

### Table Not Found

Check:

1. Table name is exactly `People` (case-sensitive)
2. You're connected to the correct database
3. Your SQL user has SELECT permissions on the table

### No Data Returned

Check:

1. The `People` table has data (run `SELECT * FROM People` in Query Editor)
2. Your Function App is running and processing Service Bus messages
3. Your API Service App is sending messages to Service Bus

---

## Quick Reference

| Component           | Database Details                                                                         |
| ------------------- | ---------------------------------------------------------------------------------------- |
| **Database Type**   | Azure SQL Database (SQL Server)                                                          |
| **Server Port**     | 1433                                                                                     |
| **Database Name**   | PersonDatabase (or your custom name)                                                     |
| **Table Name**      | People                                                                                   |
| **Columns**         | Id, Name, Age, CreatedDate                                                               |
| **Driver Class**    | com.microsoft.sqlserver.jdbc.SQLServerDriver                                             |
| **JDBC URL Format** | jdbc:sqlserver://SERVER:1433;database=DATABASE;encrypt=true;trustServerCertificate=false |

---

**✅ Build Status**: Application successfully compiled with SQL Server driver
**✅ JAR File**: `target/spring-export-app-1.0.0.jar` (ready for deployment)
