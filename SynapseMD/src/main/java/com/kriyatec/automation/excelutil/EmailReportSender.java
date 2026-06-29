package com.kriyatec.automation.excelutil;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EmailReportSender {

    private static Properties loadConfig() {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/conf.env")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load conf.env: " + e.getMessage());
        }
        return config;
    }

    public static void sendReport(String reportPath, String subject) {
        List<String> reportPaths = new ArrayList<>();
        reportPaths.add(reportPath);
        sendMultipleReports(reportPaths, subject, null);
    }

    public static void sendMultipleReports(List<String> reportPaths, String subject, int[] totalMetrics) {
        Properties config = loadConfig();
        
        String emailTo = config.getProperty("EMAIL_TO", "");
        String emailCc = config.getProperty("EMAIL_CC", "");
        String emailFrom = config.getProperty("EMAIL_FROM", "automation@example.com");
        String emailHost = config.getProperty("EMAIL_HOST", "smtp.gmail.com");
        String emailPort = config.getProperty("EMAIL_PORT", "587");
        String emailUsername = config.getProperty("EMAIL_USERNAME", "");
        String emailPassword = config.getProperty("EMAIL_PASSWORD", "");

        if (emailTo.isEmpty() || emailUsername.isEmpty() || emailPassword.isEmpty()) {
            System.err.println("Email configuration incomplete in conf.env");
            return;
        }
        
        // Create zip file with timestamp
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new java.util.Date());
        String zipPath = "reports/" + timestamp + "_test-reports.zip";
        try {
            createZipFile(reportPaths, zipPath);
        } catch (IOException e) {
            System.err.println("Failed to create zip file: " + e.getMessage());
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.port", emailPort);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", emailHost);
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.connectiontimeout", "60000");
        props.put("mail.smtp.writetimeout", "60000");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailUsername, emailPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            
            // Add TO recipients
            String[] toAddresses = emailTo.split(",");
            InternetAddress[] toRecipients = new InternetAddress[toAddresses.length];
            for (int i = 0; i < toAddresses.length; i++) {
                toRecipients[i] = new InternetAddress(toAddresses[i].trim());
            }
            message.setRecipients(Message.RecipientType.TO, toRecipients);
            
            // Add CC recipients if present
            if (!emailCc.isEmpty()) {
                String[] ccAddresses = emailCc.split(",");
                InternetAddress[] ccRecipients = new InternetAddress[ccAddresses.length];
                for (int i = 0; i < ccAddresses.length; i++) {
                    ccRecipients[i] = new InternetAddress(ccAddresses[i].trim());
                }
                message.setRecipients(Message.RecipientType.CC, ccRecipients);
            }

            message.setSubject(subject);

            // Create email body
            MimeBodyPart textPart = new MimeBodyPart();
            int[] metrics = totalMetrics != null ? totalMetrics : new int[]{0,0,0,0,0,0};
            String emailBody = String.format(
                "Hi Team,\n\n" +
                "Please find the attached test execution report.\n\n" +
                "Summary:\n" +
                "- Total Steps: %d\n" +
                "- Steps Passed: %d\n" +
                "- Steps Failed: %d\n" +
                "- Total Test Cases: %d\n" +
                "- Test Cases Passed: %d\n" +
                "- Test Cases Failed: %d\n\n" +
                "Best Regards,\n" +
                "Automation Team",
                metrics[0], metrics[1], metrics[2], metrics[3], metrics[4], metrics[5]
            );
            textPart.setText(emailBody);

            // Attach zip file
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            
            MimeBodyPart zipAttachment = new MimeBodyPart();
            File zipFile = new File(zipPath);
            if (zipFile.exists()) {
                // Set custom filename for attachment
                String attachmentName = new File(zipPath).getName();
                zipAttachment.attachFile(zipFile);
                zipAttachment.setFileName(attachmentName);
                multipart.addBodyPart(zipAttachment);
            }

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("✅ Email sent successfully to: " + emailTo);
            if (!emailCc.isEmpty()) {
                System.out.println("   CC: " + emailCc);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createZipFile(List<String> filePaths, String zipPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (!file.exists()) continue;
                
                // Preserve folder structure by using relative path from reports/
                String zipEntryName = filePath.startsWith("reports/") ? filePath.substring(8) : file.getName();
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);
                    
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
            
            // Add testData file
            File testDataFile = new File("reports/testData");
            if (testDataFile.exists()) {
                try (FileInputStream fis = new FileInputStream(testDataFile)) {
                    ZipEntry zipEntry = new ZipEntry("testData");
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
            
            // Add screenshot folder
            File screenshotDir = new File("reports/screenshot");
            if (screenshotDir.exists() && screenshotDir.isDirectory()) {
                addDirectoryToZip(zos, screenshotDir, "screenshot");
            }
        }
    }
    
    private static void addDirectoryToZip(ZipOutputStream zos, File dir, String parentPath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            String zipPath = parentPath + "/" + file.getName();
            if (file.isDirectory()) {
                addDirectoryToZip(zos, file, zipPath);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(zipPath);
                    zos.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
        }
    }
}
