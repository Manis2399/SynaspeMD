package com.kriyatec.automation;

import com.kriyatec.automation.excelutil.CustomHtmlReportGenerator;
import com.kriyatec.automation.excelutil.ExcelUtils;
import java.io.IOException;
import java.util.Map;

public class ValidationBaseClass extends BaseAutomationEngine {

	public ValidationBaseClass(RegressionBaseClass base) {
		super(base.getPage(), base.context, base.resolver);
	}

	public void validateAndFillFields(String excelSheetPath, String sheetName) throws IOException {
		var allData = ExcelUtils.getAllData(excelSheetPath);
		var stepGroups = allData.get("Step Groups");
		var validationRows = allData.get(sheetName);

		if (stepGroups == null || validationRows == null)
			return;

		for (var group : stepGroups) {
			String stepId = clean(group.get("Step ID"));
			if (stepId == null)
				continue;

			CustomHtmlReportGenerator.initStepTests();
			for (var row : validationRows) {
				if (stepId.equals(clean(row.get("Step ID")))) {
					processValidationField(row);
				}
			}
			CustomHtmlReportGenerator.addStepGroup(group.get("Step Group Name"));
		}
	}

	private void processValidationField(Map<String, String> row) {
		String name = row.get("Field Name");
		String xpath = row.get("Field Xpath");
		String type = row.get("Field Type");
		String errorXpath = row.get("Error Message Xpath");
		String isMandatory = row.get("Mandatory Field");
		String expectedError = row.get("Error Message Type");
		String value = resolver.resolve(row.get("Field Value"));

		try {
			if ("Yes".equalsIgnoreCase(isMandatory)) {
				validateMandatory(xpath, errorXpath, expectedError, name, type);
			}

			if (xpath != null && isVisible(xpath)) {
				switch (type.toLowerCase()) {
					case "input" -> fill(xpath, value);
					case "dropdown" -> selectSingleDropdown(xpath, row.get("DropdownOption Xpath"), value);
					case "multiselect" -> selectMultiDropdown(xpath, row.get("DropdownOption Xpath"), value);
					case "button", "click" -> click(xpath);
					default -> {
					}
				}
				CustomHtmlReportGenerator.addStepTest("Validated: " + name, true, "Processed " + type, value,
						"Positive");
			}
		} catch (Exception e) {
			CustomHtmlReportGenerator.addStepTest("Validation Error: " + name, false, e.getMessage(), "-", "Negative");
		}
	}

	private void validateMandatory(String xpath, String errorXpath, String expectedMsg, String name, String type) {
		if (xpath == null || errorXpath == null)
			return;

		if (type.equalsIgnoreCase("input")) {
			getActivePage().locator(xpath).clear();
			getActivePage().locator(xpath).blur();
		} else {
			getActivePage().locator(xpath).blur();
		}

		String actualError = getActivePage().locator(errorXpath).textContent().trim();
		boolean pass = actualError.contains(expectedMsg != null ? expectedMsg : "");
		CustomHtmlReportGenerator.addStepTest("Mandatory Check: " + name, pass,
				"Expected: " + expectedMsg + ", Got: " + actualError, "-", pass ? "Positive" : "Negative");
	}
}
