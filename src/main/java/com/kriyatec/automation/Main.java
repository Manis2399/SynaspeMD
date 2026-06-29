package com.kriyatec.automation;

import java.io.FileInputStream;
import java.io.IOException;
import com.microsoft.playwright.*;

import java.awt.Toolkit;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;
import java.io.File;

import com.microsoft.playwright.*;
import com.kriyatec.automation.excelutil.CustomHtmlReportGenerator;
import com.kriyatec.automation.excelutil.ExcelUtils;
import com.kriyatec.automation.excelutil.MasterReportGenerator;
import com.kriyatec.automation.excelutil.ModuleReportGenerator;
import com.kriyatec.automation.models.ModuleMetrics;

public class Main {

	public static void executeRegressionTestsWithMetrics(RegressionBaseClass baseClass, String excelPath,
			String regressionId,
			String browserType, String customReportId, TestMetrics metrics, String moduleName) throws Exception {

		// Set specific report title for this regression
		Properties envProps = new Properties();
		try (FileInputStream fis = new FileInputStream("src/main/resources/conf.env")) {
			envProps.load(fis);
			String baseTitle = envProps.getProperty("REPORT_TITLE", "Playwright Test Execution Report");
			CustomHtmlReportGenerator.setReportTitle(baseTitle + " - " + moduleName + " - Regression " + regressionId);
		} catch (IOException e) {
			CustomHtmlReportGenerator.setReportTitle(moduleName + " - Regression " + regressionId);
		}
		CustomHtmlReportGenerator.setCurrentTestType("regression");

		CustomHtmlReportGenerator.initFeature("Login Feature - " + regressionId);

		baseClass.processLogin(excelPath, "Login");

		CustomHtmlReportGenerator.addStepGroup("Login Steps - " + regressionId);

		CustomHtmlReportGenerator.initFeature("Regression Test Suite - " + regressionId);

		baseClass.processNavigationSteps(excelPath, "Navigation");

		baseClass.processExcelSheetForRegression(excelPath, "Regression");

		// Get actual metrics from test execution
		int[] actualMetrics = CustomHtmlReportGenerator.getActualMetrics();
		metrics.steps = actualMetrics[0];
		metrics.stepsPass = actualMetrics[1];
		metrics.stepsFail = actualMetrics[2];
		metrics.testCases = actualMetrics[3];
		metrics.testCasesPass = actualMetrics[4];
		metrics.testCasesFail = actualMetrics[5];

		// Generate report with folder structure:
		// reports/moduleName/RT_ModuleName_Browser_Number.html
		String moduleFolder = moduleName.replaceAll(" ", "");
		String reportName = "RT_" + moduleFolder + "_" + browserType + "_"
				+ String.format("%02d", Integer.parseInt(regressionId.replaceAll("\\D", ""))) + ".html";
		CustomHtmlReportGenerator.generateReport("reports/" + moduleFolder + "/" + reportName);

		// Store regression data for O(1) access
		System.out.println(
				"[MAIN] Storing regression data for: " + regressionId + ", Thread: " + Thread.currentThread().getId());
		CustomHtmlReportGenerator.storeRegressionData(regressionId);

	}

	public static void executeRegressionTests(RegressionBaseClass baseClass, String excelPath, String regressionId,
			String browserType) throws Exception {

		CustomHtmlReportGenerator.initFeature("Login Feature");

		baseClass.processLogin(excelPath, "Login");

		CustomHtmlReportGenerator.addStepGroup("Login Steps");

		CustomHtmlReportGenerator.initFeature("Regression Test Suite" + regressionId);

		baseClass.processNavigationSteps(excelPath, "Navigation");

		baseClass.processExcelSheetForRegression(excelPath, "Regression");

		CustomHtmlReportGenerator.generateReport("reports/regression-report-" + browserType + "-" + regressionId + "-"
				+ Thread.currentThread().getId() + ".html");

	}

