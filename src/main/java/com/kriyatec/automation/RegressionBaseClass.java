package com.kriyatec.automation;

import com.kriyatec.automation.excelutil.CustomHtmlReportGenerator;
import com.kriyatec.automation.excelutil.ExcelUtils;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.awt.Toolkit;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RegressionBaseClass extends BaseAutomationEngine {

	public Playwright playwright;
	public Browser browser;
	public String regressionId;

	public RegressionBaseClass() {
		super(null, null, new DynamicValueResolver());
	}

	public void initialize(String browserType, String regressionId, String excelPath, String testType)
			throws Exception {
		this.regressionId = regressionId;
		playwright = Playwright.create();

		Map<String, List<Map<String, String>>> configData = ExcelUtils.getAllData(excelPath);
		List<Map<String, String>> configRows = configData.get("Config");
		boolean headlessMode = false;
		String appUrl = null;

		for (Map<String, String> config : configRows) {
			if (browserType.equalsIgnoreCase(config.get("Browser Type"))
					&& testType.equalsIgnoreCase(config.get("Test Type"))) {
				appUrl = config.get("Application URL");
				String headless = config.get("Headless Mode");
				if (headless == null)
					headless = config.get("Headless\u00A0Mode");
				headlessMode = "Yes".equalsIgnoreCase(headless != null ? headless.trim() : "");
				break;
			}
		}

		if (appUrl == null)
			throw new RuntimeException("Application URL not found in Config");

		BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
				.setHeadless(headlessMode)
				.setArgs(List.of("--start-maximized"));
		browser = switch (browserType.toLowerCase()) {
			case "chrome" -> playwright.chromium().launch(options);
			case "firefox" -> playwright.firefox().launch(options);
			default -> throw new IllegalArgumentException("Unsupported browser: " + browserType);
		};

		context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
		page = context.newPage();
		page.setDefaultTimeout(15000);

		page.navigate(appUrl, new Page.NavigateOptions().setTimeout(60000));

		// Apply 90% zoom factor to ensure footer and right side are visible on all
		// machines
		page.evaluate("document.body.style.zoom = '0.9'");

		CustomHtmlReportGenerator.initPage(page);
	}

	public void processLogin(String excelPath, String sheetName) throws IOException {
		CustomHtmlReportGenerator.initFeature("Login Feature");
		CustomHtmlReportGenerator.initStepTests();

		var data = ExcelUtils.getAllData(excelPath).get(sheetName);
		for (var row : data) {
			String xpath = row.get("Field Xpath");
			String type = row.get("Field Type");
			String value = regressionId != null ? row.get(regressionId) : row.get("Functional Login");

			if (xpath == null || xpath.isEmpty())
				continue;

			try {
				if (type.equalsIgnoreCase("Input"))
					fill(xpath, value);
				else if (type.equalsIgnoreCase("Button") || type.equalsIgnoreCase("Click"))
					click(xpath);

				CustomHtmlReportGenerator.addStepTest("Login: " + row.get("Field Name"), true, "Action performed",
						value, "Positive");
			} catch (Exception e) {
				CustomHtmlReportGenerator.addStepTest("Login Error: " + row.get("Field Name"), false, e.getMessage(),
						"-", "Negative");
			}
		}
		CustomHtmlReportGenerator.addStepGroup("Login Steps");
	}

	public void processNavigationSteps(String excelPath, String sheetName) throws IOException {
		var data = ExcelUtils.getAllData(excelPath).get(sheetName);
		if (data == null)
			return;

		for (var row : data) {
			String xpath = row.get("Navigation Xpath");
			if (xpath != null)
				click(xpath);
		}
	}

	public void processExcelSheetForRegression(String excelPath, String sheetName) throws IOException {
		var allData = ExcelUtils.getAllData(excelPath);
		var stepGroups = allData.get("Step Groups");
		var regressionData = allData.get(sheetName);

		if (stepGroups == null || regressionData == null)
			return;

		for (var group : stepGroups) {
			String stepId = clean(group.get("Step ID"));
			if (stepId == null)
				continue;

			CustomHtmlReportGenerator.initStepTests();
			for (var row : regressionData) {
				if (stepId.equals(clean(row.get("Step ID")))) {
					processRegressionStep(row);
				}
			}
			CustomHtmlReportGenerator.addStepGroup(
					group.get("Field Name") != null ? group.get("Field Name") : group.get("Step Group Name"));
		}
	}

	private void processRegressionStep(Map<String, String> row) {
		String name = row.get("Field Name");
		String xpath = row.get("Field Xpath");
		String value = regressionId != null ? row.get(regressionId) : row.get("Field Value");
		String type = row.get("Field Type");
		String dropXpath = row.get("DropdownOption Xpath");

		if (xpath == null || xpath.isEmpty())
			return;

		try {
			switch (type.toLowerCase()) {
				case "input" -> fill(xpath, value);
				case "click", "button" -> click(xpath);
				case "dropdown" -> selectSingleDropdown(xpath, dropXpath, value);
				case "multiselect" -> selectMultiDropdown(xpath, dropXpath, value);
				case "escape" -> pressEscape();
				case "hover" -> hover(xpath);
				case "hoverandclick" -> hoverAndClick(xpath, dropXpath != null ? dropXpath : value);
				case "presskey" -> getActivePage().keyboard().press(value);
				default -> {
				}
			}
			CustomHtmlReportGenerator.addStepTest("Regression: " + name, true, "Action performed", value, "Positive");
		} catch (Exception e) {
			CustomHtmlReportGenerator.addStepTest("Regression Error: " + name, false, e.getMessage(), "-", "Negative");
		}
	}

	public Page getPage() {
		return page;
	}

	public void cleanup() {
		if (context != null)
			context.close();
		if (playwright != null)
			playwright.close();
	}
}