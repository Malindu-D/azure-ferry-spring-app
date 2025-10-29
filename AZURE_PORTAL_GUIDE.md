# Complete Azure Portal Setup Guide

**Step-by-step guide using primarily the Azure Portal Console UI**

**Note:** While most steps use the Portal UI, deploying your JAR file requires Azure CLI. See Section 4 for both CLI and Portal deployment options.

---

## Table of Contents

1. [Azure Communication Services Setup](#1-azure-communication-services-setup)
2. [Email Service Setup](#2-email-service-setup)
3. [Azure Database Setup](#3-azure-database-setup)
4. [Azure Container Apps Setup](#4-azure-container-apps-setup)
5. [Configure Environment Variables](#5-configure-environment-variables)
6. [Enable Managed Identity](#6-enable-managed-identity)
7. [Testing](#7-testing)
8. [Monitoring and Troubleshooting](#8-monitoring-and-troubleshooting)
9. [Production Checklist](#9-production-checklist)

---

## 1. Azure Communication Services Setup

### Step 1.1: Create Communication Services Resource

1. **Open Azure Portal**

   - Go to: https://portal.azure.com
   - Sign in with your Azure account

2. **Create Resource Group** (if you don't have one)

   - Click **"Create a resource"** in the left menu
   - Search for **"Resource group"**
   - Click **"Create"**
   - Fill in:
     - **Subscription**: Select your subscription
     - **Resource group name**: `rg-spring-export-app`
     - **Region**: `East US` (or your preferred region)
   - Click **"Review + create"** → **"Create"**

3. **Create Communication Services**

   - Click **"Create a resource"**
   - Search for **"Communication Services"**
   - Click **"Create"**
   - Fill in the form:
     - **Subscription**: Select your subscription
     - **Resource group**: `rg-spring-export-app`
     - **Resource name**: `acs-email-service-yourname123` (must be globally unique)
     - **Data location**: `United States`
   - Click **"Review + create"**
   - Click **"Create"**
   - Wait for deployment (1-2 minutes)
   - Click **"Go to resource"**

4. **Get the Endpoint**
   - In the Communication Services resource page
   - Look at **"Overview"** section
   - Copy the **"Endpoint"** value
   - Example: `https://acs-email-service-yourname123.communication.azure.com`
   - **📝 Save this** - You'll need it later as `AZURE_COMMUNICATION_ENDPOINT`

---

## 2. Email Service Setup

### Step 2.1: Create Email Communication Service

1. **Create Email Service**
   - In Azure Portal, click **"Create a resource"**
   - Search for **"Email Communication Services"**
   - Click **"Create"**
   - Fill in:
     - **Subscription**: Your subscription
     - **Resource group**: `rg-spring-export-app` (same as before)
     - **Name**: `email-service-yourname123`
     - **Data location**: `United States`
   - Click **"Review + create"** → **"Create"**
   - Wait for deployment
   - Click **"Go to resource"**

### Step 2.2: Add Email Domain

**Option A: Azure Managed Domain (Quick - Recommended for Testing)**

1. **Provision Domain**

   - In your Email Communication Service
   - Click **"Provision domains"** in left menu
   - Click **"+ Add domain"** button
   - Select **"Azure Managed Domain"**
   - Click **"Add"**
   - Wait 2-5 minutes for provisioning

2. **Get Sender Address**
   - Click on **"Provision domains"** in left menu
   - You'll see your domain listed (e.g., `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`)
   - Click on the domain name
   - In the **"Overview"** tab, you'll see:
     - **MailFrom address**: `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`
   - **📝 Save this** - You'll need it as `EMAIL_SENDER_ADDRESS`

**Option B: Custom Domain (Production - More Complex)**

1. **Add Custom Domain**

   - In your Email Communication Service
   - Click **"Provision domains"** in left menu
   - Click **"+ Add domain"**
   - Select **"Custom domain"**
   - Enter your subdomain: `mail.yourdomain.com`
   - Click **"Add"**

2. **Verify Domain Ownership**

   - Click on your domain name in the list
   - Go to **"Verify domain"** tab
   - You'll see DNS records to add:

   **TXT Record for Verification:**

   ```
   Type: TXT
   Host: @ (or leave blank)
   Value: ms-domain-verification=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   TTL: 3600
   ```

3. **Add DNS Records to Your Domain Provider**

   - Log in to your domain provider (GoDaddy, Namecheap, etc.)
   - Go to DNS Management
   - Add the TXT record shown in Azure Portal
   - Save changes

4. **Verify in Azure**

   - Wait 15-30 minutes for DNS propagation
   - Go back to Azure Portal → Your domain
   - Click **"Verify"** button
   - Wait for green checkmark

5. **Configure Email Authentication**

   - After verification, go to **"Configure SPF & DKIM"** tab
   - You'll see DNS records to add:

   **SPF Record (TXT):**

   ```
   Type: TXT
   Host: @
   Value: v=spf1 include:spf.protection.outlook.com -all
   TTL: 3600
   ```

   **DKIM Records (2 CNAME records):**

   ```
   Record 1:
   Type: CNAME
   Host: selector1-azurecomm-prod-net._domainkey
   Value: selector1-azurecomm-prod-net._domainkey.azurecomm.net
   TTL: 3600

   Record 2:
   Type: CNAME
   Host: selector2-azurecomm-prod-net._domainkey
   Value: selector2-azurecomm-prod-net._domainkey.azurecomm.net
   TTL: 3600
   ```

   **DMARC Record (TXT):**

   ```
   Type: TXT
   Host: _dmarc
   Value: v=DMARC1; p=none; pct=100; rua=mailto:dmarc@yourdomain.com
   TTL: 3600
   ```

6. **Add DNS Records**

   - Go to your domain provider's DNS management
   - Add all the records above
   - Save changes
   - Wait 15-30 minutes

7. **Verify Email Authentication**
   - Go back to Azure Portal
   - Click **"Verify"** for each record
   - Wait for all green checkmarks

### Step 2.3: Connect Domain to Communication Services

1. **Link Domain**

   - Go to your **Communication Services** resource (not Email Service)
   - Click **"Domains"** in left menu under **"Email"** section
   - Click **"Connect domain"**
   - Select:
     - **Email service**: `email-service-yourname123`
     - **Domain**: Your provisioned domain
   - Click **"Connect"**

2. **Verify Connection**
   - Your domain should now appear in the list
   - Status should be **"Connected"**

---

## 3. Use Your Existing Azure SQL Database

**✅ You already have an Azure SQL Database from your Function App!**

This Spring application will connect to the **SAME database** that your Function App uses. You don't need to create a new database.

### Step 3.1: Locate Your Existing SQL Database

1. **Find Your SQL Server**

   - In Azure Portal, search for **"SQL databases"**
   - You should see your existing database (e.g., `PersonDatabase`)
   - Click on it
   - Note the **Server name** (e.g., `your-server.database.windows.net`)

2. **Get Connection Details**

   - In your database page, click **"Connection strings"** in left menu
   - Select **"JDBC"** tab
   - Copy the connection string - it looks like:

   ```
   jdbc:sqlserver://YOUR_SERVER.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false;loginTimeout=30;
   ```

   - **📝 Save this** - You'll need it for environment variables

3. **Verify the People Table Exists**
   - In your database, click **"Query editor"** in left menu
   - Sign in with your SQL Server credentials
   - Run this query to verify the table:
   ```sql
   SELECT TOP 10 * FROM People;
   ```
   - You should see columns: `Id`, `Name`, `Age`, `CreatedDate`

### Step 3.2: Allow Azure Services to Access SQL Server

1. **Configure Firewall**

   - Go to your **SQL Server** (not the database)
   - Click **"Networking"** in left menu under **"Security"**
   - Under **"Exceptions"** section:
     - ✅ Check **"Allow Azure services and resources to access this server"**
   - Click **"Save"**

2. **Add Your IP (for local testing)**
   - Still in **"Networking"** page
   - Under **"Firewall rules"** section
   - Click **"+ Add your client IPv4 address"**
   - Enter a rule name: `MyLocalIP`
   - Click **"Save"**

---

## 4. Azure Container Apps Setup

### Step 4.1: Create Container Apps Environment

1. **Create Container Apps Environment**

   - Click **"Create a resource"**
   - Search for **"Container Apps Environment"**
   - Click **"Create"**

2. **Fill in Basics**

   - **Subscription**: Your subscription
   - **Resource group**: `rg-spring-export-app`
   - **Environment name**: `cae-spring-export`
   - **Region**: Same region as other resources
   - **Zone redundancy**: **Disabled** (for development)
   - Click **"Next: Monitoring"**

3. **Monitoring** (Optional)
   - **Enable Log Analytics**: Yes (recommended)
   - Create new Log Analytics workspace or use existing
   - Click **"Review + create"**
   - Click **"Create"**
   - Wait 2-3 minutes for deployment
   - Click **"Go to resource"**

### Step 4.2: Deploy Application with Azure CLI

**Option A: Direct JAR Deployment** (Requires Azure Container Registry access)

If you have ACR or cloud build access, use this simple command:

```powershell
az containerapp up `
  --name spring-export-app `
  --resource-group rg-spring-export-app `
  --location eastus `
  --environment cae-spring-export `
  --artifact target/spring-export-app-1.0.0.jar `
  --ingress external `
  --target-port 8080
```

**Option B: Container Image Deployment** (Recommended - More Reliable)

If Option A fails with registry errors, use this approach:

1. **Install Docker Desktop** (if not already installed)
   
   Download from: https://www.docker.com/products/docker-desktop/

2. **Create Temporary Dockerfile**

   Create a file named `Dockerfile` in your project root:

   ```dockerfile
   FROM eclipse-temurin:21-jre-alpine
   WORKDIR /app
   COPY target/spring-export-app-1.0.0.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

   Or use PowerShell to create it:
   ```powershell
   cd "C:\Users\malin\OneDrive\Desktop\test-spring-export-app"
   @"
   FROM eclipse-temurin:21-jre-alpine
   WORKDIR /app
   COPY target/spring-export-app-1.0.0.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "app.jar"]
   "@ | Out-File -FilePath Dockerfile -Encoding ASCII
   ```

3. **Build Your JAR** (if not already built)

   ```powershell
   mvn clean package -DskipTests
   ```

4. **Deploy with Container Image**

   ```powershell
   az containerapp up `
     --name spring-export-app `
     --resource-group rg-spring-export-app `
     --location eastus `
     --environment cae-spring-export `
     --source . `
     --ingress external `
     --target-port 8080
   ```

   This command will:
   - Build a Docker image from your Dockerfile
   - Create Azure Container Registry if needed (Basic tier)
   - Push the image
   - Deploy to Container Apps

5. **Get Application URL**

   After deployment completes (3-5 minutes), the command will output your application URL:

   ```
   https://spring-export-app.nicegrass-12345678.eastus.azurecontainerapps.io
   ```

   **📝 Save this URL** - This is your application endpoint

   You can also retrieve it later:

   **Via Azure CLI:**

   ```powershell
   az containerapp show `
     --name spring-export-app `
     --resource-group rg-spring-export-app `
     --query properties.configuration.ingress.fqdn `
     --output tsv
   ```

   **Via Azure Portal:**

   - Navigate to your Container App **"spring-export-app"**
   - Click **"Overview"**
   - Copy the **Application Url** from the overview page

### Step 4.3: Alternative - Create Container App via Portal UI (No CLI)

If you prefer using the Azure Portal instead of Azure CLI:

1. **Create Container App**

   - Click **"Create a resource"**
   - Search for **"Container App"**
   - Click **"Create"**

2. **Fill in Basics**

   - **Subscription**: Your subscription
   - **Resource group**: `rg-spring-export-app`
   - **Container app name**: `spring-export-app`
   - **Region**: Same region as other resources
   - **Container Apps Environment**: Select `cae-spring-export` (the one you created in Step 4.1)
   - Click **"Next: Container"**

3. **Container Configuration**

   - **Use quickstart image**: ✅ **Check** this box (for initial deployment)
   - **Name**: `spring-export-app`
   - **Image source**: **"Quick start image"**
   - **Quickstart image**: Select **"Simple hello world container"**
   - Click **"Next: Bindings"** → **"Next: Ingress"**

4. **Ingress Settings** ⚠️ **IMPORTANT**

   - **Ingress**: ✅ **Enabled**
   - **Ingress traffic**: **"Accepting traffic from anywhere"**
   - **Ingress type**: **"HTTP"**
   - **Target port**: **`8080`** (your Spring Boot application port)
   - **Transport**: **Auto**
   - Click **"Next: Tags"** → **"Review + create"**
   - Click **"Create"**
   - Wait 2-3 minutes for deployment
   - Click **"Go to resource"**

5. **Get Application URL**

   - In your Container App **"spring-export-app"**
   - Click **"Overview"**
   - Copy the **Application Url** (e.g., `https://spring-export-app.nicegrass-12345678.eastus.azurecontainerapps.io`)
   - **📝 Save this URL** - This is your application endpoint

6. **Update Container with Your JAR** (Requires Azure CLI)

   Since the Portal method uses a quickstart image initially, you'll need Azure CLI for this one step to update it with your JAR:

   ```powershell
   # Install Azure CLI if needed
   winget install Microsoft.AzureCLI

   # Login and update
   az login
   az containerapp update `
     --name spring-export-app `
     --resource-group rg-spring-export-app `
     --artifact target/spring-export-app-1.0.0.jar
   ```

   This will replace the hello-world container with your Spring Boot application.

   **Alternative - Pure Portal UI (Advanced):** You can manually create a container image using Azure Container Instances or Azure App Service Build Service, but this is more complex. The CLI method above is recommended.

**Note:** The Azure CLI method (Step 4.2) is simpler as it deploys your JAR directly in one command. The Portal method (Step 4.3) requires this extra Azure CLI update step for JAR deployment.

---

## 5. Configure Environment Variables

Now that your app is deployed, you need to configure it with the Azure resource credentials.

### Step 5.1: Set Environment Variables in Container App

1. **Go to Container App Configuration**

   - In Azure Portal, navigate to your Container App **"spring-export-app"**
   - Click **"Environment variables"** in left menu under **"Settings"**

2. **Add Environment Variables**

   Click **"+ Add"** for each variable:

   **Variable 1: AZURE_COMMUNICATION_ENDPOINT**

   - **Name**: `AZURE_COMMUNICATION_ENDPOINT`
   - **Source**: **"Manual entry"**
   - **Value**: `https://acs-email-service-yourname123.communication.azure.com`
   - Click **"Add"**

   **Variable 2: EMAIL_SENDER_ADDRESS**

   - **Name**: `EMAIL_SENDER_ADDRESS`
   - **Source**: **"Manual entry"**
   - **Value**: `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`
   - Click **"Add"**

   **Variable 3: DB_URL**

   - **Name**: `DB_URL`
   - **Source**: **"Manual entry"**
   - **Value**: `jdbc:sqlserver://your-server.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false`
     - Replace `your-server` with your actual SQL Server name
   - Click **"Add"**

   **Variable 4: DB_USERNAME**

   - **Name**: `DB_USERNAME`
   - **Source**: **"Manual entry"**
   - **Value**: Your SQL Server admin username (e.g., `sqladmin`)
   - Click **"Add"**

   **Variable 5: DB_PASSWORD**

   - **Name**: `DB_PASSWORD`
   - **Source**: **"Manual entry"**
   - **Value**: Your SQL Server admin password
   - Click **"Add"**

   **Variable 6: DB_DRIVER**

   - **Name**: `DB_DRIVER`
   - **Source**: **"Manual entry"**
   - **Value**: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
   - Click **"Add"**

   **Variable 7: SPRING_PROFILES_ACTIVE** (Optional)

   - **Name**: `SPRING_PROFILES_ACTIVE`
   - **Source**: **"Manual entry"**
   - **Value**: `prod`
   - Click **"Add"**

3. **Apply Changes**
   - Click **"Apply"** at the bottom
   - The Container App will automatically restart with new environment variables
   - Wait 2-3 minutes for the app to restart
   - Verify the revision status shows **"Running"** in the **"Revisions"** section

---

## 6. Enable Managed Identity

### Step 6.1: Enable System-Assigned Managed Identity

1. **Enable Identity**
   - In your Container App **"spring-export-app"**
   - Click **"Identity"** in left menu under **"Settings"**
   - Go to **"System assigned"** tab
   - Set **Status** to **"On"**
   - Click **"Save"**
   - Click **"Yes"** to confirm
   - Wait for it to be enabled
   - **📝 Copy the Object (principal) ID** shown

### Step 6.2: Grant Permissions to Communication Services

1. **Go to Communication Services**

   - Navigate to your **Communication Services** resource
   - Click **"Access control (IAM)"** in left menu

2. **Add Role Assignment**

   - Click **"+ Add"** → **"Add role assignment"**
   - Go to **"Role"** tab:
     - Search for: **"Contributor"**
     - Select **"Contributor"**
     - Click **"Next"**

3. **Select Members**

   - Go to **"Members"** tab
   - **Assign access to**: **"Managed identity"**
   - Click **"+ Select members"**
   - **Managed identity**: Select **"Container App"**
   - Find and select: **"spring-export-app"**
   - Click **"Select"**
   - Click **"Next"**

4. **Review and Assign**
   - Go to **"Review + assign"** tab
   - Click **"Review + assign"**
   - Wait for assignment to complete

---

## 7. Testing

### Step 7.1: Test Health Endpoint

1. **Get App URL**

   - Go to your Container App **"spring-export-app"**
   - Click **"Overview"**
   - Copy the **Application Url** (e.g., `https://spring-export-app.nicegrass-12345678.eastus.azurecontainerapps.io`)

2. **Test in Browser**
   - Open your browser
   - Go to: `https://your-container-app-url/api/export/health`
   - You should see:
   ```json
   {
     "status": "UP",
     "service": "Spring Export Email Application"
   }
   ```

### Step 7.2: Test Email Export

1. **Create Test Request**

   - Open PowerShell
   - Run this command (replace with your values):

   ```powershell
   $url = "https://spring-export-app.nicegrass-12345678.eastus.azurecontainerapps.io/api/export/table"

   $body = @{
       tableName = "People"
       recipientEmail = "your-email@example.com"
       subject = "Test Data Export"
   } | ConvertTo-Json

   Invoke-RestMethod -Uri $url -Method POST -Body $body -ContentType "application/json"
   ```

2. **Expected Response**

   ```json
   {
     "status": "success",
     "message": "Data exported and email sent successfully",
     "messageId": "xxxxx-xxxxx-xxxxx",
     "tableName": "People"
   }
   ```

3. **Check Email**
   - Check the recipient's inbox
   - Look for email with subject "Test Data Export"
   - Email should contain HTML table with data from People table

---

## 8. Monitoring & Troubleshooting

### Step 8.1: View Application Logs

1. **Real-time Logs**

   - Go to your Container App **"spring-export-app"**
   - Click **"Log stream"** in left menu under **"Monitoring"**
   - Select **"Console logs"**
   - Watch logs in real-time

2. **Log Analytics** (If enabled during environment creation)
   - Go to your Container App
   - Click **"Logs"** in left menu
   - Use Kusto queries to search logs:
   ```kusto
   ContainerAppConsoleLogs_CL
   | where ContainerAppName_s == "spring-export-app"
   | order by TimeGenerated desc
   | take 50
   ```

### Step 8.2: Common Issues

**Issue 1: Container App shows "Failed" status**

- Go to **"Revision management"** → Check for errors
- View **"Log stream"** for startup errors
- Check environment variables are set correctly
- Verify JAR file was built successfully (`mvn clean package`)

**Issue 2: Email not sending**

- Verify sender address matches your domain
- Check Managed Identity has Contributor role on Communication Services
- Go to Communication Services → **"Metrics"** → Check email send metrics

**Issue 3: Database connection failed**

- Go to SQL Server → **"Networking"** → Verify firewall rules
- Check if "Allow Azure services and resources to access this server" is enabled
- Verify DB_URL format for SQL Server:
  - Should be: `jdbc:sqlserver://SERVER:1433;database=DATABASE;encrypt=true;trustServerCertificate=false`
- Verify DB_USERNAME and DB_PASSWORD match your Function App's SQL credentials
- Test connection using Azure Portal Query Editor

**Issue 4: 404 Not Found**

- Verify container app is running (check **"Revision management"**)
- Check URL is correct: `https://.../api/export/table`
- View **"Log stream"** for application startup errors
- Verify port 8080 is correctly configured in Ingress settings

**Issue 5: Container fails to start**

- Check **"Log stream"** for Java errors
- Verify all required environment variables are set
- Check Container Apps logs for Java runtime errors
- Check Container Apps logs for Java runtime errors

---

## 9. Production Checklist

Before going to production:

- [ ] Use **Consumption** or **Dedicated** workload profile for Container Apps Environment
- [ ] Use **Custom Domain** for email (not Azure Managed Domain)
- [ ] Configure **Auto-scaling** (Container Apps → Scale)
- [ ] Set up **Azure Monitor** alerts for failures
- [ ] Review **Firewall rules** on SQL Server database
- [ ] Use **Azure Key Vault** for sensitive data (passwords)
- [ ] Set up **CI/CD pipeline** (Azure DevOps or GitHub Actions)
- [ ] Configure **Custom domain** for Container Apps
- [ ] Enable **HTTPS** with SSL/TLS certificates
- [ ] Review **Cost management** and set budgets
- [ ] Enable **Container Apps diagnostics** settings
- [ ] Set up **health probes** for container

---

## Quick Reference: All Settings

### Resource Group

- **Name**: `rg-spring-export-app`
- **Region**: `East US`

### Communication Services

- **Name**: `acs-email-service-yourname123`
- **Endpoint**: `https://acs-email-service-yourname123.communication.azure.com`

### Email Service

- **Name**: `email-service-yourname123`
- **Domain**: Azure Managed or Custom
- **Sender**: `DoNotReply@xxxxx.azurecomm.net`

### SQL Server (Azure SQL Database)

- **Server**: `your-server.database.windows.net`
- **Database**: `PersonDatabase` (same as Function App)
- **Table**: `People` (with columns: Id, Name, Age, CreatedDate)
- **Username**: Your SQL admin username
- **Port**: `1433`

### Azure Container Registry

- **Name**: `acrspringexportyourname123`
- **Login server**: `acrspringexportyourname123.azurecr.io`
- **Image**: `spring-export-app:latest`

### Container Apps Environment

- **Name**: `cae-spring-export`
- **Type**: Consumption

### Container App

- **Name**: `spring-export-app`
- **URL**: `https://spring-export-app.nicegrass-12345678.eastus.azurecontainerapps.io`
- **Port**: `8080`
- **CPU**: `0.5 cores`
- **Memory**: `1.0 Gi`

### Environment Variables Summary

```
AZURE_COMMUNICATION_ENDPOINT=https://acs-email-service-yourname123.communication.azure.com
EMAIL_SENDER_ADDRESS=DoNotReply@xxxxx-xxxx-xxxx-xxxx-xxxxx.azurecomm.net
DB_URL=jdbc:sqlserver://your-server.database.windows.net:1433;database=PersonDatabase;encrypt=true;trustServerCertificate=false
DB_USERNAME=your-sql-admin-username
DB_PASSWORD=YourSqlPassword123!
DB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
SPRING_PROFILES_ACTIVE=prod
```

---

## 🎉 Congratulations!

Your Spring Boot application is now deployed and running in Azure Container Apps!

**Next Steps:**

- Test the health endpoint and email export functionality
- Set up monitoring and alerts
- Configure your API service app to call this endpoint
- Set up CI/CD for automated deployments

---

**Need Help?**

- Check **Log stream** in Azure Portal for real-time logs
- Review **Container App metrics** for performance
- Use **Log Analytics** for detailed query and analysis
- Consult the project **README.md** and other guides