	public static void executeValidationTestsWithMetrics(RegressionBaseClass baseClass, String excelPath,
			String browserType, String customReportId, TestMetrics metrics, String moduleName)
			throws Exception {

		// Set specific report title for validation
		Properties envProps = new Properties();
		try (FileInputStream fis = new FileInputStream("src/main/resources/conf.env")) {
			envProps.load(fis);
			String baseTitle = envProps.getProperty("REPORT_TITLE", "Playwright Test Execution Report");
			CustomHtmlReportGenerator.setReportTitle(baseTitle + " - " + moduleName + " - Validation");
		} catch (IOException e) {
			CustomHtmlReportGenerator.setReportTitle(moduleName + " - Validation");
		}
		CustomHtmlReportGenerator.setCurrentTestType("validation");

		CustomHtmlReportGenerator.initFeature("Login Feature - Validation");

		baseClass.processLogin(excelPath, "Login");

		if (baseClass.getPage() == null || baseClass.getPage().isClosed()) {
			System.err.println("Page is null or closed after login, reinitializing...");
			baseClass.initialize(browserType, null, excelPath, "Validation");
		}

		CustomHtmlReportGenerator.initFeature("Validation Test Suite");

		ValidationBaseClass validationBaseClass = new ValidationBaseClass(baseClass);

		baseClass.processNavigationSteps(excelPath, "Navigation");

		validationBaseClass.validateAndFillFields(excelPath, "Validation");

		// Get actual validation metrics
		int[] actualMetrics = CustomHtmlReportGenerator.getActualMetrics();
		metrics.steps = actualMetrics[0];
		metrics.stepsPass = actualMetrics[1];
		metrics.stepsFail = actualMetrics[2];
		metrics.testCases = actualMetrics[3];
		metrics.testCasesPass = actualMetrics[4];
		metrics.testCasesFail = actualMetrics[5];

		// Generate report with folder structure:
		// reports/moduleName/VT_ModuleName_Browser_Number.html
		String moduleFolder = moduleName.replaceAll(" ", "");
		String reportName = "VT_" + moduleFolder + "_" + browserType + "_01.html";
		CustomHtmlReportGenerator.generateReport("reports/" + moduleFolder + "/" + reportName);

		// Store validation data
		CustomHtmlReportGenerator.storeRegressionData("validation");
	}

	public static void executeValidationTests(RegressionBaseClass baseClass, String excelPath, String browserType)
			throws Exception {

		CustomHtmlReportGenerator.initFeature("Login Feature");

		baseClass.processLogin(excelPath, "Login");

		if (baseClass.getPage() == null || baseClass.getPage().isClosed()) {
			System.err.println("Page is null or closed after login, reinitializing...");
			baseClass.initialize(browserType, null, excelPath, "Validation");
		}

		CustomHtmlReportGenerator.initFeature("Validation Test Suite");

		ValidationBaseClass validationBaseClass = new ValidationBaseClass(baseClass);

		baseClass.processNavigationSteps(excelPath, "Navigation");

		validationBaseClass.validateAndFillFields(excelPath, "Validation");

		CustomHtmlReportGenerator.generateReport("reports/validation-report-" + browserType + "-"
				+ System.currentTimeMillis() + ".html");
	}

