# Environment Variables Configuration Guide

## Complete List of Environment Variables

### 🔴 Required Variables (Must Configure)

#### 1. AZURE_COMMUNICATION_ENDPOINT

- **Description**: Azure Communication Services endpoint URL
- **How to get**:
  ```powershell
  az communication show --name <acs-name> --resource-group <rg-name> --query "hostName" -o tsv
  ```
- **Format**: `https://your-acs-name.communication.azure.com`
- **Example**: `https://acs-email-service-unique123.communication.azure.com`

#### 2. EMAIL_SENDER_ADDRESS

- **Description**: Verified sender email address from your Azure Communication Services domain
- **How to get**:
  - Azure Portal → Communication Services → Email → Domains
  - Copy the sender address from your verified domain
- **Azure Managed Domain Format**: `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`
- **Custom Domain Format**: `noreply@mail.yourdomain.com`
- **Example**: `DoNotReply@12345678-1234-1234-1234-123456789abc.azurecomm.net`

#### 3. DB_URL

- **Description**: JDBC connection string for your Azure SQL Database
- **⚠️ IMPORTANT**: Use the SAME database as your Function App (the one with the `People` table)
- **SQL Server Format**: `jdbc:sqlserver://<server-name>.database.windows.net:1433;database=<database-name>;encrypt=true;trustServerCertificate=false`
- **How to get**:
  - Azure Portal → SQL databases → Your database → Connection strings → JDBC tab
  - OR from your Function App's `SqlConnectionString` configuration
- **Example**: `jdbc:sqlserver://your-server.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false`
- **Note**: Replace `<server-name>` and `<database-name>` with your actual values

#### 4. DB_USERNAME

- **Description**: Azure SQL Server administrator username
- **⚠️ IMPORTANT**: Use the SAME username as your Function App
- **How to get**: From your Function App's `SqlConnectionString` (look for `User ID=...`)
- **Format**: Any valid username
- **Example**: `sqladmin`
- **Note**: Should match the username in your SQL Server authentication

#### 5. DB_PASSWORD

- **Description**: Azure SQL Server administrator password
- **⚠️ IMPORTANT**: Use the SAME password as your Function App
- **How to get**: From your Function App's `SqlConnectionString` (look for `Password=...`)
- **Requirements**:
  - Minimum 8 characters
  - Contains uppercase, lowercase, numbers
  - May contain special characters
- **Example**: `MySecurePassword123!`
- **⚠️ Security**: Store in Azure Key Vault for production

---

### 🟡 Optional Variables (Default Values Provided)

#### 6. DB_DRIVER

- **Description**: JDBC driver class name
- **Default**: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
- **Value**: `com.microsoft.sqlserver.jdbc.SQLServerDriver` (for Azure SQL / SQL Server)
- **When to set**: Usually not needed (default is SQL Server)

#### 7. DB_CONFIG_PATH

- **Description**: Path to external database configuration file
- **Default**: `C:\Users\malin\OneDrive\Desktop\test-fuction-app`
- **Format**: Absolute path to directory containing `database.properties`
- **Example**: `/mnt/config` (Azure Spring Apps)
- **Note**: App will use environment variables if file not found

#### 8. AZURE_COMMUNICATION_CONNECTION_STRING

- **Description**: Connection string for Azure Communication Services (local development only)
- **Default**: Empty (uses Managed Identity)
- **How to get**:
  ```powershell
  az communication list-key --name <acs-name> --resource-group <rg-name> --query "primaryConnectionString" -o tsv
  ```
- **Format**: `endpoint=https://...;accesskey=...`
- **⚠️ Important**:
  - Use ONLY for local development
  - DO NOT use in production
  - Use Managed Identity in Azure Spring Apps

#### 9. SPRING_PROFILES_ACTIVE

- **Description**: Active Spring profile
- **Default**: None
- **Options**: `dev`, `prod`, `test`
- **Example**: `prod`

---

## How to Set Environment Variables

### Option 1: Azure Spring Apps (Production)

```powershell
az spring app update `
  --name spring-export-app `
  --resource-group <your-rg> `
  --service <your-spring-apps> `
  --env `
    AZURE_COMMUNICATION_ENDPOINT="https://acs-email-service-unique123.communication.azure.com" `
    EMAIL_SENDER_ADDRESS="DoNotReply@12345678-1234-1234-1234-123456789abc.azurecomm.net" `
    DB_URL="jdbc:postgresql://psql-server.postgres.database.azure.com:5432/exportdb" `
    DB_USERNAME="dbadmin" `
    DB_PASSWORD="YourStrongPassword123!" `
    DB_DRIVER="org.postgresql.Driver" `
    SPRING_PROFILES_ACTIVE="prod"
```

### Option 2: Azure Spring Apps via Portal

1. Go to Azure Portal
2. Navigate to your Azure Spring Apps instance
3. Click on your app → **Configuration** → **Environment variables**
4. Add each variable:
   - Name: `AZURE_COMMUNICATION_ENDPOINT`
   - Value: `https://your-acs.communication.azure.com`
5. Click **Save**

### Option 3: Local Development (Windows PowerShell)

