# Complete Deployment Guide for Spring Export Email Application

## Table of Contents

1. [Application Overview](#application-overview)
2. [Architecture Flow](#architecture-flow)
3. [Prerequisites](#prerequisites)
4. [Azure Resources Setup](#azure-resources-setup)
5. [Configuration Guide](#configuration-guide)
6. [Deploy to Azure Spring Apps](#deploy-to-azure-spring-apps)
7. [Testing the Application](#testing-the-application)
8. [Troubleshooting](#troubleshooting)

---

## 1. Application Overview

This Spring Boot application:

- **Receives requests** from your API service app (test-api-service-app)
- **Reads database configuration** from the function app directory (test-function-app)
- **Retrieves entire table data** from your database
- **Formats data as HTML table** in email
- **Sends email** via Azure Communication Services

### Key Features

✅ Reads DB config from external file or environment variables  
✅ Supports PostgreSQL and MySQL databases  
✅ Exports entire tables with all columns and rows  
✅ Sends formatted HTML emails  
✅ Uses Azure Managed Identity for secure authentication  
✅ Java 21 with Spring Boot 3.4.0

---

## 2. Architecture Flow

```
API Service App (test-api-service-app)
         ↓
    HTTP POST Request
         ↓
Spring Export App (this app)
         ↓
    1. Read DB config from test-function-app/database.properties
    2. Connect to Database
    3. Query entire table: SELECT * FROM table_name
    4. Format data as HTML table
    5. Send email via Azure Communication Services
         ↓
    Email delivered to recipient
```

---

## 3. Prerequisites

### Local Development

- ✅ **Java 21** installed (JDK 21)
- ✅ **Maven 3.9+** installed
- ✅ **Azure CLI** installed: https://docs.microsoft.com/cli/azure/install-azure-cli
- ✅ **Azure subscription** with appropriate permissions

### Azure Resources Needed

- ✅ Azure Database (PostgreSQL or MySQL)
- ✅ Azure Communication Services (Email)
- ✅ Azure Spring Apps instance
- ✅ Email Domain verified in Azure Communication Services

---

## 4. Azure Resources Setup

### Step 1: Create Azure Communication Services

```powershell
# Login to Azure
az login

# Set variables
$RESOURCE_GROUP = "rg-spring-export-app"
$LOCATION = "eastus"
$ACS_NAME = "acs-email-service-unique123"

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Azure Communication Services
az communication create `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --location "global" `
  --data-location "United States"

# Get the endpoint
az communication show `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --query "hostName" -o tsv
```

**Output:** `acs-email-service-unique123.communication.azure.com`

### Step 2: Set Up Email Domain

**Option A: Azure Managed Domain (Quick Start - Free)**

```powershell
# Create Email Communication Service
$EMAIL_SERVICE_NAME = "email-service-unique123"

az communication email create `
  --name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --location "global" `
  --data-location "United States"

# Create Azure Managed Domain
az communication email domain create `
  --name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --domain-management "AzureManaged"

# Link domain to Communication Service
az communication email domain link `
  --name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --communication-resource-id $(az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP --query "id" -o tsv)
```

**Your sender address will be:** `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`

**Option B: Custom Domain (Production)**

1. Go to Azure Portal → Communication Services → Email → Domains
2. Add custom domain (e.g., mail.yourdomain.com)
3. Add DNS TXT records to verify ownership
4. Wait for verification
5. Link domain to Communication Service

### Step 3: Create Azure Database (PostgreSQL Example)

```powershell
$DB_SERVER_NAME = "psql-spring-export-unique123"
$DB_NAME = "exportdb"
$ADMIN_USER = "dbadmin"
$ADMIN_PASSWORD = "YourStrongPassword123!"

# Create PostgreSQL Flexible Server
az postgres flexible-server create `
  --name $DB_SERVER_NAME `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION `
  --admin-user $ADMIN_USER `
  --admin-password $ADMIN_PASSWORD `
  --sku-name Standard_B1ms `
  --tier Burstable `
  --version 14 `
  --storage-size 32 `
  --public-access 0.0.0.0-255.255.255.255

# Create database
az postgres flexible-server db create `
  --server-name $DB_SERVER_NAME `
  --resource-group $RESOURCE_GROUP `
  --database-name $DB_NAME
```

**Connection String:**

```
jdbc:postgresql://${DB_SERVER_NAME}.postgres.database.azure.com:5432/${DB_NAME}
```

### Step 4: Create Azure Spring Apps

```powershell
$SPRING_APPS_NAME = "spring-apps-export-unique123"
$APP_NAME = "spring-export-app"

# Create Azure Spring Apps instance (Standard tier for production)
az spring create `
  --name $SPRING_APPS_NAME `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION `
  --sku Standard

# Create app in Azure Spring Apps
az spring app create `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --instance-count 1 `
  --memory 2Gi `
  --assign-endpoint true `
  --runtime-version Java_21
```

### Step 5: Enable Managed Identity

```powershell
# Enable system-assigned managed identity
az spring app identity assign `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME

# Get the principal ID
$PRINCIPAL_ID = az spring app identity show `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --query "principalId" -o tsv

# Grant Contributor role to Communication Services
az role assignment create `
  --assignee $PRINCIPAL_ID `
  --role "Contributor" `
  --scope $(az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP --query "id" -o tsv)
```

---

## 5. Configuration Guide

### 5.1 Database Configuration File

**Create this file:** `C:\Users\malin\OneDrive\Desktop\test-fuction-app\database.properties`

```properties
# PostgreSQL Configuration
db.url=jdbc:postgresql://psql-spring-export-unique123.postgres.database.azure.com:5432/exportdb
db.username=dbadmin
db.password=YourStrongPassword123!
db.driver=org.postgresql.Driver
```

### 5.2 Environment Variables for Azure Spring Apps

```powershell
# Set environment variables
az spring app update `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --env `
    AZURE_COMMUNICATION_ENDPOINT="https://acs-email-service-unique123.communication.azure.com" `
    EMAIL_SENDER_ADDRESS="DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net" `
    DB_CONFIG_PATH="/mnt/config" `
    DB_URL="jdbc:postgresql://psql-spring-export-unique123.postgres.database.azure.com:5432/exportdb" `
    DB_USERNAME="dbadmin" `
    DB_PASSWORD="YourStrongPassword123!" `
    DB_DRIVER="org.postgresql.Driver" `
    SPRING_PROFILES_ACTIVE="prod"
```

### 5.3 Complete Environment Variables Reference

| Variable Name                           | Description                           | Example Value                                                  | Required                       |
| --------------------------------------- | ------------------------------------- | -------------------------------------------------------------- | ------------------------------ |
| `AZURE_COMMUNICATION_ENDPOINT`          | Azure Communication Services endpoint | `https://your-acs.communication.azure.com`                     | ✅ Yes                         |
| `EMAIL_SENDER_ADDRESS`                  | Verified sender email address         | `DoNotReply@xxxxx.azurecomm.net`                               | ✅ Yes                         |
| `DB_URL`                                | Database JDBC URL                     | `jdbc:postgresql://server.postgres.database.azure.com:5432/db` | ✅ Yes                         |
| `DB_USERNAME`                           | Database username                     | `dbadmin`                                                      | ✅ Yes                         |
| `DB_PASSWORD`                           | Database password                     | `YourPassword123!`                                             | ✅ Yes                         |
| `DB_DRIVER`                             | JDBC driver class                     | `org.postgresql.Driver`                                        | ❌ No (defaults to PostgreSQL) |
| `DB_CONFIG_PATH`                        | Path to database config file          | `C:\Users\malin\OneDrive\Desktop\test-fuction-app`             | ❌ No                          |
| `AZURE_COMMUNICATION_CONNECTION_STRING` | Connection string (dev only)          | `endpoint=https://...;accesskey=...`                           | ❌ No (use Managed Identity)   |

---

## 6. Deploy to Azure Spring Apps

### Method 1: Deploy JAR file

```powershell
# Build the application
cd "C:\Users\malin\OneDrive\Desktop\test-spring-export-app"
mvn clean package -DskipTests

# Deploy to Azure Spring Apps
az spring app deploy `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --artifact-path target/spring-export-app-1.0.0.jar `
  --runtime-version Java_21

# Get the app URL
az spring app show `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --query "properties.url" -o tsv
```

### Method 2: Deploy from Source Code

```powershell
az spring app deploy `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --source-path . `
  --build-env BP_JVM_VERSION=21
```

### View Logs

```powershell
# Stream logs
az spring app logs `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --follow

# View recent logs
az spring app logs `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --lines 100
```

---

## 7. Testing the Application

### 7.1 Health Check

```powershell
# Get app URL
$APP_URL = az spring app show `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME `
  --query "properties.url" -o tsv

# Test health endpoint
curl "https://$APP_URL/api/export/health"
```

**Expected Response:**

```json
{
  "status": "UP",
  "service": "Spring Export Email Application"
}
```

### 7.2 Test Export Request (from API Service App)

**Example Request:**

```powershell
# Create test request
$body = @{
    tableName = "employees"
    recipientEmail = "recipient@example.com"
    subject = "Employee Data Export"
    ccEmails = @("manager@example.com")
} | ConvertTo-Json

# Send request
Invoke-RestMethod `
  -Uri "https://$APP_URL/api/export/table" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

**Expected Response:**

```json
{
  "status": "success",
  "message": "Data exported and email sent successfully",
  "messageId": "xxxxx-xxxxx-xxxxx",
  "tableName": "employees"
}
```

### 7.3 Sample cURL Request

```bash
curl -X POST https://your-app-url.azuremicroservices.io/api/export/table \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "employees",
    "recipientEmail": "user@example.com",
    "subject": "Data Export Report",
    "ccEmails": ["manager@example.com"]
  }'
```

---

## 8. Troubleshooting

### Common Issues

#### Issue 1: Database Connection Failed

**Error:** `Could not open JDBC Connection`

**Solutions:**

1. Check firewall rules in Azure Database

```powershell
az postgres flexible-server firewall-rule create `
  --resource-group $RESOURCE_GROUP `
  --name $DB_SERVER_NAME `
  --rule-name AllowAzureServices `
  --start-ip-address 0.0.0.0 `
  --end-ip-address 0.0.0.0
```

2. Verify connection string and credentials
3. Check if database exists

#### Issue 2: Email Not Sending

**Error:** `Failed to send email`

**Solutions:**

1. Verify Managed Identity has permissions
2. Check email domain is verified
3. Verify sender address format
4. Check Azure Communication Services logs

```powershell
# Check ACS diagnostics
az monitor diagnostic-settings create `
  --name acs-diagnostics `
  --resource $(az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP --query "id" -o tsv) `
  --logs '[{"category":"EmailSendMailLogs","enabled":true}]' `
  --workspace $(az monitor log-analytics workspace show --resource-group $RESOURCE_GROUP --workspace-name your-workspace --query "id" -o tsv)
```

#### Issue 3: Table Not Found

**Error:** `Table is empty or does not exist`

**Solutions:**

1. Verify table name (case-sensitive)
2. Check database schema
3. Ensure table is in `public` schema

```sql
-- List all tables
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
```

#### Issue 4: App Not Starting

**Solutions:**

```powershell
# Check app status
az spring app show `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME

# Restart app
az spring app restart `
  --name $APP_NAME `
  --resource-group $RESOURCE_GROUP `
  --service $SPRING_APPS_NAME
```

---

## 9. Integration with API Service App

Your **test-api-service-app** should call this Spring app:

```java
// Example API Service code to forward request
@PostMapping("/forward-export")
public ResponseEntity<?> forwardExportRequest(@RequestBody ExportRequest request) {
    RestTemplate restTemplate = new RestTemplate();

    String springAppUrl = "https://your-spring-app.azuremicroservices.io/api/export/table";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<ExportRequest> entity = new HttpEntity<>(request, headers);

    return restTemplate.postForEntity(springAppUrl, entity, Map.class);
}
```

---

## 10. Production Checklist

- [ ] Use **Managed Identity** (not connection strings)
- [ ] Enable **HTTPS only**
- [ ] Configure **firewall rules** on database
- [ ] Set up **Application Insights** for monitoring
- [ ] Configure **rate limiting** on API endpoints
- [ ] Use **custom domain** for email (not Azure Managed Domain)
- [ ] Enable **auto-scaling** in Azure Spring Apps
- [ ] Set up **alerts** for failures
- [ ] Implement **retry logic** for email sending
- [ ] Add **input validation** and sanitization
- [ ] Configure **CORS** if needed
- [ ] Set up **CI/CD pipeline** for deployments

---

## Quick Reference Commands

```powershell
# Build
mvn clean package

# Deploy
az spring app deploy --name $APP_NAME --resource-group $RESOURCE_GROUP --service $SPRING_APPS_NAME --artifact-path target/spring-export-app-1.0.0.jar

# View logs
az spring app logs --name $APP_NAME --resource-group $RESOURCE_GROUP --service $SPRING_APPS_NAME --follow

# Restart
az spring app restart --name $APP_NAME --resource-group $RESOURCE_GROUP --service $SPRING_APPS_NAME

# Scale
az spring app scale --name $APP_NAME --resource-group $RESOURCE_GROUP --service $SPRING_APPS_NAME --instance-count 2

# Delete
az spring delete --name $SPRING_APPS_NAME --resource-group $RESOURCE_GROUP
```

---

## Support

For issues or questions:

- Check Azure Spring Apps documentation
- Review Azure Communication Services docs
- Check application logs in Azure Portal
- Enable debug logging: `logging.level.com.example=DEBUG`

---

**Application Created Successfully! ✅**