	public static void executeFunctionalTestsWithMetrics(RegressionBaseClass baseClass, String excelPath,
			String browserType, String customReportId, TestMetrics metrics, String moduleName)
			throws Exception {
		Properties envProps = new Properties();
		try (FileInputStream fis = new FileInputStream("src/main/resources/conf.env")) {
			envProps.load(fis);
			String baseTitle = envProps.getProperty("REPORT_TITLE", "Playwright Test Execution Report");
			// CustomHtmlReportGenerator.setReportTitle(baseTitle + " - " + moduleName + " -
			// Functional");
			CustomHtmlReportGenerator.setReportTitle(baseTitle);
		} catch (IOException e) {
			CustomHtmlReportGenerator.setReportTitle(moduleName + " - Functional");
		}
		CustomHtmlReportGenerator.setCurrentTestType("functional");
		CustomHtmlReportGenerator.initFeature("Login Feature - Functional");
		baseClass.processLogin(excelPath, "Login");

		CustomHtmlReportGenerator.initFeature("Functional Test Suite");
		FunctionalBaseClass functional = new FunctionalBaseClass(baseClass);
		baseClass.processNavigationSteps(excelPath, "Navigation");
		functional.run(excelPath, "Functional");

		int[] actualMetrics = CustomHtmlReportGenerator.getActualMetrics();
		metrics.steps = actualMetrics[0];
		metrics.stepsPass = actualMetrics[1];
		metrics.stepsFail = actualMetrics[2];
		metrics.testCases = actualMetrics[3];
		metrics.testCasesPass = actualMetrics[4];
		metrics.testCasesFail = actualMetrics[5];

		String moduleFolder = moduleName.replaceAll(" ", "");
		String reportName = "FT_" + moduleFolder + "_" + browserType + "_01.html";
		CustomHtmlReportGenerator.generateReport("reports/" + moduleFolder + "/" + reportName);
		CustomHtmlReportGenerator.storeRegressionData("functional");
	}

	// Helper class to return test metrics
	static class TestMetrics {
		int steps = 0, stepsPass = 0, stepsFail = 0;
		int testCases = 0, testCasesPass = 0, testCasesFail = 0;
	}

	public static TestMetrics runTestInstanceWithMetrics(String browserType, String regressionId, String excelPath,
			String testType, String customReportId, String moduleName) {
		TestMetrics metrics = new TestMetrics();
		RegressionBaseClass baseClass = null;

		try {

			// Clear only current thread data for parallel execution
			CustomHtmlReportGenerator.clearThreadData();

			baseClass = new RegressionBaseClass();

			baseClass.initialize(browserType, regressionId, excelPath, testType);

			if ("Regression".equalsIgnoreCase(testType)) {
				executeRegressionTestsWithMetrics(baseClass, excelPath, regressionId, browserType, customReportId,
						metrics, moduleName);
			} else if ("Validation".equalsIgnoreCase(testType)) {
				executeValidationTestsWithMetrics(baseClass, excelPath, browserType, customReportId, metrics,
						moduleName);
			} else if ("Functional".equalsIgnoreCase(testType)) {
				executeFunctionalTestsWithMetrics(baseClass, excelPath, browserType, customReportId, metrics,
						moduleName);
			}

		} catch (Exception e) {
			System.err.println("Error in test instance for " + (regressionId != null ? regressionId : "Validation")
					+ ": " + e.getMessage());
			e.printStackTrace();
			metrics.stepsFail++; // Count as failure
		} finally {
			if (baseClass != null && baseClass.page != null) {
				baseClass.cleanup();
			}
		}
		return metrics;
	}