```powershell
# Set for current session
$env:AZURE_COMMUNICATION_ENDPOINT = "https://acs-email-service-unique123.communication.azure.com"
$env:EMAIL_SENDER_ADDRESS = "DoNotReply@12345678-1234-1234-1234-123456789abc.azurecomm.net"
$env:DB_URL = "jdbc:postgresql://localhost:5432/mydatabase"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "password"
$env:AZURE_COMMUNICATION_CONNECTION_STRING = "endpoint=https://...;accesskey=..."

# Run application
mvn spring-boot:run
```

### Option 4: Local Development (application.yml)

**⚠️ Warning**: Don't commit secrets to source control!

Create `application-local.yml`:

```yaml
azure:
  communication:
    email:
      endpoint: https://acs-email-service-unique123.communication.azure.com
      sender-address: DoNotReply@12345678-1234-1234-1234-123456789abc.azurecomm.net
      connection-string: endpoint=https://...;accesskey=...

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydatabase
    username: postgres
    password: password
```

Run with profile:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Option 5: Database Config File

Create file: `C:\Users\malin\OneDrive\Desktop\test-fuction-app\database.properties`

```properties
db.url=jdbc:postgresql://psql-server.postgres.database.azure.com:5432/exportdb
db.username=dbadmin
db.password=YourStrongPassword123!
db.driver=org.postgresql.Driver
```

Then set only:

```powershell
$env:DB_CONFIG_PATH = "C:\Users\malin\OneDrive\Desktop\test-fuction-app"
$env:AZURE_COMMUNICATION_ENDPOINT = "https://acs-email-service-unique123.communication.azure.com"
$env:EMAIL_SENDER_ADDRESS = "DoNotReply@xxxxx.azurecomm.net"
```

---

## Quick Fill Template

Copy and fill this template:

```powershell
# === REQUIRED VARIABLES ===

# Azure Communication Services endpoint
$env:AZURE_COMMUNICATION_ENDPOINT = "https://[YOUR-ACS-NAME].communication.azure.com"

# Verified sender email address
$env:EMAIL_SENDER_ADDRESS = "DoNotReply@[YOUR-DOMAIN].azurecomm.net"

# Database connection URL
$env:DB_URL = "jdbc:postgresql://[SERVER-NAME].postgres.database.azure.com:5432/[DB-NAME]"

# Database username
$env:DB_USERNAME = "[YOUR-DB-USERNAME]"

# Database password
$env:DB_PASSWORD = "[YOUR-DB-PASSWORD]"


# === OPTIONAL VARIABLES ===

# Database driver (only if using MySQL)
# $env:DB_DRIVER = "com.mysql.cj.jdbc.Driver"

# Database config file path
# $env:DB_CONFIG_PATH = "C:\Users\malin\OneDrive\Desktop\test-fuction-app"

# Connection string (local dev only - DO NOT USE IN PRODUCTION)
# $env:AZURE_COMMUNICATION_CONNECTION_STRING = "endpoint=https://...;accesskey=..."

# Spring profile
# $env:SPRING_PROFILES_ACTIVE = "prod"
```

---

## Verification Checklist

After setting variables, verify:

- [ ] `AZURE_COMMUNICATION_ENDPOINT` is a valid URL starting with `https://`
- [ ] `EMAIL_SENDER_ADDRESS` matches your verified domain
- [ ] `DB_URL` includes correct server name and database name
- [ ] `DB_USERNAME` and `DB_PASSWORD` are correct
- [ ] Can connect to database from Azure Spring Apps network
- [ ] Managed Identity has permissions on Communication Services
- [ ] Email domain is verified in Azure Portal

---

## Testing Variables

```powershell
# Test database connection
mvn spring-boot:run

# Check logs for:
# "Database configured successfully: jdbc:postgresql://..."
# "Configuring Azure Communication Services Email Client"

# Test health endpoint
curl http://localhost:8080/api/export/health
```

---

## Troubleshooting

### Issue: "Could not open JDBC Connection"

**Solution**: Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and firewall rules

### Issue: "Failed to send email"

**Solution**: Verify `AZURE_COMMUNICATION_ENDPOINT` and `EMAIL_SENDER_ADDRESS`, check domain verification

### Issue: "Authentication failed"

**Solution**:

- Production: Ensure Managed Identity has permissions
- Dev: Check `AZURE_COMMUNICATION_CONNECTION_STRING`

### Issue: "Database config file not found"

**Solution**: Create `database.properties` file at `DB_CONFIG_PATH` location

---

## Security Best Practices

✅ **DO:**

- Use Managed Identity in production
- Store secrets in Azure Key Vault
- Use connection string only for local development
- Rotate passwords regularly
- Use strong passwords (20+ characters)

❌ **DON'T:**

- Commit connection strings to Git
- Share passwords in plain text
- Use same password for multiple environments
- Use connection strings in production
- Hardcode secrets in application.yml

---

## Quick Reference

```powershell
# View current variables (PowerShell)
Get-ChildItem Env: | Where-Object { $_.Name -like "*AZURE*" -or $_.Name -like "*DB_*" }

# View in Azure Spring Apps
az spring app show `
  --name spring-export-app `
  --resource-group <rg> `
  --service <spring-apps> `
  --query "properties.activeDeployment.properties.deploymentSettings.environmentVariables"
```
