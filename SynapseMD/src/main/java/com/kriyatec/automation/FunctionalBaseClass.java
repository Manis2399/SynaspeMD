package com.kriyatec.automation;

import com.kriyatec.automation.excelutil.*;
import java.util.*;

public class FunctionalBaseClass extends BaseAutomationEngine {

    private final ThreadLocal<Map<String, String>> capturedValues = ThreadLocal.withInitial(HashMap::new);

    public FunctionalBaseClass(RegressionBaseClass base) {
        super(base.getPage(), base.context, new DynamicValueResolver());
    }

    public void run(String excelPath, String sheetName) throws Exception {
        var data = ExcelUtils.getAllData(excelPath);
        var steps = data.get(sheetName);
        var groups = data.get("Step Groups");

        for (var group : groups) {
            String stepId = clean(group.get("Step ID"));
            if (stepId == null)
                continue;

            for (var row : steps) {
                if (stepId.equals(clean(row.get("Step ID")))) {
                    CustomHtmlReportGenerator.initStepTests();
                    executeStep(row);
                    CustomHtmlReportGenerator.addStepGroup(stepId + ": " + row.get("Field Name"));
                }
            }
        }
    }

    private void executeStep(Map<String, String> r) {
        String stepId = clean(r.get("Step ID"));
        String name = r.get("Field Name");
        String xpath = resolver.resolve(r.get("Field Xpath"));
        String type = r.get("Field Type");
        String rawValue = r.get("Field Value");
        String value = resolver.resolve(rawValue);
        String verify = r.get("Verification Type");
        String vXpath = r.get("Verification Xpath");
        String expected = r.get("Expected Value");
        String postCheck = r.get("Post Step Verification");
        String scenario = r.get("Scenario Type");
        String dropXpath = r.get("DropdownOption Xpath");

        boolean isVerificationOnly = (type == null || type.isBlank()) && verify != null && !verify.isBlank();
        boolean isCaptureOnly = "Yes".equalsIgnoreCase(postCheck) && verify != null && vXpath != null;

        if ((type == null || type.isBlank()) && !isVerificationOnly && !isCaptureOnly) {
            System.out.println("   ⚠️  SKIP Step ID " + stepId + ": No Field Type and No Verification - Step: " + name);
            return;
        }

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🎯 Step ID: " + stepId + " | 📋 " + name);
        System.out.println("   Type: " + (isVerificationOnly ? "Verification Only" : type)
                + (value != null && !value.isEmpty() ? " | Input: " + value : ""));

        try {
            if (isCaptureOnly) {
                String capturedValue = getValue(verify, vXpath);
                capturedValues.get().put(vXpath, capturedValue);
                CustomHtmlReportGenerator.addStepTest("CAPTURE: " + name, true, "Captured: " + capturedValue,
                        capturedValue, "Positive");
                return;
            }

            if (!isVerificationOnly) {
                System.out.println("   🔧 Executing: " + type.toLowerCase());
                switch (type.toLowerCase()) {
                    case "input" -> fill(xpath, value);
                    case "click", "button" -> click(xpath);
                    case "dropdown" -> selectSingleDropdown(xpath, dropXpath, value);
                    case "multiselect" -> selectMultiDropdown(xpath, dropXpath, value);
                    case "image", "video" -> uploadMedia(xpath, value, type);
                    case "clear" -> getActivePage().locator(xpath).clear();
                    // case "hover" -> getActivePage().locator(xpath).hover();
                    case "doubleclick" -> getActivePage().dblclick(xpath);
                    case "wait" -> getActivePage().waitForTimeout(Long.parseLong(value));
                    case "newtab" -> openUserInNewTab(value);
                    case "escape" -> pressEscape();
                    case "presskey" -> getActivePage().keyboard().press(value);
                    case "hover" -> hover(xpath);
                    case "hoverandclick" -> hoverAndClick(xpath, dropXpath != null ? dropXpath : value);
                    default -> {
                    }
                }
                CustomHtmlReportGenerator.addStepTest("ACTION: " + name, true, "Value: " + value, value, scenario);
            }

            if (verify != null && !verify.isBlank()) {
                String expectedValue = (expected == null || expected.isBlank()) ? value : expected;
                verify(verify, vXpath, expectedValue, name, scenario, type);
            }

        } catch (Exception e) {
            String ss = screenshotBase64();
            CustomHtmlReportGenerator.addStepTest("FAILED: " + name, false, e.getMessage(), ss, scenario);
            System.out.println("   ❌ FAIL: " + name + " | " + e.getMessage());
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}