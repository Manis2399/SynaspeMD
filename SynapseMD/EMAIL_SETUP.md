# Email Report Configuration

## Setup Instructions

### 1. Update conf.env File
Edit `src/main/resources/conf.env` and configure the following properties:

```properties
# Email Configuration
EMAIL_TO=manager@example.com,tester@example.com
EMAIL_CC=qa@example.com
EMAIL_FROM=automation@example.com
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
```

### 2. Gmail Setup (if using Gmail)

#### Generate App Password:
1. Go to Google Account Settings
2. Navigate to Security → 2-Step Verification (enable if not already)
3. Go to Security → App passwords
4. Select "Mail" and "Other (Custom name)"
5. Generate password and copy it
6. Use this password in `EMAIL_PASSWORD` field

### 3. Configuration Details

- **EMAIL_TO**: Comma-separated list of recipient emails (manager, team members)
- **EMAIL_CC**: Comma-separated list of CC recipients
- **EMAIL_FROM**: Sender email address
- **EMAIL_HOST**: SMTP server (smtp.gmail.com for Gmail, smtp.office365.com for Outlook)
- **EMAIL_PORT**: SMTP port (587 for TLS, 465 for SSL)
- **EMAIL_USERNAME**: Your email username
- **EMAIL_PASSWORD**: Your email password or app-specific password

### 4. Other Email Providers

#### Outlook/Office365:
```properties
EMAIL_HOST=smtp.office365.com
EMAIL_PORT=587
```

#### Yahoo:
```properties
EMAIL_HOST=smtp.mail.yahoo.com
EMAIL_PORT=587
```

### 5. Maven Dependency
Already added to pom.xml:
```xml
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>
```

### 6. How It Works

After test execution completes:
1. HTML report is generated
2. Email is automatically sent with:
   - Test execution summary in email body
   - HTML report attached
   - Sent to all configured recipients (TO and CC)

### 7. Email Body Includes:
- Total Steps (Passed/Failed)
- Total Test Cases (Passed/Failed)
- Attached HTML report

### 8. Troubleshooting

**Email not sending?**
- Check conf.env configuration
- Verify SMTP credentials
- For Gmail, ensure "Less secure app access" is enabled OR use App Password
- Check firewall/network settings for SMTP port access
- Review console logs for error messages

**Authentication failed?**
- Use App Password instead of regular password (Gmail)
- Enable 2-factor authentication first (Gmail)
- Check username/password are correct

## Example Output

```
✅ Email sent successfully to: manager@example.com,tester@example.com
   CC: qa@example.com
```