	public static void runTestInstance(String browserType, String regressionId, String excelPath, String testType) {
		RegressionBaseClass baseClass = null;

		try {
			// Reset report generator for clean report
			CustomHtmlReportGenerator.resetForNewReport();

			String customReportId = "Legacy-" + System.currentTimeMillis();
			runTestInstanceWithMetrics(browserType, regressionId, excelPath, testType, customReportId, "Module");
		} catch (Exception e) {
			System.err.println("Error in test instance for " + (regressionId != null ? regressionId : "Validation")
					+ ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (baseClass != null && baseClass.page != null) {
				baseClass.cleanup();
			}
		}
	}

	// Helper method to get value by key, handling trailing spaces
	private static String getValueByKey(Map<String, String> map, String key) {
		// First try exact match
		String value = map.get(key);
		if (value != null)
			return value.trim();

		// Try with trailing space
		value = map.get(key + " ");
		if (value != null)
			return value.trim();

		// Try finding key that starts with the target key
		for (String mapKey : map.keySet()) {
			if (mapKey.trim().equals(key)) {
				return map.get(mapKey).trim();
			}
		}
		return null;
	}

	private static String sanitizeFilename(String filename) {
		if (filename == null || filename.trim().isEmpty()) {
			return "report.html";
		}
		String sanitized = filename.trim()
				.replaceAll("[\\s]+", "_")
				.replaceAll("[–—]", "-")
				.replaceAll("[^a-zA-Z0-9._-]", "")
				.replaceAll("_{2,}", "_")
				.replaceAll("^_+|_+$", "");
		if (!sanitized.toLowerCase().endsWith(".html")) {
			sanitized += ".html";
		}
		return sanitized;
	}

	public static void main(String[] args) {

		try {

			Properties envProps = new Properties();
			try (FileInputStream fis = new FileInputStream("src/main/resources/conf.env")) {
				envProps.load(fis);

			} catch (IOException e) {
				throw new IllegalStateException("Could not load conf.env file: " + e.getMessage());
			}

			String masterExcelPath = envProps.getProperty("MASTER_EXCEL_PATH");
			if (masterExcelPath == null || masterExcelPath.trim().isEmpty()) {
				throw new IllegalStateException("MASTER_EXCEL_PATH must be specified in the conf.env file.");
			}

			String reportTitle = envProps.getProperty("REPORT_TITLE", "Playwright Test Execution Report");
			CustomHtmlReportGenerator.setReportTitle(reportTitle);

			System.out.println("\n" + "=".repeat(80));
			System.out.println("📂 MASTER EXCEL PATH: " + masterExcelPath);
			System.out.println("=".repeat(80) + "\n");

			Map<String, List<Map<String, String>>> masterData = ExcelUtils.getAllData(masterExcelPath);

			List<Map<String, String>> moduleRows = masterData.get("Modules");

			if (moduleRows == null || moduleRows.isEmpty()) {
				throw new IllegalStateException("Modules sheet is missing or empty in MasterDataSheet.xlsx");
			}

			List<ModuleMetrics> allModuleMetrics = new ArrayList<>();

			for (Map<String, String> moduleRow : moduleRows) {
				// Debug: Print all keys and values

				// Handle column names with potential trailing spaces
				String status = getValueByKey(moduleRow, "Status");
				if (!"Active".equalsIgnoreCase(status)) {

					continue;
				}

				String moduleName = getValueByKey(moduleRow, "Module Name");
				String excelPath = getValueByKey(moduleRow, "Excel Path");

				// Path normalization: If the path is absolute and points to the old project,
				// or if the file doesn't exist, try to find it in the current project's Excel
				// folder.
				if (excelPath != null && !excelPath.trim().isEmpty()) {
					File file = new File(excelPath);
					if (!file.exists() || excelPath.contains("/klasguru/")) {
						String fileName = file.getName();
						String localExcelPath = new File("Excel", fileName).getAbsolutePath();
						if (new File(localExcelPath).exists()) {
							System.out.println("🔄 NORMALIZING PATH: " + excelPath + " ➡️ " + localExcelPath);
							excelPath = localExcelPath;
						}
					}
				}

				String indexFileName = getValueByKey(moduleRow, "Index File Name");
				indexFileName = sanitizeFilename(indexFileName);

				System.out.println("\n" + "=".repeat(80));
				System.out.println("📋 MODULE: " + moduleName);
				System.out.println("📂 MODULE EXCEL PATH: " + excelPath);
				System.out.println("📄 INDEX FILE: " + indexFileName);
				System.out.println("=".repeat(80) + "\n");

				long moduleStartTime = System.currentTimeMillis();

				ModuleMetrics moduleMetrics = new ModuleMetrics(moduleName, indexFileName);
				List<String> regressionReports = new ArrayList<>();
				List<String> functionalReports = new ArrayList<>();
				List<String> validationReports = new ArrayList<>();

				final String finalExcelPath = excelPath;

				Map<String, List<Map<String, String>>> configData = ExcelUtils.getAllData(finalExcelPath);
				List<Map<String, String>> configRows = configData.get("Config");

				if (configRows == null || configRows.isEmpty()) {
					System.err.println("Config sheet missing in: " + finalExcelPath);
					continue;
				}

				CustomHtmlReportGenerator.setModuleInfo(moduleName, indexFileName);

				ExecutorService executor = Executors.newFixedThreadPool(10);
				final int[] totalSteps = { 0 }, stepsPass = { 0 }, stepsFail = { 0 };
				final int[] totalTestCases = { 0 }, testCasesPass = { 0 }, testCasesFail = { 0 };
				int regressionCount = 0;
				int reportCounter = 1;
				int testRunCount = 0; // Count all test runs (Functional, Validation, Regression)

				try {
					for (Map<String, String> config : configRows) {
						// Debug: Print config data

						String testType = getValueByKey(config, "Test Type"); // Use Test Type column
						String browserType = getValueByKey(config, "Browser Type");
						String runMode = getValueByKey(config, "Run mode");

						// Skip empty config rows
						if (testType == null || testType.trim().isEmpty()) {

							continue;
						}

						if ("Regression".equalsIgnoreCase(testType)
								&& ("Parallel".equalsIgnoreCase(runMode) || "Parllel".equalsIgnoreCase(runMode))) {
							String iterationsStr = getValueByKey(config, "No of Iterations");
							int iterations = iterationsStr != null ? (int) Double.parseDouble(iterationsStr) : 1;
							String regressionIdStr = getValueByKey(config, "Regression ID");
							String[] regressionIds = regressionIdStr != null
									? regressionIdStr.split(",")
									: new String[] {};

							regressionCount += iterations;
							testRunCount += iterations;

							for (int i = 0; i < iterations; i++) {
								String regId = regressionIds[i % regressionIds.length].trim();
								String customReportId = moduleName.replaceAll(" ", "") + "-" + reportCounter++;
								String moduleFolder = moduleName.replaceAll(" ", "");
								String reportPath = moduleFolder + "/RT_" + moduleFolder + "_" + browserType + "_"
										+ String.format("%02d", Integer.parseInt(regId.replaceAll("\\D", "")))
										+ ".html";
								regressionReports.add(reportPath);

								executor.submit(() -> {
									TestMetrics testMetrics = runTestInstanceWithMetrics(browserType, regId,
											finalExcelPath,
											testType, customReportId, moduleName);
									synchronized (totalSteps) {
										totalSteps[0] += testMetrics.steps;
										stepsPass[0] += testMetrics.stepsPass;
										stepsFail[0] += testMetrics.stepsFail;
										totalTestCases[0] += testMetrics.testCases;
										testCasesPass[0] += testMetrics.testCasesPass;
										testCasesFail[0] += testMetrics.testCasesFail;
									}
								});
							}
						} else if ("Validation".equalsIgnoreCase(testType)) {
							testRunCount++; // Count validation as 1 test run
							String customReportId = moduleName.replaceAll(" ", "") + "-Val-" + reportCounter++;
							TestMetrics testMetrics = runTestInstanceWithMetrics(browserType, null, finalExcelPath,
									testType,
									customReportId, moduleName);
							totalSteps[0] += testMetrics.steps;
							stepsPass[0] += testMetrics.stepsPass;
							stepsFail[0] += testMetrics.stepsFail;
							totalTestCases[0] += testMetrics.testCases;
							testCasesPass[0] += testMetrics.testCasesPass;
							testCasesFail[0] += testMetrics.testCasesFail;

							String moduleFolder = moduleName.replaceAll(" ", "");
							String valReportPath = moduleFolder + "/VT_" + moduleFolder + "_" + browserType
									+ "_01.html";
							validationReports.add(valReportPath);
						} else if ("Functional".equalsIgnoreCase(testType)) {
							testRunCount++; // Count functional as 1 test run
							String customReportId = moduleName.replaceAll(" ", "") + "-Func-" + reportCounter++;
							TestMetrics testMetrics = runTestInstanceWithMetrics(browserType, null, finalExcelPath,
									testType,
									customReportId, moduleName);
							totalSteps[0] += testMetrics.steps;
							stepsPass[0] += testMetrics.stepsPass;
							stepsFail[0] += testMetrics.stepsFail;
							totalTestCases[0] += testMetrics.testCases;
							testCasesPass[0] += testMetrics.testCasesPass;
							testCasesFail[0] += testMetrics.testCasesFail;

							String moduleFolder = moduleName.replaceAll(" ", "");
							String funcReportPath = moduleFolder + "/FT_" + moduleFolder + "_" + browserType
									+ "_01.html";
							functionalReports.add(funcReportPath);
						}
					}
				} finally {
					executor.shutdown();
					while (!executor.isTerminated()) {
						Thread.sleep(1000);
					}
				}

				long moduleEndTime = System.currentTimeMillis();
				long timeTaken = (moduleEndTime - moduleStartTime) / 60000;

				moduleMetrics.setRegressionCount(testRunCount); // Use total test run count instead of just regressions
				moduleMetrics.setTotalSteps(totalSteps[0]);
				moduleMetrics.setStepsPass(stepsPass[0]);
				moduleMetrics.setStepsFail(stepsFail[0]);
				moduleMetrics.setTotalTestCases(totalTestCases[0]);
				moduleMetrics.setTestCasesPass(testCasesPass[0]);
				moduleMetrics.setTestCasesFail(testCasesFail[0]);
				moduleMetrics.setTimeTakenMinutes(timeTaken);

				Map<String, Integer> metrics = new HashMap<>();
				metrics.put("regressions", testRunCount); // Use total test run count
				metrics.put("totalSteps", totalSteps[0]);
				metrics.put("stepsFailed", stepsFail[0]);
				metrics.put("totalTestCases", totalTestCases[0]);
				metrics.put("testCasesPass", testCasesPass[0]);
				metrics.put("testCasesFailed", testCasesFail[0]);

				// Add per-regression metrics - collect actual regression IDs
				List<String> allRegressionIds = new ArrayList<>();
				for (Map<String, String> config : configRows) {
					String testType = getValueByKey(config, "Test Type");
					String runMode = getValueByKey(config, "Run mode");
					if ("Regression".equalsIgnoreCase(testType)
							&& ("Parallel".equalsIgnoreCase(runMode) || "Parllel".equalsIgnoreCase(runMode))) {
						String iterationsStr = getValueByKey(config, "No of Iterations");
						int iterations = iterationsStr != null ? (int) Double.parseDouble(iterationsStr) : 1;
						String regressionIdStr = getValueByKey(config, "Regression ID");
						String[] regressionIds = regressionIdStr != null ? regressionIdStr.split(",") : new String[] {};
						for (int i = 0; i < iterations; i++) {
							allRegressionIds.add(regressionIds[i % regressionIds.length].trim());
						}
					}
				}

				for (int i = 0; i < regressionReports.size(); i++) {
					String regressionId = i < allRegressionIds.size() ? allRegressionIds.get(i) : "unknown";
					int[] regMetrics = CustomHtmlReportGenerator.getRegressionMetrics(regressionId);
					metrics.put("regression_" + i + "_totalSteps", regMetrics[0]);
					metrics.put("regression_" + i + "_stepsPass", regMetrics[1]);
					metrics.put("regression_" + i + "_stepsFailed", regMetrics[2]);
					metrics.put("regression_" + i + "_totalTestCases", regMetrics[3]);
					metrics.put("regression_" + i + "_testCasesPass", regMetrics[4]);
					metrics.put("regression_" + i + "_testCasesFailed", regMetrics[5]);
				}

				// Add functional test metrics
				if (!functionalReports.isEmpty()) {
					int[] funcMetrics = CustomHtmlReportGenerator.getRegressionMetrics("functional");
					metrics.put("functional_totalSteps", funcMetrics[0]);
					metrics.put("functional_stepsPass", funcMetrics[1]);
					metrics.put("functional_stepsFailed", funcMetrics[2]);
					metrics.put("functional_totalTestCases", funcMetrics[3]);
					metrics.put("functional_testCasesPass", funcMetrics[4]);
					metrics.put("functional_testCasesFailed", funcMetrics[5]);
				}

				// Add validation test metrics
				if (!validationReports.isEmpty()) {
					int[] valMetrics = CustomHtmlReportGenerator.getRegressionMetrics("validation");
					metrics.put("validation_totalSteps", valMetrics[0]);
					metrics.put("validation_stepsPass", valMetrics[1]);
					metrics.put("validation_stepsFailed", valMetrics[2]);
					metrics.put("validation_totalTestCases", valMetrics[3]);
					metrics.put("validation_testCasesPass", valMetrics[4]);
					metrics.put("validation_testCasesFailed", valMetrics[5]);
				}

				String moduleReportPath = "reports/" + indexFileName;

				ModuleReportGenerator.generateModuleReport(moduleName, regressionReports, functionalReports,
						validationReports, metrics, moduleReportPath);

				// Archive all regression data for parent reports
				CustomHtmlReportGenerator.archiveAllRegressions();

				allModuleMetrics.add(moduleMetrics);

			}

			MasterReportGenerator.generateMasterReport(allModuleMetrics, "reports/index.html");

			// Send email with all reports
			sendAllReportsEmail(allModuleMetrics);

		} catch (Exception e) {
			System.err.println("ERROR in main method: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Helper method to extract module name from Excel path
	private static String getModuleNameFromPath(String excelPath) {
		try {
			String fileName = new java.io.File(excelPath).getName();
			return fileName.replace(".xlsx", "").replace("KlasGuru ", "");
		} catch (Exception e) {
			return "Module";
		}
	}

	private static void sendAllReportsEmail(List<ModuleMetrics> allModuleMetrics) {
		try {
			List<String> allReportPaths = new ArrayList<>();

			// Add index.html
			allReportPaths.add("reports/index.html");

			// Add all module reports
			for (ModuleMetrics module : allModuleMetrics) {
				String moduleIndexPath = "reports/" + module.getIndexFileName();
				File moduleIndexFile = new File(moduleIndexPath);
				if (moduleIndexFile.exists()) {
					allReportPaths.add(moduleIndexPath);
				}

				// Add all test reports for this module
				String moduleFolder = module.getModuleName().replaceAll(" ", "");
				File moduleFolderFile = new File("reports/" + moduleFolder);
				if (moduleFolderFile.exists() && moduleFolderFile.isDirectory()) {
					File[] reportFiles = moduleFolderFile.listFiles(
							(dir, name) -> name.endsWith(".html") && !name.equals(module.getIndexFileName()));
					if (reportFiles != null) {
						for (File reportFile : reportFiles) {
							allReportPaths.add("reports/" + moduleFolder + "/" + reportFile.getName());
						}
					}
				}
			}

			Properties envProps = new Properties();
			try (FileInputStream fis = new FileInputStream("src/main/resources/conf.env")) {
				envProps.load(fis);
			} catch (IOException e) {
				System.err.println("Failed to load conf.env: " + e.getMessage());
				return;
			}

			String reportTitle = envProps.getProperty("REPORT_TITLE", "Playwright Test Execution Report");
			String subject = reportTitle + " - All Test Reports";

			// Calculate total metrics from all modules
			int totalSteps = 0, totalStepsPass = 0, totalStepsFail = 0;
			int totalTestCases = 0, totalTestCasesPass = 0, totalTestCasesFail = 0;
			for (ModuleMetrics module : allModuleMetrics) {
				totalSteps += module.getTotalSteps();
				totalStepsPass += module.getStepsPass();
				totalStepsFail += module.getStepsFail();
				totalTestCases += module.getTotalTestCases();
				totalTestCasesPass += module.getTestCasesPass();
				totalTestCasesFail += module.getTestCasesFail();
			}
			int[] totalMetrics = new int[] { totalSteps, totalStepsPass, totalStepsFail, totalTestCases,
					totalTestCasesPass, totalTestCasesFail };

			com.kriyatec.automation.excelutil.EmailReportSender.sendMultipleReports(allReportPaths, subject,
					totalMetrics);

		} catch (Exception e) {
			System.err.println("Failed to send email: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
