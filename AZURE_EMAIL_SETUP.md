# Azure Communication Services Email Setup Guide

Complete guide to set up Azure Communication Services for sending emails from your Spring application.

---

## What is Azure Communication Services?

Azure Communication Services (ACS) enables you to add communication capabilities to your applications including:

- ✉️ **Email** - Send transactional and marketing emails
- 📞 **Voice & Video** - Add calling capabilities
- 💬 **SMS** - Send text messages
- 💭 **Chat** - Real-time messaging

For this application, we only need **Email** service.

---

## Step-by-Step Setup

### Step 1: Create Azure Communication Services Resource

#### Using Azure Portal

1. **Open Azure Portal**: https://portal.azure.com
2. Click **Create a resource**
3. Search for **"Communication Services"**
4. Click **Create**
5. Fill in details:
   - **Subscription**: Select your subscription
   - **Resource Group**: Create new or select existing (e.g., `rg-spring-export-app`)
   - **Resource Name**: Unique name (e.g., `acs-email-service-unique123`)
   - **Data Location**: Choose region (e.g., `United States`)
6. Click **Review + Create**
7. Click **Create**

#### Using Azure CLI

```powershell
# Login to Azure
az login

# Set variables
$RESOURCE_GROUP = "rg-spring-export-app"
$LOCATION = "eastus"
$ACS_NAME = "acs-email-service-unique123"  # Must be globally unique

# Create resource group (if not exists)
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Communication Services
az communication create `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --location "global" `
  --data-location "United States"

# Verify creation
az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP
```

---

### Step 2: Get Communication Services Endpoint

#### Using Azure Portal

1. Go to your Communication Services resource
2. Click **Overview**
3. Copy the **Endpoint** (e.g., `https://acs-email-service-unique123.communication.azure.com`)

#### Using Azure CLI

```powershell
# Get endpoint
az communication show `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --query "hostName" -o tsv
```

**Save this endpoint** - you'll need it for `AZURE_COMMUNICATION_ENDPOINT` environment variable.

---

### Step 3: Create Email Communication Service

#### Using Azure Portal

1. In Azure Portal, search for **"Email Communication Services"**
2. Click **Create**
3. Fill in details:
   - **Subscription**: Your subscription
   - **Resource Group**: Same as Communication Services
   - **Name**: Unique name (e.g., `email-service-unique123`)
   - **Data Location**: Same region as Communication Services
4. Click **Review + Create**
5. Click **Create**

#### Using Azure CLI

```powershell
$EMAIL_SERVICE_NAME = "email-service-unique123"

az communication email create `
  --name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --location "global" `
  --data-location "United States"
```

---

### Step 4: Set Up Email Domain

You have **TWO OPTIONS**:

#### **Option A: Azure Managed Domain (Quick & Free) - Recommended for Testing**

This is the fastest way to get started. Azure provides a free domain.

##### Using Azure Portal

1. Go to your Email Communication Service resource
2. Click **Provision Domains** → **Add domain**
3. Select **Azure Managed Domain**
4. Click **Add**
5. Wait 2-5 minutes for provisioning
6. Copy the sender address (e.g., `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`)

##### Using Azure CLI

```powershell
# Create Azure Managed Domain
az communication email domain create `
  --domain-name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --domain-management "AzureManaged"

# Get sender address
az communication email domain show `
  --domain-name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --query "fromSenderDomain" -o tsv
