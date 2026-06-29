package com.kriyatec.automation.models;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;

public class Captcha {

    public static void main(String[] args) throws Exception {
        String apiKey = "AIzaSyBcKbtVrsmXKRVDMhl94TXWrLL46BUFUxY";

        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions().setHeadless(false));

        Page page = browser.newPage();
        page.navigate("https://www.tpcindia.com/Admin/signin.aspx");

        // 1. Locate the CAPTCHA element
        Locator captchaLocator = page.locator("img[src*='CaptchaImage']");
        captchaLocator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        // 2. Resolve the Absolute URL
        // This fixes the "Invalid URL" error by combining the base site with the .axd path
        String relativeSrc = captchaLocator.getAttribute("src");
        String absoluteUrl = page.evaluate("src => new URL(src, window.location.href).href", relativeSrc).toString();
        System.out.println("Resolved Captcha URL: " + absoluteUrl);

        // 3. Download the image bytes using the browser's context
        // This is "downloading" instead of "screenshotting"
        APIResponse response = page.request().get(absoluteUrl);

        if (response.status() != 200) {
            System.err.println("Failed to download image from address.");
            return;
        }

        byte[] imageBytes = response.body();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 4. Solve via Gemini
        System.out.println("Sending downloaded image data to Gemini...");
        String captchaText = solveCaptcha(base64Image, apiKey);

        // 5. Clean and Fill
        captchaText = captchaText.replaceAll("[^A-Za-z0-9]", "");
        System.out.println("CAPTCHA SOLVED: " + captchaText);

        // 6. Fill username
        page.fill("//input[@id='txt_uid']", "TRZ");
        System.out.println("Username filled: TRZ");
        Thread.sleep(300);

        // 7. Fill password
        page.fill("//input[@id='txt_pwd']", "tts");
        System.out.println("Password filled: tts");
        Thread.sleep(300);

        // 8. Fill CAPTCHA
        if (!captchaText.isEmpty()) {
            page.fill("//input[@id='txtCaptcha']", captchaText);
            System.out.println("CAPTCHA filled: " + captchaText);
        }

        // 9. Click login button using mouse action
        Locator loginButton = page.locator("//button[@id='btnLogin']");
        // loginButton.scrollIntoViewIfNeeded();
        loginButton.hover();
        Thread.sleep(200);
        page.mouse().click(loginButton.boundingBox().x + loginButton.boundingBox().width / 2,
                loginButton.boundingBox().y + loginButton.boundingBox().height / 2);
        System.out.println("Login button clicked with mouse!");

        Thread.sleep(3000);

        // Check for "Return to Login page" link and retry login if found
        while (page.locator("//a[.='Return to Login page.']").isVisible()) {
            System.out.println("Found 'Return to Login page' link. Retrying login...");
            page.locator("//a[.='Return to Login page.']").click();
            Thread.sleep(2000);
            
            // Re-solve CAPTCHA
            captchaLocator = page.locator("img[src*='CaptchaImage']");
            captchaLocator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            relativeSrc = captchaLocator.getAttribute("src");
            absoluteUrl = page.evaluate("src => new URL(src, window.location.href).href", relativeSrc).toString();
            response = page.request().get(absoluteUrl);
            imageBytes = response.body();
            base64Image = Base64.getEncoder().encodeToString(imageBytes);
            captchaText = solveCaptcha(base64Image, apiKey);
            captchaText = captchaText.replaceAll("[^A-Za-z0-9]", "");
            
            // Re-fill credentials
            page.fill("//input[@id='txt_uid']", "TRZ");
            page.fill("//input[@id='txt_pwd']", "tts");
            page.fill("//input[@id='txtCaptcha']", captchaText);
            
            // Re-click login
            loginButton = page.locator("//button[@id='btnLogin']");
            loginButton.hover();
            Thread.sleep(200);
            page.mouse().click(loginButton.boundingBox().x + loginButton.boundingBox().width / 2,
                    loginButton.boundingBox().y + loginButton.boundingBox().height / 2);
            Thread.sleep(3000);
        }

        // Wait for File Transfer link to be available
        Locator FileTransfer = page.locator("//a[@href='file-transfer.aspx']");
        FileTransfer.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        FileTransfer.click();

        page.waitForLoadState();

            Thread.sleep(1000);

        // Select Origin radio
        Locator originRadio = page.locator("//label[.='Origin']");
        if (!originRadio.isChecked()) {
            originRadio.check();
        }

        // Get today's date
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String currentDate = today.format(formatter);

        System.out.println("Today's Date: " + currentDate);

        // Select date
        Locator dateDropdown = page.locator("#ContentPlaceHolderBody_DropDownList1");
        dateDropdown.selectOption(currentDate);
        System.out.println("Date selected: " + currentDate);
            Thread.sleep(1000);

        // Create folder for downloads
        new java.io.File("tpc").mkdirs();

        // Download DRS file
        Download drsDownload = page.waitForDownload(() -> {
            page.locator("#ContentPlaceHolderBody_btn_odrs").click();
        });

        String drsPath = "tpc/" + drsDownload.suggestedFilename();
        drsDownload.saveAs(Paths.get(drsPath));

        System.out.println("DRS Downloaded: " + drsPath);
            
        Thread.sleep(1000);

        // Download IOM file
        Download iomDownload = page.waitForDownload(() -> {
            page.locator("#ContentPlaceHolderBody_btn_oiom").click();
        });

        String iomPath = "tpc/" + iomDownload.suggestedFilename();
        iomDownload.saveAs(Paths.get(iomPath));

        System.out.println("IOM Downloaded: " + iomPath);

        Thread.sleep(1000);

        // Select Destination radio
        Locator destinationRadio = page.locator("//label[.='Destination']");
        destinationRadio.check();

            Thread.sleep(1000);

        LocalDate yesterday = LocalDate.now().minusDays(1);

        DateTimeFormatter des_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String destDate = yesterday.format(des_formatter);

        System.out.println("Selected Date: " + destDate);
            Thread.sleep(1000);
        dateDropdown.selectOption(destDate);

        Thread.sleep(1000);

        // Download DRS file
        Download destinationMFTDownload = page.waitForDownload(() -> {
            page.locator("#ContentPlaceHolderBody_btn_omft").click();
        });


        String destinationMFTPath = "tpc/" + destinationMFTDownload.suggestedFilename();
        destinationMFTDownload.saveAs(Paths.get(destinationMFTPath));

        System.out.println("Destnation MFT Downloaded: " + destinationMFTPath);

        Thread.sleep(1000);

        browser.close();

        Thread.sleep(1000);

        playwright.close();

    }

    private static String solveCaptcha(String base64Image, String apiKey) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();

        parts.add(createPart("text", "Read the characters in this image. Return only the text."));
        parts.add(createImagePart(base64Image));

        contentObj.add("parts", parts);
        contents.add(contentObj);
        payload.add("contents", contents);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> apiRes = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(apiRes.body()).getAsJsonObject();

        return json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString().trim();
    }

    private static JsonObject createPart(String key, String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty(key, value);
        return obj;
    }

    private static JsonObject createImagePart(String base64) {
        JsonObject obj = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mime_type", "image/png");
        inlineData.addProperty("data", base64);
        obj.add("inline_data", inlineData);
        return obj;
    }
}
