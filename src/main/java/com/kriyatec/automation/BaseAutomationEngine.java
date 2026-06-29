package com.kriyatec.automation;

import com.kriyatec.automation.excelutil.CustomHtmlReportGenerator;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.Base64;

public abstract class BaseAutomationEngine {
    protected Page page;
    protected BrowserContext context;
    protected final DynamicValueResolver resolver;

    public BaseAutomationEngine(Page page, BrowserContext context, DynamicValueResolver resolver) {
        this.page = page;
        this.context = context;
        this.resolver = resolver;
        if (page != null) {
            CustomHtmlReportGenerator.initPage(page);
        }
    }

    protected Page getActivePage() {
        return page;
    }

    protected void smartWait(String xpath, int timeoutMs) {
        try {
            getActivePage().waitForSelector(xpath, new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));
        } catch (Exception e) {
            System.out.println("   [WARN] Smart wait failed for: " + xpath);
        }
    }

    protected void fill(String xpath, String text) {
        smartWait(xpath, 10000);
        getActivePage().locator(xpath).scrollIntoViewIfNeeded();
        getActivePage().fill(xpath, text != null ? text : "");
    }

    protected void click(String xpath) {
        smartWait(xpath, 10000);
        getActivePage().locator(xpath).scrollIntoViewIfNeeded();
        getActivePage().click(xpath);
    }

    protected void pressEscape() {
        getActivePage().keyboard().press("Escape");
        System.out.println("   [INFO] Escape key pressed to close overlay/dropdown.");
    }

    protected void hoverAndClick(String hoverXpath, String clickXpath) {
        try {
            System.out.println("   🖱️ Hovering on: " + hoverXpath + " then clicking: " + clickXpath);

            Locator hoverLocator = getActivePage().locator(hoverXpath);

            // Hover with force and slow movement for reliability
            hoverLocator.scrollIntoViewIfNeeded();
            hoverLocator.hover(new Locator.HoverOptions().setForce(true));

            if (clickXpath == null || clickXpath.trim().isEmpty()) {
                System.out.println("   [INFO] No click target provided, performing hover only.");
                getActivePage().waitForTimeout(800);
                return;
            }

            Locator clickLocator = getActivePage().locator(clickXpath);

            // Small delay to let filter icon appear
            getActivePage().waitForTimeout(800);

            // Click the filter icon
            clickLocator.scrollIntoViewIfNeeded();
            clickLocator.click(new Locator.ClickOptions().setForce(true));

            System.out.println("   ✅ Hover + Click successful");
        } catch (Exception e) {
            System.err.println("   ❌ Hover + Click failed: " + e.getMessage());
            throw e;
        }
    }

    protected void hover(String xpath) {
        smartWait(xpath, 5000);
        getActivePage().locator(xpath).scrollIntoViewIfNeeded();
        getActivePage().locator(xpath).hover(new Locator.HoverOptions().setForce(true));
        getActivePage().waitForTimeout(600); // Wait for UI to react
    }

    protected String screenshotBase64() {
        try {
            byte[] screenshot = getActivePage().screenshot(new Page.ScreenshotOptions().setFullPage(true));
            return Base64.getEncoder().encodeToString(screenshot);
        } catch (Exception e) {
            return "";
        }
    }

    protected void dragAndDrop(String sourceXpath, String targetXpath) {
        try {
            Locator source = getActivePage().locator(sourceXpath);
            Locator target = getActivePage().locator(targetXpath);
            source.scrollIntoViewIfNeeded();
            target.scrollIntoViewIfNeeded();
            source.dragTo(target);
        } catch (Exception e) {
            dragAndDropWithMouse(sourceXpath, targetXpath);
        }
    }

    protected void dragAndDropWithMouse(String sourceXpath, String targetXpath) {
        Locator source = getActivePage().locator(sourceXpath);
        Locator target = getActivePage().locator(targetXpath);
        BoundingBox s = source.boundingBox();
        BoundingBox t = target.boundingBox();
        if (s == null || t == null)
            throw new RuntimeException("Elements not visible for drag-drop");

        Mouse mouse = getActivePage().mouse();
        mouse.move(s.x + s.width / 2, s.y + s.height / 2);
        mouse.down();
        getActivePage().waitForTimeout(500);
        mouse.move(t.x + t.width / 2, t.y + t.height / 2, new Mouse.MoveOptions().setSteps(10));
        mouse.up();
    }

    protected void selectSingleDropdown(String xpath, String optionXpath, String value) {
        click(xpath);
        if (optionXpath != null && !optionXpath.isBlank()) {
            String targetXpath = optionXpath.replace("{{VALUE}}", value);

            // If the Xpath still matches multiple elements, try to filter by text
            Locator options = getActivePage().locator(targetXpath);
            if (options.count() > 1) {
                System.out.println(
                        "   [INFO] Multiple options found for '" + targetXpath + "'. Filtering by text: " + value);
                options.filter(new Locator.FilterOptions().setHasText(value)).first().click();
            } else {
                click(targetXpath);
            }
        } else {
            getActivePage().selectOption(xpath, value);
        }
    }

    protected void selectMultiDropdown(String xpath, String optionXpath, String value) {
        click(xpath);
        String[] values = value.split(",");
        for (String v : values) {
            String trimmedValue = v.trim();
            String targetXpath = optionXpath != null ? optionXpath.replace("{{VALUE}}", trimmedValue) : trimmedValue;

            // If the Xpath still matches multiple elements, try to filter by text
            Locator options = getActivePage().locator(targetXpath);
            if (options.count() > 1) {
                System.out.println("   [INFO] Multiple options found for '" + targetXpath + "'. Filtering by text: "
                        + trimmedValue);
                options.filter(new Locator.FilterOptions().setHasText(trimmedValue)).first().click();
            } else {
                click(targetXpath);
            }
        }
        // Click the field again to close the dropdown as requested
        click(xpath);
    }

    protected boolean isVisible(String xpath) {
        try {
            return getActivePage().locator(xpath).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    protected void openUserInNewTab(String userRef) {
        boolean isUrl = userRef.startsWith("http");
        String url = isUrl ? userRef : getActivePage().url();

        BrowserContext newContext = getActivePage().context().browser().newContext();
        Page newPage = newContext.newPage();
        newPage.navigate(url);

        this.page = newPage;
        this.context = newContext;
        CustomHtmlReportGenerator.initPage(page);
    }

    protected void verify(String type, String xpath, String expected, String name, String scenario, String fieldType) {
        String actual = getValue(type, xpath);
        boolean pass = switch (type.toLowerCase()) {
            case "textequals" -> actual.equals(expected);
            case "textcontains" -> actual.contains(expected);
            case "visible" -> isVisible(xpath);
            case "notvisible" -> !isVisible(xpath);
            default -> false;
        };
        CustomHtmlReportGenerator.addStepTestWithVerification("VERIFY: " + name, pass,
                "Exp: " + expected + ", Got: " + actual, "-", scenario, actual);
    }

    protected String getValue(String type, String xpath) {
        try {
            return getActivePage().locator(xpath).textContent().trim();
        } catch (Exception e) {
            return "";
        }
    }

    protected void cleanup() {
        if (context != null)
            context.close();
    }

    protected void uploadMedia(String xpath, String fileName, String type) {
        String subDir = type.equalsIgnoreCase("Image") ? "images/" : "videos/";
        String filePath = "src/test/resources/files/" + subDir + fileName;
        getActivePage().setInputFiles(xpath, java.nio.file.Paths.get(filePath));
    }

    protected String clean(String s) {
        if (s == null)
            return null;
        return s.replaceAll("\\.0$", "").trim();
    }
}