```

**Your Sender Address**: `DoNotReply@[guid].azurecomm.net`

**⚠️ Limitations:**

- Can only use `DoNotReply@` address
- May have deliverability issues with some email providers
- Suitable for testing and internal emails
- **Recommended for testing/development only**

---

#### **Option B: Custom Domain (Production) - Recommended for Production**

Use your own domain for better deliverability and branding.

##### Prerequisites

- You own a domain (e.g., `yourdomain.com`)
- Access to DNS settings

##### Steps Using Azure Portal

1. **Add Custom Domain**

   - Go to Email Communication Service
   - Click **Provision Domains** → **Add domain**
   - Select **Custom domain**
   - Enter subdomain: `mail.yourdomain.com` or `communications.yourdomain.com`
   - Click **Add**

2. **Verify Domain Ownership**

   - Azure will show DNS TXT records
   - Copy the TXT record values
   - Add them to your DNS provider:

   ```
   Type: TXT
   Name: @
   Value: ms-domain-verification=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   TTL: 3600
   ```

3. **Wait for Verification**

   - Can take 15 minutes to 48 hours
   - Check status in portal
   - Click **Verify** when ready

4. **Configure Email Authentication (SPF, DKIM, DMARC)**

   After verification, Azure provides these DNS records:

   **SPF Record:**

   ```
   Type: TXT
   Name: @
   Value: v=spf1 include:spf.protection.outlook.com -all
   TTL: 3600
   ```

   **DKIM Records (2 records):**

   ```
   Type: CNAME
   Name: selector1-azurecomm-prod-net._domainkey
   Value: selector1-azurecomm-prod-net._domainkey.azurecomm.net
   TTL: 3600

   Type: CNAME
   Name: selector2-azurecomm-prod-net._domainkey
   Value: selector2-azurecomm-prod-net._domainkey.azurecomm.net
   TTL: 3600
   ```

   **DMARC Record:**

   ```
   Type: TXT
   Name: _dmarc
   Value: v=DMARC1; p=none; pct=100; rua=mailto:dmarc@yourdomain.com
   TTL: 3600
   ```

5. **Verify Email Authentication**
   - Click **Verify** for each record
   - All should show green checkmarks

**Your Sender Addresses**: Any address `@mail.yourdomain.com`

- `noreply@mail.yourdomain.com`
- `notifications@mail.yourdomain.com`
- `reports@mail.yourdomain.com`

---

### Step 5: Link Email Domain to Communication Services

#### Using Azure Portal

1. Go to Communication Services resource
2. Click **Email** → **Domains**
3. Click **Connect domain**
4. Select your Email Communication Service
5. Select your domain
6. Click **Connect**

#### Using Azure CLI

```powershell
# Get Communication Services resource ID
$ACS_ID = az communication show `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --query "id" -o tsv

# Link domain to Communication Services
az communication email domain update `
  --domain-name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --user-engagement-tracking "Disabled"

# For custom domain, use your domain name instead of "AzureManagedDomain"
```

---

### Step 6: Test Email Sending

#### Get Connection String (for local testing only)

```powershell
# Get primary connection string
az communication list-key `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --query "primaryConnectionString" -o tsv
```

**Format**: `endpoint=https://...;accesskey=...`

#### Test with PowerShell

Save this script as `test-email.ps1`:

```powershell
# Install Azure Communication Email module
Install-Module -Name Az.Communication -Force

# Set variables
$ResourceGroup = "rg-spring-export-app"
$AcsName = "acs-email-service-unique123"
$SenderAddress = "DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net"
$RecipientAddress = "your-email@example.com"

# Get connection string
$ConnectionString = az communication list-key `
  --name $AcsName `
  --resource-group $ResourceGroup `
  --query "primaryConnectionString" -o tsv

Write-Host "Testing email send..."
Write-Host "From: $SenderAddress"
Write-Host "To: $RecipientAddress"

# Note: You'll need to use Azure SDK in application
# This is just to verify the setup
```

---

### Step 7: Configure for Production (Managed Identity)

For production, use **Managed Identity** instead of connection strings.

```powershell
# Enable system-assigned managed identity on Spring App
az spring app identity assign `
  --name spring-export-app `
  --resource-group $RESOURCE_GROUP `
  --service <your-spring-apps-name>

# Get the principal ID
$PRINCIPAL_ID = az spring app identity show `
  --name spring-export-app `
  --resource-group $RESOURCE_GROUP `
  --service <your-spring-apps-name> `
  --query "principalId" -o tsv

# Assign Contributor role to Communication Services
az role assignment create `
  --assignee $PRINCIPAL_ID `
  --role "Contributor" `
  --scope $(az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP --query "id" -o tsv)
```

---

## Summary: What You Need for Your App

After completing the setup, you'll have:

### For Environment Variables:

1. **AZURE_COMMUNICATION_ENDPOINT**

   ```
   https://acs-email-service-unique123.communication.azure.com
   ```

2. **EMAIL_SENDER_ADDRESS**

   - Azure Managed: `DoNotReply@xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.azurecomm.net`
   - Custom: `noreply@mail.yourdomain.com`

3. **AZURE_COMMUNICATION_CONNECTION_STRING** (local dev only)
   ```
   endpoint=https://...;accesskey=...
   ```

---

## Cost Estimation

### Free Tier

- ✅ **First 1,000 emails/month**: FREE
- ✅ Azure Managed Domain: FREE
- ✅ Email storage: FREE (first 2 GB)

### Paid Usage

- 📧 **$0.00025 per email** after free tier
- 💾 **$0.15/GB** storage after 2 GB

**Example Cost:**

- 10,000 emails/month = 1,000 (free) + 9,000 × $0.00025 = **$2.25/month**
- 50,000 emails/month = 1,000 (free) + 49,000 × $0.00025 = **$12.25/month**

**Custom Domain**: Additional DNS hosting costs from your domain provider

---

## Verification Checklist

After setup, verify:

- [ ] Communication Services resource created
- [ ] Email Communication Service created
- [ ] Email domain provisioned (Azure Managed or Custom)
- [ ] Domain verification complete (for custom domains)
- [ ] SPF, DKIM, DMARC configured (for custom domains)
- [ ] Domain linked to Communication Services
- [ ] Managed Identity enabled on Spring App
- [ ] Managed Identity has Contributor role on ACS
- [ ] Environment variables configured in Spring App

---

## Testing the Setup

### Test 1: Portal Test (Azure Managed Domain Only)

1. Go to Email Communication Service
2. Click **Try Email** blade
3. Fill in test email details
4. Click **Send**
5. Check recipient inbox

### Test 2: Application Test

Once your Spring app is deployed:

```powershell
$APP_URL = "https://spring-export-app.azuremicroservices.io"

