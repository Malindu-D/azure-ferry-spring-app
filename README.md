# Spring Export Email Application

A Spring Boot application that exports database table data and sends it via email using Azure Communication Services.

## Deployment Options

- **Azure Container Apps** (Recommended) - See `AZURE_PORTAL_GUIDE.md`
- **Local Development** - See below

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Azure subscription
- Azure SQL Database (same as your Function App)
- Azure Communication Services with verified email domain
- Azure CLI (for deployment)

### Build JAR

```powershell
# Build the application
mvn clean package -DskipTests

# The JAR will be created at: target/spring-export-app-1.0.0.jar
```

### Run Locally

```powershell
# Set environment variables (Windows PowerShell)
$env:DB_URL = "jdbc:sqlserver://your-server.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false"
$env:DB_USERNAME = "sqladmin"
$env:DB_PASSWORD = "YourPassword123!"
$env:DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
$env:AZURE_COMMUNICATION_ENDPOINT = "https://your-acs.communication.azure.com"
$env:EMAIL_SENDER_ADDRESS = "DoNotReply@xxxxx.azurecomm.net"

# Run the application
java -jar target/spring-export-app-1.0.0.jar

# Or use Maven
mvn spring-boot:run
```

### Deploy to Azure Container Apps

The simplest way to deploy is using Azure CLI, which automatically handles containerization:

```powershell
# Login to Azure
az login

# Deploy to Container Apps (creates container automatically from JAR)
az containerapp up `
  --name spring-export-app `
  --resource-group rg-spring-export-app `
  --location eastus `
  --environment cae-spring-export `
  --artifact target/spring-export-app-1.0.0.jar `
  --ingress external `
  --target-port 8080
```

For detailed step-by-step instructions using Azure Portal, see `AZURE_PORTAL_GUIDE.md`.

### Test the API

**Using PowerShell:**

```powershell
# Health check
Invoke-RestMethod -Uri "http://localhost:8080/api/export/health"

# Export table data (People table)
$body = @{
    tableName = "People"
    recipientEmail = "user@example.com"
    subject = "People Data Export"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/export/table" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

**Using curl:**

```bash
# Health check
curl http://localhost:8080/api/export/health

# Export table data
curl -X POST http://localhost:8080/api/export/table \
  -H "Content-Type: application/json" \
  -d '{"tableName": "People", "recipientEmail": "user@example.com", "subject": "Data Export"}'
```

## How It Works

1. **Receives request** from API service with table name and recipient email
2. **Connects to database** using configuration from external file or environment variables
3. **Queries entire table**: `SELECT * FROM table_name`
4. **Formats data** as HTML table
5. **Sends email** via Azure Communication Services

## API Endpoints

### POST /api/export/table

Export table data and send via email

**Request Body:**

```json
{
  "tableName": "People",
  "recipientEmail": "user@example.com",
  "subject": "People Data Export",
  "ccEmails": ["manager@example.com"]
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Data exported and email sent successfully",
  "messageId": "xxxxx-xxxxx-xxxxx",
  "tableName": "People"
}
```

### GET /api/export/health

Health check endpoint

**Response:**

```json
{
  "status": "UP",
  "service": "Spring Export Email Application"
}
```

## Configuration

### Required Environment Variables

| Variable                       | Description                           | Example                                                             |
| ------------------------------ | ------------------------------------- | ------------------------------------------------------------------- |
| `AZURE_COMMUNICATION_ENDPOINT` | Azure Communication Services endpoint | `https://your-acs.communication.azure.com`                          |
| `EMAIL_SENDER_ADDRESS`         | Verified sender email                 | `DoNotReply@xxxxx.azurecomm.net`                                    |
| `DB_URL`                       | Database JDBC URL                     | `jdbc:sqlserver://server:1433;database=PersonDatabase;encrypt=true` |
| `DB_USERNAME`                  | Database username                     | `sqladmin`                                                          |
| `DB_PASSWORD`                  | Database password                     | `YourPassword123!`                                                  |
| `DB_DRIVER`                    | JDBC driver class                     | `com.microsoft.sqlserver.jdbc.SQLServerDriver`                      |

### Optional Environment Variables

| Variable                                | Description                  | Default                                            |
| --------------------------------------- | ---------------------------- | -------------------------------------------------- |
| `DB_CONFIG_PATH`                        | Path to database config file | `C:\Users\malin\OneDrive\Desktop\test-fuction-app` |
| `AZURE_COMMUNICATION_CONNECTION_STRING` | Connection string (dev only) | -                                                  |

## Deployment

See **[AZURE_PORTAL_GUIDE.md](AZURE_PORTAL_GUIDE.md)** for complete step-by-step deployment instructions to Azure Container Apps using the Azure Portal (no CLI required).

For Azure CLI-based deployment, use the `az containerapp up` command shown in the Quick Start section above.

## Project Structure

```
src/
├── main/
│   ├── java/com/example/springexportapp/
│   │   ├── SpringExportEmailApplication.java    # Main application
│   │   ├── controller/
│   │   │   └── ExportController.java            # REST API endpoints
│   │   ├── service/
│   │   │   ├── DataExportService.java           # Business logic
│   │   │   └── EmailService.java                # Email sending service
│   │   ├── repository/
│   │   │   └── ExportDataRepository.java        # Database operations
│   │   ├── model/
│   │   │   ├── DataExportRequest.java           # Request DTO
│   │   │   └── ExportData.java                  # Data DTO
│   │   └── config/
│   │       ├── DatabaseConfig.java              # Database configuration
│   │       └── AzureEmailConfig.java            # Azure email configuration
│   └── resources/
│       └── application.yml                       # Application properties
```

## Technologies

- **Java 21** - Latest LTS version
- **Spring Boot 3.4.0** - Web framework
- **Spring Data JPA** - Database access
- **Azure Communication Services 1.0.15** - Email delivery
- **SQL Server JDBC Driver** - Database connectivity
- **Azure Identity 1.13.0** - Managed Identity authentication
- **Lombok** - Code generation
- **Maven** - Build tool

## License

MIT License