$body = @{
    tableName = "test_table"
    recipientEmail = "your-email@example.com"
    subject = "Test Email from Spring App"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$APP_URL/api/export/table" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

---

## Troubleshooting

### Issue: Email not received

**Check:**

- [ ] Sender address matches configured domain
- [ ] Domain verification complete
- [ ] Check spam/junk folder
- [ ] Verify recipient email is valid
- [ ] Check ACS quotas and limits

### Issue: "Domain not verified"

**Solution:**

- Wait 15-30 minutes after adding DNS records
- Verify DNS records propagated: `nslookup -type=TXT yourdomain.com`
- Check DNS syntax is correct (no extra spaces)

### Issue: "Authentication failed"

**Solution:**

- For local dev: Check connection string is correct
- For Azure: Verify Managed Identity has permissions
- Check endpoint URL is correct

### View Email Logs

```powershell
# Enable diagnostic settings
az monitor diagnostic-settings create `
  --name email-diagnostics `
  --resource $(az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP --query "id" -o tsv) `
  --logs '[{"category":"EmailSendMailLogs","enabled":true}]' `
  --workspace <log-analytics-workspace-id>
```

---

## Best Practices

✅ **DO:**

- Use Azure Managed Domain for testing
- Use Custom Domain for production
- Use Managed Identity in Azure
- Configure SPF, DKIM, DMARC for custom domains
- Monitor email delivery metrics
- Implement retry logic for failed emails

❌ **DON'T:**

- Use connection strings in production
- Skip SPF/DKIM/DMARC for custom domains
- Use Azure Managed Domain for high-volume production emails
- Hardcode sender addresses in application

---

## Additional Resources

- [Azure Communication Services Documentation](https://docs.microsoft.com/azure/communication-services/)
- [Email SDK Reference](https://docs.microsoft.com/azure/communication-services/concepts/email/sdk-features)
- [Email Best Practices](https://docs.microsoft.com/azure/communication-services/concepts/email/email-best-practices)
- [Domain Verification Guide](https://docs.microsoft.com/azure/communication-services/quickstarts/email/add-custom-verified-domains)

---

## Quick Setup Script

Complete PowerShell script to set up everything:

```powershell
# Variables
$RESOURCE_GROUP = "rg-spring-export-app"
$LOCATION = "eastus"
$ACS_NAME = "acs-email-service-$(Get-Random -Maximum 9999)"
$EMAIL_SERVICE_NAME = "email-service-$(Get-Random -Maximum 9999)"

# Create resources
az group create --name $RESOURCE_GROUP --location $LOCATION

az communication create `
  --name $ACS_NAME `
  --resource-group $RESOURCE_GROUP `
  --location "global" `
  --data-location "United States"

az communication email create `
  --name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --location "global" `
  --data-location "United States"

az communication email domain create `
  --domain-name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --domain-management "AzureManaged"

# Get values
$ENDPOINT = az communication show --name $ACS_NAME --resource-group $RESOURCE_GROUP --query "hostName" -o tsv
$SENDER = az communication email domain show `
  --domain-name "AzureManagedDomain" `
  --email-service-name $EMAIL_SERVICE_NAME `
  --resource-group $RESOURCE_GROUP `
  --query "fromSenderDomain" -o tsv

Write-Host "`n=== Setup Complete ==="
Write-Host "AZURE_COMMUNICATION_ENDPOINT: https://$ENDPOINT"
Write-Host "EMAIL_SENDER_ADDRESS: DoNotReply@$SENDER"
```

---

**You're now ready to send emails from your Spring application! 🎉**
