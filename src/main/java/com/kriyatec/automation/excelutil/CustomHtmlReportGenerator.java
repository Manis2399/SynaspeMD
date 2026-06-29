package com.kriyatec.automation.excelutil;



import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.kriyatec.automation.models.FeatureGroup;
import com.kriyatec.automation.models.StepsTest;
import com.kriyatec.automation.models.TestStepGroup;
import com.microsoft.playwright.Page;


public class CustomHtmlReportGenerator {

    private static final ThreadLocal<List<StepsTest>> threadLocalStepGroups = ThreadLocal.withInitial(ArrayList::new);
    private static final ConcurrentHashMap<Long, FeatureGroup> allThreadFeatures = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Page> allThreadPages = new ConcurrentHashMap<>();
    private static final List<FeatureGroup> sequentialFeatures = new ArrayList<>();
    private static final List<FeatureGroup> archivedFeatures = new ArrayList<>();
    private static final ConcurrentHashMap<String, List<FeatureGroup>> testLogByRegression = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> featureToRegressionIdMap = new ConcurrentHashMap<>();
    private static boolean isSequentialMode = "sequential".equals(System.getenv("RunMode"));
    private static String currentModuleName = "";
    private static String currentModuleIndexFile = "";
    private static String reportTitle = "Playwright Test Execution Report";
    private static String currentTestType = "regression"; // Track current test type

    
    private static Date totalStartTime;
    private static Date totalEndTime;

    
    public synchronized static void setSequentialMode(boolean sequentialMode) {
        isSequentialMode = sequentialMode;
    }

    public synchronized static void setModuleInfo(String moduleName, String moduleIndexFile) {
        currentModuleName = moduleName;
        currentModuleIndexFile = moduleIndexFile;
    }

    public synchronized static void setReportTitle(String title) {
        reportTitle = title;
    }
    
    public synchronized static void setCurrentTestType(String testType) {
        currentTestType = testType != null ? testType.toLowerCase() : "regression";
    }

    public synchronized static void clearThreadData() {
        long threadId = Thread.currentThread().getId();
        FeatureGroup removed = allThreadFeatures.remove(threadId);
        allThreadPages.remove(threadId);
        threadLocalStepGroups.remove();
        System.out.println("[CLEAR] Thread " + threadId + " data cleared - Had feature: " + (removed != null));
    }

    public synchronized static void resetForNewReport() {
        totalStartTime = null;
        totalEndTime = null;
        clearThreadData();
    }

    public synchronized static int[] getActualMetrics() {
        int totalSteps = 0, totalStepsFailed = 0, totalTestCases = 0, totalFailedTestCases = 0;
        
        List<FeatureGroup> featureGroupList;
        if (isSequentialMode) {
            featureGroupList = sequentialFeatures;
        } else {
            long threadId = Thread.currentThread().getId();
            FeatureGroup currentFeature = allThreadFeatures.get(threadId);
            featureGroupList = currentFeature != null ? List.of(currentFeature) : new ArrayList<>();
            System.out.println("[METRICS] Thread " + threadId + " getting metrics from current thread only - Features: " + featureGroupList.size());
        }
        
        for (FeatureGroup featureGroup : featureGroupList) {
            for (TestStepGroup stepGroup : featureGroup.getStepGroups()) {
                totalSteps++;
                boolean stepPassed = true;
                for (StepsTest testCase : stepGroup.getTestCases()) {
                    totalTestCases++;
                    if (!testCase.isPassed()) {
                        totalFailedTestCases++;
                        stepPassed = false;
                    }
                }
                if (!stepPassed) {
                    totalStepsFailed++;
                }
            }
        }
        int[] result = new int[]{totalSteps, totalSteps - totalStepsFailed, totalStepsFailed, totalTestCases, totalTestCases - totalFailedTestCases, totalFailedTestCases};
        System.out.println("[METRICS] Calculated - Steps: " + totalSteps + ", TestCases: " + totalTestCases + ", Thread: " + Thread.currentThread().getId());
        return result;
    }

    public synchronized static int[] getRegressionMetrics(String regressionId) {
        int totalSteps = 0, totalStepsFailed = 0, totalTestCases = 0, totalFailedTestCases = 0;
        
        List<FeatureGroup> featureGroupList = testLogByRegression.getOrDefault(regressionId, new ArrayList<>());
        
        for (FeatureGroup featureGroup : featureGroupList) {
            for (TestStepGroup stepGroup : featureGroup.getStepGroups()) {
                totalSteps++;
                boolean stepPassed = true;
                for (StepsTest testCase : stepGroup.getTestCases()) {
                    totalTestCases++;
                    if (!testCase.isPassed()) {
                        totalFailedTestCases++;
                        stepPassed = false;
                    }
                }
                if (!stepPassed) {
                    totalStepsFailed++;
                }
            }
        }
        return new int[]{totalSteps, totalSteps - totalStepsFailed, totalStepsFailed, totalTestCases, totalTestCases - totalFailedTestCases, totalFailedTestCases};
    }

    public synchronized static void archiveCurrentThreadData() {
        // Move current thread data to archive for main reports
        long threadId = Thread.currentThread().getId();
        FeatureGroup currentFeature = allThreadFeatures.get(threadId);
        if (currentFeature != null) {
            archivedFeatures.add(currentFeature);
        }
        clearThreadData();
    }

    public synchronized static List<FeatureGroup> getArchivedFeatures() {
        return new ArrayList<>(archivedFeatures);
    }

    public synchronized static void storeRegressionData(String regressionId) {
        long threadId = Thread.currentThread().getId();
        FeatureGroup currentFeature = allThreadFeatures.get(threadId);
        if (currentFeature != null) {
            testLogByRegression.computeIfAbsent(regressionId, k -> new ArrayList<>()).add(currentFeature);
            System.out.println("[TEST_LOG] Stored " + regressionId + " data - Features: " + currentFeature.getStepGroups().size() + " steps");
            System.out.println("[TEST_LOG] Current testLog keys: " + testLogByRegression.keySet());
        }
        clearThreadData();
        System.out.println("[TEST_LOG] Cleared thread " + threadId + " data after storing " + regressionId);
    }

    public synchronized static List<FeatureGroup> getRegressionData(String regressionId) {
        return new ArrayList<>(testLogByRegression.getOrDefault(regressionId, new ArrayList<>()));
    }
    
    public synchronized static void updateRegressionData(String regressionId, List<FeatureGroup> features) {
        testLogByRegression.put(regressionId, new ArrayList<>(features));
    }

    public synchronized static void archiveAllRegressions() {
        System.out.println("[ARCHIVE] Archiving regressions: " + testLogByRegression.keySet());
        int totalFeatures = 0;
        for (List<FeatureGroup> features : testLogByRegression.values()) {
            totalFeatures += features.size();
            archivedFeatures.addAll(features);
        }
        System.out.println("[ARCHIVE] Moved " + totalFeatures + " features to archive");
        testLogByRegression.clear();
        System.out.println("[ARCHIVE] Cleared testLog, archived features count: " + archivedFeatures.size());
    }

    public synchronized static void initFeature(String featureName) {
    	
    	 if (totalStartTime == null) {
             totalStartTime = new Date(); // Set the start time when the first feature is initialized
         }
    	
        if (isSequentialMode) {
            sequentialFeatures.add(new FeatureGroup(featureName));
        } else {
            long getId = Thread.currentThread().getId();
            allThreadFeatures.put(getId, new FeatureGroup(featureName));
        }
    }

    
    public synchronized static void initPage(Page playwrightPage) { 
    	long getId = Thread.currentThread().getId(); // ✅ Use getId() instead of getId()
    	allThreadPages.put(getId, playwrightPage); 
    }
    
    

    public synchronized static void addStepGroup(String stepName) {
        FeatureGroup featureGroup;
        if (isSequentialMode) {
            featureGroup = sequentialFeatures.get(sequentialFeatures.size() - 1);
        } else {
            long getId = Thread.currentThread().getId();
            featureGroup = allThreadFeatures.get(getId);
        }
        if (featureGroup != null) {
            boolean stepExists = featureGroup.getStepGroups().stream()
                    .anyMatch(stepGroup -> stepGroup.getStepName().equals(stepName));
            if (!stepExists) {
                featureGroup.getStepGroups().add(new TestStepGroup(stepName, threadLocalStepGroups.get()));
                threadLocalStepGroups.remove();
            }
        }
    }
    
//    public synchronized static void addStepTest(String message, Boolean passed, String details) {
//        StepsTest test = new StepsTest(message, passed, details);
//        test.setTimestamp(new Date().toString());
//        if (!passed) {
//            long getId = Thread.currentThread().getId();
//            Page page = allThreadPages.get(getId);
//            if (page != null) {
//                byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
//                String base64Screenshot = Base64.getEncoder().encodeToString(screenshotBytes);
//                test.setScreenshot(base64Screenshot);
//            }
//        }
//        threadLocalStepGroups.get().add(test);
//    }

    
    public synchronized static void addStepTest(String message, Boolean passed, String details, String inputValue, String scenario) {
        StepsTest test = new StepsTest(message, passed, details, inputValue, scenario);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        String startTime = formatter.format(new Date());
        test.setStartTime(startTime);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String endTime = formatter.format(new Date());
        test.setEndTime(endTime);
        test.setTimestamp(new Date().toString());
        
        if (!passed) {
            long getId = Thread.currentThread().getId();
            Page page = allThreadPages.get(getId);
            if (page != null) {
                try {
                    String screenshotPath = captureScreenshotToFile(page, message);
                    test.setScreenshot(screenshotPath);
                } catch (Exception e) {
                    System.err.println("[ERROR] Screenshot capture failed: " + e.getMessage());
                }
            }
        }
        threadLocalStepGroups.get().add(test);
    }
    
    public synchronized static void addStepTestWithVerification(String message, Boolean passed, String details, String inputValue, String scenario, String verification) {
        StepsTest test = new StepsTest(message, passed, details, inputValue, scenario);
        test.setVerification(verification);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        String startTime = formatter.format(new Date());
        test.setStartTime(startTime);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String endTime = formatter.format(new Date());
        test.setEndTime(endTime);
        test.setTimestamp(new Date().toString());
        
        if (!passed) {
            long getId = Thread.currentThread().getId();
            Page page = allThreadPages.get(getId);
            if (page != null) {
                try {
                    String screenshotPath = captureScreenshotToFile(page, message);
                    test.setScreenshot(screenshotPath);
                } catch (Exception e) {
                    System.err.println("[ERROR] Screenshot capture failed: " + e.getMessage());
                }
            }
        }
        threadLocalStepGroups.get().add(test);
    }
    
    private static String captureScreenshotToFile(Page page, String testName) {
        try {
            String testType = getCurrentTestType();
            String folderPath = "reports/screenshot/" + currentModuleName + "/" + testType;
            java.nio.file.Path directory = java.nio.file.Paths.get(folderPath);
            java.nio.file.Files.createDirectories(directory);
            
            String fileName = testName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ".png";
            String filePath = folderPath + "/" + fileName;
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get(filePath)).setFullPage(true));
            
            return "../screenshot/" + currentModuleName + "/" + testType + "/" + fileName;
        } catch (Exception e) {
            System.err.println("Error capturing screenshot: " + e.getMessage());
            return null;
        }
    }
    
    private static String getCurrentTestType() {
        // Return the explicitly set test type
        if (currentTestType != null && !currentTestType.isEmpty()) {
            return currentTestType;
        }
        
        // Fallback to stack trace detection
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean foundFunctional = false;
        boolean foundValidation = false;
        boolean foundRegression = false;
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName().toLowerCase();
            if (className.contains("functionalbaseclass") || className.contains("functional")) {
                foundFunctional = true;
            }
            if (className.contains("validationbaseclass") || className.contains("validation")) {
                foundValidation = true;
            }
            if (className.contains("regressionbaseclass") || className.contains("regression")) {
                foundRegression = true;
            }
        }
        
        if (foundFunctional) return "functional";
        if (foundValidation) return "validation";
        if (foundRegression) return "regression";
        return "regression";
    }
    


    public synchronized static void initStepTests() {
        threadLocalStepGroups.set(new ArrayList<>());
    }
    
    private synchronized static void generateTestDataJSFile(List<FeatureGroup> featureGroupList) {
        generateTestDataJSFile(featureGroupList, null);
    }
    
    private synchronized static void generateTestDataJSFile(List<FeatureGroup> featureGroupList, String regressionId) {
        try {
            String reportKey = currentModuleName + "_" + getCurrentTestType();
            File file = new File("reports/testData");
            
            StringBuilder existingContent = new StringBuilder();
            boolean fileExists = file.exists();
            
            if (fileExists) {
                existingContent.append(new String(Files.readAllBytes(file.toPath())));
            } else {
                file.getParentFile().mkdirs();
                existingContent.append("window.testDataStore = window.testDataStore || {};\n");
                existingContent.append("window.getTestData = function(key) { return window.testDataStore[key]; };\n\n");
            }
            
            String keyPattern = "window.testDataStore['" + reportKey + "']";
            int startIndex = existingContent.indexOf(keyPattern);
            
            if (startIndex != -1) {
                int endIndex = existingContent.indexOf("};\n", startIndex) + 3;
                existingContent.delete(startIndex, endIndex);
            }
            
            List<FeatureGroup> allFeatures = new ArrayList<>(featureGroupList);
            
            StringBuilder jsBuilder = new StringBuilder();
            
            jsBuilder.append("window.testDataStore['").append(reportKey).append("'] = {\n");
            jsBuilder.append("  features: [\n");
            
            for (int i = 0; i < allFeatures.size(); i++) {
                FeatureGroup featureGroup = allFeatures.get(i);
                String featureKey = featureGroup.getFeatureName() + "_" + System.identityHashCode(featureGroup);
                String regId = featureToRegressionIdMap.getOrDefault(featureKey, "regression");
                jsBuilder.append("    {featureName: '").append(escapeJs(featureGroup.getFeatureName()))
                        .append("', regressionId: '").append(regId).append("', stepGroups: [\n");
                
                for (int j = 0; j < featureGroup.getStepGroups().size(); j++) {
                    TestStepGroup stepGroup = featureGroup.getStepGroups().get(j);
                    jsBuilder.append("      {stepName: '").append(escapeJs(stepGroup.getStepName()))
                            .append("', id: '").append(stepGroup.getId()).append("', testCases: [\n");
                    
                    for (int k = 0; k < stepGroup.getTestCases().size(); k++) {
                        StepsTest testCase = stepGroup.getTestCases().get(k);
                        String inputVal = testCase.getInputValue();
                        String verification = testCase.getVerification();
                        String scenario = testCase.getScenario();
                        
                        // Move input to verification for count/text capture scenarios
                        if (testCase.getName() != null && (testCase.getName().toUpperCase().contains("COUNT CAPTURE") || testCase.getName().toUpperCase().contains("TEXT CAPTURE")) && 
                            inputVal != null && !inputVal.trim().isEmpty()) {
                            if (testCase.getName().toUpperCase().contains("COUNT")) {
                                verification = "Captured Count: " + inputVal;
                            } else {
                                verification = "Captured Text: " + inputVal;
                            }
                            inputVal = null;
                        }
                        
                        if (inputVal != null && (inputVal.trim().isEmpty() || inputVal.startsWith("iVBORw0KGgo") || inputVal.startsWith("/9j/") || inputVal.startsWith("R0lGODlh") || inputVal.length() > 100)) inputVal = null;
                        jsBuilder.append("        {name: '").append(escapeJs(testCase.getName()))
                                .append("', passed: ").append(testCase.isPassed())
                                .append(", scenario: '").append(escapeJs(scenario != null ? scenario : "-"))
                                .append("', inputValue: '").append(escapeJs(inputVal != null ? inputVal : "-"))
                                .append("', verification: '").append(escapeJs(verification != null ? verification : "-"))
                                .append("', startTime: '").append(testCase.getStartTime() != null ? testCase.getStartTime() : "-")
                                .append("', endTime: '").append(testCase.getEndTime() != null ? testCase.getEndTime() : "-")
                                .append("', screenshot: ").append(testCase.getScreenshot() != null ? "'" + testCase.getScreenshot() + "'" : "null")
                                .append("}" + (k < stepGroup.getTestCases().size() - 1 ? "," : "") + "\n");
                    }
                    jsBuilder.append("      ]}" + (j < featureGroup.getStepGroups().size() - 1 ? "," : "") + "\n");
                }
                jsBuilder.append("    ]}" + (i < allFeatures.size() - 1 ? "," : "") + "\n");
            }
            jsBuilder.append("  ]\n};\n\n");
            
            existingContent.append(jsBuilder.toString());
            
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write(existingContent.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    /**
     * Creates URL-safe aliases for files with problematic characters
     * This method creates symbolic links or copies with sanitized names
     */
    public static void createUrlSafeAlias(String originalPath) {
        try {
            File originalFile = new File(originalPath);
            if (!originalFile.exists()) {
                return;
            }
            
            String originalName = originalFile.getName();
            String sanitizedName = sanitizeFilename(originalName);
            
            // Only create alias if names are different
            if (!originalName.equals(sanitizedName)) {
                File aliasFile = new File(originalFile.getParent(), sanitizedName);
                
                // Create symbolic link or copy file
                try {
                    java.nio.file.Files.createSymbolicLink(
                        aliasFile.toPath(), 
                        originalFile.toPath()
                    );
                    System.out.println("[URL_SAFE] Created symbolic link: " + sanitizedName + " -> " + originalName);
                } catch (Exception e) {
                    // Fallback to copying file if symbolic link fails
                    java.nio.file.Files.copy(
                        originalFile.toPath(), 
                        aliasFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    System.out.println("[URL_SAFE] Created copy: " + sanitizedName + " (original: " + originalName + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to create URL-safe alias: " + e.getMessage());
        }
    }
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "report.html";
        }
        
        // Remove or replace problematic characters
        String sanitized = filename
            .trim()
            .replaceAll("[\\s]+", "_")           // Replace spaces with underscores
            .replaceAll("[–—−‒]", "-")                   // Replace all dash variants with regular hyphen
            .replaceAll("[^a-zA-Z0-9._-]", "")   // Remove other special characters
            .replaceAll("_{2,}", "_")            // Replace multiple underscores with single
            .replaceAll("^_+|_+$", "");          // Remove leading/trailing underscores
        
        // Ensure .html extension
        if (!sanitized.toLowerCase().endsWith(".html")) {
            sanitized += ".html";
        }
        
        return sanitized;
    }

	public synchronized static void generateReport(String reportPath) {
		try {
			totalEndTime = new Date(); 
			// Sanitize the report path
			File originalFile = new File(reportPath);
			String sanitizedFilename = sanitizeFilename(originalFile.getName());
			String sanitizedPath = originalFile.getParent() + File.separator + sanitizedFilename;
			
			File file = new File(sanitizedPath);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
	        // Format the date
//	        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	        String formattedStartTime = dateFormat.format(totalStartTime);
//	        String formattedEndTime = dateFormat.format(totalEndTime);
			
//			int totalFeatures = 0;
			int totalSteps = 0;
			int totalStepsFailed = 0;
			int totalPassedTestCases = 0;
			int totalTestCases = 0;
			int totalFailedTestCases = 0;
			String bootstrapcss = new String(Files.readAllBytes(Paths.get("src/main/resources/css/bootstrap.txt")));
			String bootstrapicons = new String(Files.readAllBytes(Paths.get("src/main/resources/css/bootstrap-icons.txt")));
			String bootstrapscript = new String(Files.readAllBytes(Paths.get("src/main/resources/script/bootstrap.txt")));
			StringBuilder htmlBuilder = new StringBuilder();
			htmlBuilder.append("<html><head><title>").append(reportTitle).append("</title>").append("<meta charset='UTF-8'>")
					.append("<meta name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no'>")
					.append("<style>")
					.append("body {font-family: Arial, sans-serif; background-color: #f4f7fc; padding: 20px;} ")
					.append(".badge {font-size: 12px; padding: 5px; border-radius: 12px; display: inline-block;} ")
					.append(".card.clickable {cursor: pointer;} ")
					.append(".screenshot-cam{font-size: 1.2rem; cursor: pointer;}")
					.append(".feature-breakup{position: absolute; right: 199px; }")
					.append(".test-info{width:100%;padding: 10px;} ")
					.append(".card-title{width:100%;} ")
					.append("td{word-break: break-word;max-width: 200px;} ")
					.append(".table-responsive {overflow-x: auto;} ")
					.append(".table {min-width: 800px;} ")
					.append(".table td:nth-last-child(-n+2), .table th:nth-last-child(-n+2) {text-align: center;vertical-align: middle;} ")
					.append(".badge-success {background-color: #28a745; color: white;} ")
					.append(".badge-danger {background-color: #dc3545; color: white;} ")
					.append(".bi-camera {cursor: pointer;font-size: 1.2rem;} ")
					.append(".header-icon-right {right: 100px;position: absolute;}")
					.append(".badge-default {background-color: #6c757d; color: white;} ")
					.append(".card-body {display: flex;min-height:300px;flex-wrap: wrap;} ")
					.append(".steps{padding: 10px;border-bottom: 1px solid #e9eaec;cursor:pointer;}")
					.append(".left-panel {border-right: 1px solid #e9eaec;min-width: 300px;flex: 0 0 35%;max-height: 600px;overflow-y: auto;} ")
					.append(".right-panel {flex: 1;overflow-x: auto;} ")
					.append("@media (max-width: 768px) { .card-body {flex-direction: column;} .left-panel {border-right: none;border-bottom: 1px solid #e9eaec;flex: 1;} }")
					.append(".collapsible {background-color: #f9f9f9; cursor: pointer; padding: 10px; border: 1px solid #ccc; border-radius: 5px; margin-top: 5px;} ")
					.append(".content {display: none; padding: 0 10px; margin-top: 5px; border-left: 3px solid #ccc;} ")
					.append(".header {font-size: 24px; color: #333; margin-bottom: 10px; font-weight: bold; margin-top: 10px;} ")
					.append(".summary {background-color: #fff; padding: 10px; border-radius: 5px; border: 1px solid #ccc; margin-top: 20px;} ")
					.append(".step-summary {font-size: 16px; margin-top: 10px;} ")
					.append(".card {background-color: #fff; border-radius: 8px; padding: 15px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);margin-bottom: 10px;} ")
					.append(".card-header {font-size: 18px; font-weight: bold; color: #333; padding: 10px 0;} ")
					.append(".drawer {position: fixed; top: 0; right: -85vw; width: 85vw; height: 100vh; background: #fff; box-shadow: -4px 0 8px rgba(0,0,0,0.3); transition: right 0.3s ease; z-index: 1000; overflow-y: auto;} ")
					.append("@media (max-width: 768px) { .drawer {width: 90vw; right: -90vw;} .offcanvas-end {width: 90vw !important;} }")
					.append(".drawer.open {right: 0;} ")
					.append(".drawer-header {padding: 20px; border-bottom: 2px solid #007bff; background: #f8f9fa;} ")
					.append(".drawer-title {margin: 0; color: #007bff; font-size: 18px;} ")
					.append(".drawer-close {float: right; background: none; border: none; font-size: 24px; cursor: pointer;} ")
					.append(".drawer-content {padding: 20px;} ")
					.append(".drawer-overlay {position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.5); z-index: 999; display: none;} ")
					.append(".drawer-overlay.show {display: block;} ")
					.append("</style>").append("<script>")
					.append("document.addEventListener('DOMContentLoaded', function() {")
					.append(" window.showImageFromPath = function(imagePath) {\r\n"
							+ "    const imgElement = document.getElementById(\"base64Image\");\r\n"
							+ "    imgElement.src = imagePath;\r\n"
							+ "    const modal = new bootstrap.Modal(document.getElementById('imageModal'));\r\n"
							+ "    modal.show();\r\n" + "  };")
					.append(" window.showImage = function(base64ImageString) {\r\n"
							+ "    const imgElement = document.getElementById(\"base64Image\");\r\n"
							+ "    imgElement.src = base64ImageString;\r\n"
							+ "    const modal = new bootstrap.Modal(document.getElementById('imageModal'));\r\n"
							+ "    modal.show();\r\n" + "  };")
					.append("  window.showStep = function(stepId) { "
							+ "        var selectedElement = document.getElementById(stepId); "
							+ "        selectedElement.parentElement.parentElement.querySelectorAll('.test-info').forEach(function(element) { "
							+ "            element.style.display = 'none'; " + "        }); "
							+ "        if (selectedElement) { "
							+ "            selectedElement.style.display = 'block'; " + "        } " + "    };"
							+ "    var parentContainers = document.querySelectorAll('.left-panel');"
							+ "    parentContainers.forEach(function(parent) { "
							+ "      var firstStep = parent.querySelector('.steps');" + "        if (firstStep) { "
							+ "            firstStep.click() " + "        }" + "    });" + "});")
					.append("</script>")
					.append("<style>"+bootstrapcss+"</style>")
					.append("<style>"+bootstrapicons+"</style>")
					.append("<style>.breadcrumb {background-color: #fff; padding: 10px; border-radius: 5px; margin-bottom: 20px; margin-top: 0px;} .breadcrumb-item + .breadcrumb-item::before {content: '>';padding-right: 0.5rem;padding-left: 0.5rem;color: #6c757d;}</style>")
					.append("<script src='../../testData'></script>")
					.append("<script>window.currentReportKey = '" + currentModuleName + "_" + getCurrentTestType() + "'; window.testData = window.getTestData(window.currentReportKey) || {features:[]};</script>")
					.append("</head><body>")
					.append("<nav aria-label='breadcrumb'><ol class='breadcrumb'>")
					.append("<li class='breadcrumb-item'><a href='../index.html'>Home</a></li>")
					.append("<li class='breadcrumb-item'><a href='../").append(currentModuleIndexFile).append("'>").append(currentModuleName).append("</a></li>")
					.append("<li class='breadcrumb-item active' aria-current='page'>").append(getCurrentTestType().substring(0, 1).toUpperCase() + getCurrentTestType().substring(1)).append(" Report</li>")
					.append("</ol></nav>")
					.append("<div class='header'>").append(reportTitle).append("</div>")
					.append("<div style='font-size: 20px; font-weight: 600; color: #555; margin-bottom: 20px;'>").append(currentModuleName)
					.append(" - ").append(getCurrentTestType().substring(0, 1).toUpperCase() + getCurrentTestType().substring(1)).append("</div>")
			.append("<div style='position: absolute; top: 20px; right: 20px; font-size: 14px;'>")
            .append("<div><b>Test Start Time:</b> " + totalStartTime + "</div>")
            .append("<div><b>Test End Time:</b> " + totalEndTime + "</div>")
            .append("</div>");

			List<FeatureGroup> featureGroupList;
			if (isSequentialMode) {
				featureGroupList = sequentialFeatures;
			} else {
				// Use only current thread's features for leaf node report
				long threadId = Thread.currentThread().getId();
				FeatureGroup currentFeature = allThreadFeatures.get(threadId);
				featureGroupList = currentFeature != null ? List.of(currentFeature) : new ArrayList<>();
				System.out.println("[REPORT] Thread " + threadId + " generating report with " + featureGroupList.size() + " features");
			}
			for (FeatureGroup featureGroup : featureGroupList) {
//				totalFeatures++;
				for (TestStepGroup stepGroup : featureGroup.getStepGroups()) {
					totalSteps++;
					boolean stepPassed = true;
					for (StepsTest testCase : stepGroup.getTestCases()) {
						totalTestCases++;
						if (!testCase.isPassed()) {
							totalFailedTestCases++;
							stepPassed = false;
						} else {
							totalPassedTestCases++;
						}
					}
					if (!stepPassed) {
						totalStepsFailed++;
					}
				}
			}
			// Generating the summary section at the top
			htmlBuilder.append("<div class='summary'>").append("<h3>Summary</h3>");
			htmlBuilder.append("<div class='row text-center mt-4'>");

			// Card: Total Steps/Test Cases
			htmlBuilder.append("<div class='col-lg-3 col-md-6 col-sm-12'>");
			htmlBuilder.append("<div class='card clickable border-primary mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterDrawerData(\"all\")'>");
			htmlBuilder.append("<h5 class='card-title'>Total Steps/Test Cases</h5>");
			htmlBuilder.append("<h1 class='card-text text-primary'>" + totalSteps + " / " + totalTestCases + "</h1>");
			htmlBuilder.append("</div></div>");

			// Card: Steps Pass/Fail
			htmlBuilder.append("<div class='col-lg-3 col-md-6 col-sm-12'>");
			htmlBuilder.append("<div class='card clickable border-success mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterDrawerData(\"passed\")'>");
			htmlBuilder.append("<h5 class='card-title'>Steps Pass / Fail</h5>");
			htmlBuilder.append("<h1 class='card-text text-success'>" + (totalSteps - totalStepsFailed) + " / <span class='text-danger'>" + totalStepsFailed + "</span></h1>");
			htmlBuilder.append("</div></div>");

			// Card: Test Cases Pass/Fail
			htmlBuilder.append("<div class='col-lg-3 col-md-6 col-sm-12'>");
			htmlBuilder.append("<div class='card clickable border-success mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterDrawerData(\"passed\")'>");
			htmlBuilder.append("<h5 class='card-title'>Test Cases Pass / Fail</h5>");
			htmlBuilder.append("<h1 class='card-text text-success'>" + totalPassedTestCases + " / <span class='text-danger'>" + totalFailedTestCases + "</span></h1>");
			htmlBuilder.append("</div></div>");

			htmlBuilder.append("</div>"); // End of summary row
			htmlBuilder.append("</div>"); // End of summary

			htmlBuilder.append("<div class='accordion'>");
			for (int i = 0; i < featureGroupList.size(); i++) {
				FeatureGroup featureGroup = featureGroupList.get(i);
				boolean featurePassed = true;
				Integer stepsPassed = 0;
				Integer testsPassed = 0;
				Integer totalTests = 0;
				Integer totalStepsInFeature = featureGroup.getStepGroups().size();
				for (TestStepGroup stepGroup : featureGroup.getStepGroups()) {
					totalTests = totalTests + stepGroup.getTestCases().size();
					boolean stepPassed = true;
					for (StepsTest testCase : stepGroup.getTestCases()) {
						if (!testCase.isPassed()) {
							featurePassed = false;
							stepPassed = false;
						} else {
							testsPassed++;
						}
					}
					if (stepPassed) {
						stepsPassed++;
					}
				}
				htmlBuilder
						// card start
						.append("<div class=\"accordion-item\">").append("<h2 class=\"accordion-header\">")
						.append("<button class=\"accordion-button collapsed\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#collapse"
								+ i + "\" aria-expanded=\"false\" aria-controls=\"collapse" + i + "\">")
						.append("<span><b>Feature : </b>").append(featureGroup.getFeatureName()).append("</span>")
						.append("<div class='feature-breakup'>").append("<span><b>Steps Passed : </b>")
						.append(stepsPassed).append("</span>").append(" / ").append(totalStepsInFeature)
						.append("<b> | </b>").append("<span><b>Tests Passed : </b>").append(testsPassed).append(" / ")
						.append(totalTests).append("</span>").append("</div>")
						.append(featurePassed
								? "<div class='header-icon-right' style='color:green;'><i class=\"bi bi-patch-check-fill\" ></i> Passed</span></div> "
								: "<div class='header-icon-right' style='color:red;font-weight: 500;'><i class=\"bi bi-x-circle screenshot-cam\" > Failed</i></span></div> ")
						.append("</button></h2>")
						.append("<div id='collapse" + i
								+ "' class=\"accordion-collapse collapse hide\" data-bs-parent=\"#accordionExample\">")
						.append("<div class=\"accordion-body\">")
						// card body start
						.append("<div class='card-body'>");
				htmlBuilder
						// left panel start
						.append("<div class='left-panel'>");
				List<TestStepGroup> stepGroupList = new ArrayList<>(featureGroup.getStepGroups());
				for (int j = 0; j < stepGroupList.size(); j++) {
					TestStepGroup stepGroup = stepGroupList.get(j);
					boolean allTestPassed = true;
					for (StepsTest testCase : stepGroup.getTestCases()) {
						if (!testCase.isPassed()) {
							allTestPassed = false;
						}
					}

					String id = stepGroup.getId();
					Integer stepIdx = j + 1;
					htmlBuilder
							.append("<div class='steps' onclick=showStep('"
									+ id + "');><b>Step " + stepIdx + " : </b> ")
							.append("<small>" + stepGroup.getStepName() + "</small>")
							.append(allTestPassed
									? "<div class='float-end' style='color:green;'><i class=\"bi bi-patch-check-fill\" ></i></span></div> "
									: "<div class='float-end' style='color:red;font-weight: 500;'><i class=\"bi bi-x-circle\" ></i></span></div> ");
					htmlBuilder.append("</div>");
				}
				// left panel end
				htmlBuilder.append("</div>");

				htmlBuilder.append("<div class='right-panel'>");
				for (int j = 0; j < stepGroupList.size(); j++) {
					TestStepGroup stepGroup = stepGroupList.get(j);
					String id = stepGroup.getId();
					htmlBuilder.append("<div class ='test-info' id='" + id + "'>");
					htmlBuilder.append("<table class=\"table\">" + "<thead>" + "<tr>"
					        + "<th scope='col'>#</th>"
					        + "<th scope='col'>Test Case</th>"
					        + "<th scope='col'>Scenario Type</th>"
					        + "<th scope='col'style='min-width: 7vw;'>Input Value</th>"
					        + "<th scope='col' style='min-width: 10vw;'>Verification</th>"
					        + "<th scope='col' style='min-width: 7vw;'>Start Time</th>"
					        + "<th scope='col' style='min-width: 7vw;'>End Time</th>"
							+ "<th scope='col' >Result</th>"
					        + "<th scope='col'>Screenshot</th>" 
					        + "</tr>" + "</thead><tbody class='table-group-divider'>");
					List<StepsTest> testCases = new ArrayList<>(stepGroup.getTestCases());
//					if (testCases.size() == 0) {
//						htmlBuilder.append("<tr>").append("<th scope=\"row\"></th>").append("<td>No Records!</td>")
//								.append("<td></td>").append("<td></td>").append("</tr>");
//						continue;
//					}
					// In the HTML table row generation:
					for (int k = 0; k < testCases.size(); k++) {
					    StepsTest testCase = testCases.get(k);
					    Integer rowId = k + 1;
					    String inputVal = testCase.getInputValue();
					    String verification = testCase.getVerification();
					    String scenario = testCase.getScenario();
					    
					    // Move input to verification for count/text capture scenarios
					    if (testCase.getName() != null && (testCase.getName().toUpperCase().contains("COUNT CAPTURE") || testCase.getName().toUpperCase().contains("TEXT CAPTURE")) && 
					        inputVal != null && !inputVal.trim().isEmpty()) {
					        if (testCase.getName().toUpperCase().contains("COUNT")) {
					            verification = "Captured Count: " + inputVal;
					        } else {
					            verification = "Captured Text: " + inputVal;
					        }
					        inputVal = null;
					    }
					    
					    if (inputVal != null && (inputVal.trim().isEmpty() || inputVal.startsWith("iVBORw0KGgo") || inputVal.startsWith("/9j/") || inputVal.startsWith("R0lGODlh") || inputVal.length() > 100)) inputVal = null;
					    htmlBuilder.append("<tr>")
					        .append("<th scope=\"row\">" + rowId + "</th>")
					        .append("<td>" + testCase.getName() + "</td>")
					        .append("<td>" + (scenario != null ? scenario : "-") + "</td>")
					        .append("<td>" + (inputVal != null ? inputVal : "-") + "</td>")
					        .append("<td>" + (verification != null ? verification : "-") + "</td>")
					        .append("<td>" + (testCase.getStartTime() != null ? testCase.getStartTime() : "-") + "</td>")
					        .append("<td>" + (testCase.getEndTime() != null ? testCase.getEndTime() : "-") + "</td>")
					        .append("<td>")
					        .append(testCase.isPassed() 
					            ? "<span class='badge badge-success'> Passed </span>"
					            : "<span class='badge badge-danger'> Failed </span>")
					        .append("</td>")
					        .append("<td>" + (!testCase.isPassed() && testCase.getScreenshot() != null
					            ? "<i class=\"bi bi-camera\" onclick=\"showImageFromPath('" + testCase.getScreenshot() + "')\"></i>"
					            : "-") + "</td>")
					        .append("</tr>");
					}
					htmlBuilder.append("</tbody></table></div>");
				}
				htmlBuilder.append("</div></div></div></div></div>");
			}
			// accordion end
			htmlBuilder.append("</div>");
			


			htmlBuilder.append(
					"<div class=\"modal modal-lg fade\" id=\"imageModal\" tabindex=\"-1\" aria-labelledby=\"imageModalLabel\" aria-hidden=\"true\">\r\n"
							+ "  <div class=\"modal-dialog modal-dialog-centered\">\r\n"
							+ "    <div class=\"modal-content\">\r\n" + "      <div class=\"modal-header\">\r\n"
							+ "        <h5 class=\"modal-title\" id=\"imageModalLabel\">Testcase Screenshot</h5>\r\n"
							+ "        <button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"modal\" aria-label=\"Close\"><span aria-hidden='true' style='font-size:2rem;line-height:1;'>&times;</span></button>\r\n"
							+ "      </div>\r\n" + "      <div class=\"modal-body text-center\">\r\n"
							+ "        <!-- Image placeholder -->\r\n"
							+ "        <img id=\"base64Image\" src=\"\" alt=\"Base64\" class=\"img-fluid\">\r\n"
							+ "      </div>\r\n" + "      <div class=\"modal-footer\">\r\n"
							+ "        <button type=\"button\" class=\"btn btn-secondary\" data-bs-dismiss=\"modal\">Close</button>\r\n"
							+ "      </div>\r\n" + "    </div>\r\n" + "  </div>\r\n" + "</div>");
			// Add Bootstrap Offcanvas Drawer
			htmlBuilder.append("<div class='offcanvas offcanvas-end' tabindex='-1' id='detailsDrawer' aria-labelledby='detailsDrawerLabel' style='width:100vw;'>")
					.append("<div class='offcanvas-header'>")
					.append("<h5 class='offcanvas-title' id='detailsDrawerLabel'>" + currentModuleName + " - Details Summary</h5>")
					.append("<button type='button' class='btn-close' data-bs-dismiss='offcanvas' aria-label='Close'><span aria-hidden='true' style='font-size:2rem;line-height:1;'>&times;</span></button>")
					.append("</div>")
					.append("<div class='offcanvas-body'>")
					.append("<div style='margin-bottom:15px;'><strong>Module:</strong> <span id='drawerModule'>" + currentModuleName + "</span> &nbsp;&nbsp;&nbsp; <strong>Test ID:</strong> <span id='drawerRegression'></span></div>")
					.append("<div class='mb-3'>")
					.append("<button class='btn btn-outline-primary btn-sm me-2' onclick='filterDrawerData(\"all\")'>All</button>")
					.append("<button class='btn btn-outline-success btn-sm me-2' onclick='filterDrawerData(\"passed\")'>Passed</button>")
					.append("<button class='btn btn-outline-danger btn-sm' onclick='filterDrawerData(\"failed\")'>Failed</button>")
					.append("</div>")
					.append("<div class='table-responsive' style='max-height: calc(100vh - 250px); overflow-y: auto;'>")
					.append("<table class='table table-sm table-bordered'>")
					.append("<thead style='position: sticky; top: 0; background-color: white; z-index: 10;'><tr>")
					.append("<th style='width:5%'>S.No</th><th style='width:25%'>Test Case</th><th style='width:15%'>Test Data</th><th style='width:20%'>Expected Result</th><th style='width:20%'>Actual Result</th><th style='width:10%'>Status</th><th style='width:5%'>Screenshot</th>")
					.append("</tr></thead>")
					.append("<tbody id='drawerTableBody'>");
			
			htmlBuilder.append("</tbody></table></div></div></div>");
			
			// Extract regression ID from report path
			String regressionDisplayId = new File(reportPath).getName().replace(".html", "");
			// Use sanitized filename for links but original name for display
			String sanitizedDisplayId = new File(sanitizedPath).getName().replace(".html", "");
			
			// Embed current regression data directly in the report
			htmlBuilder.append("<script>")
					.append("window.currentReportData = {regressionId: '").append(sanitizedDisplayId).append("', features: [");
			
			for (int i = 0; i < featureGroupList.size(); i++) {
				FeatureGroup fg = featureGroupList.get(i);
				htmlBuilder.append("{featureName: '").append(escapeJs(fg.getFeatureName())).append("', stepGroups: [");
				for (int j = 0; j < fg.getStepGroups().size(); j++) {
					TestStepGroup sg = fg.getStepGroups().get(j);
					htmlBuilder.append("{stepName: '").append(escapeJs(sg.getStepName())).append("', testCases: [");
					for (int k = 0; k < sg.getTestCases().size(); k++) {
						StepsTest tc = sg.getTestCases().get(k);
						String inputVal = tc.getInputValue();
						String verification = tc.getVerification();
						if (tc.getName() != null && (tc.getName().toUpperCase().contains("COUNT CAPTURE") || tc.getName().toUpperCase().contains("TEXT CAPTURE")) && inputVal != null && !inputVal.trim().isEmpty()) {
							if (tc.getName().toUpperCase().contains("COUNT")) {
								verification = "Captured Count: " + inputVal;
							} else {
								verification = "Captured Text: " + inputVal;
							}
							inputVal = null;
						}
						if (inputVal != null && (inputVal.trim().isEmpty() || inputVal.startsWith("iVBORw0KGgo") || inputVal.startsWith("/9j/") || inputVal.startsWith("R0lGODlh") || inputVal.length() > 100)) inputVal = null;
						htmlBuilder.append("{name: '").append(escapeJs(tc.getName()))
								.append("', passed: ").append(tc.isPassed())
								.append(", scenario: '").append(escapeJs(tc.getScenario() != null ? tc.getScenario() : "-"))
								.append("', inputValue: '").append(escapeJs(inputVal != null ? inputVal : "-"))
								.append("', verification: '").append(escapeJs(verification != null ? verification : "-"))
								.append("', screenshot: ").append(tc.getScreenshot() != null ? "'" + tc.getScreenshot() + "'" : "null")
								.append("}" + (k < sg.getTestCases().size() - 1 ? "," : ""));
					}
					htmlBuilder.append("]}" + (j < fg.getStepGroups().size() - 1 ? "," : ""));
				}
				htmlBuilder.append("]}" + (i < featureGroupList.size() - 1 ? "," : ""));
			}
			
			htmlBuilder.append("]};")
					.append("let currentFilter = 'all';")
					.append("function filterDrawerData(filter) {")
					.append("  currentFilter = filter;")
					.append("  document.querySelectorAll('.offcanvas-body .btn').forEach(btn => btn.classList.remove('active'));")
					.append("  const activeBtn = filter === 'all' ? 0 : filter === 'passed' ? 1 : 2;")
					.append("  document.querySelectorAll('.offcanvas-body .btn')[activeBtn]?.classList.add('active');")
					.append("  const tbody = document.getElementById('drawerTableBody');")
					.append("  tbody.innerHTML = '';")
					.append("  let displayRegId = window.currentReportData.regressionId;")
					.append("  if(displayRegId.includes('_Functional_') || displayRegId.includes('_Regression_') || displayRegId.includes('_Validation_')) {")
					.append("    const parts = displayRegId.split('_');")
					.append("    displayRegId = parts.filter(p => !'" + currentModuleName + "'.split('_').includes(p) && !['Functional','Regression','Validation','Test','Process'].includes(p)).join('_');")
					.append("  }")
					.append("  document.getElementById('drawerRegression').textContent = displayRegId;")
					.append("  let sno = 0;")
					.append("  window.currentReportData.features.forEach(feature => {")
					.append("    feature.stepGroups.forEach(stepGroup => {")
					.append("      stepGroup.testCases.forEach(testCase => {")
					.append("        if (filter === 'all' || (filter === 'passed' && testCase.passed) || (filter === 'failed' && !testCase.passed)) {")
					.append("          sno++;")
					.append("          let testData = testCase.inputValue || '-';")
					.append("          let expectedResult = testCase.verification || '-';")
					.append("          let actualResult = '-';")
					.append("          if (expectedResult.includes('Captured:') && expectedResult.includes('Current:') && expectedResult.includes('Match:')) {")
					.append("            let capturedMatch = expectedResult.match(/Captured:\\s*([^|]+)/);")
					.append("            let currentMatch = expectedResult.match(/Current:\\s*([^|]+)/);")
					.append("            let matchStatus = expectedResult.match(/Match:\\s*(\\w+)/);")
					.append("            if (capturedMatch && currentMatch && matchStatus) {")
					.append("              expectedResult = capturedMatch[1].trim();")
					.append("              actualResult = currentMatch[1].trim() + (matchStatus[1].trim() === 'Yes' ? '' : '<br>Match: No');")
					.append("            }")
					.append("            testData = '-';")
					.append("          } else if (expectedResult.includes('Captured Count:') || expectedResult.includes('Captured Text:')) {")
					.append("            let capturedMatch = expectedResult.match(/Captured (?:Count|Text):\\s*(.+)/);")
					.append("            if (capturedMatch) {")
					.append("              expectedResult = capturedMatch[1].trim();")
					.append("              actualResult = capturedMatch[1].trim();")
					.append("            }")
					.append("            testData = '-';")
					.append("          } else if (expectedResult.includes('|') && expectedResult.includes('Found:')) {")
					.append("            let parts = expectedResult.split('|');")
					.append("            let expValue = parts[0].replace(/^Expected:\\s*/i, '').trim();")
					.append("            let foundMatch = expectedResult.match(/Found:\\s*(\\w+)/);")
					.append("            if (foundMatch) {")
					.append("              expectedResult = expValue;")
					.append("              actualResult = expValue + (foundMatch[1].trim() === 'Yes' ? '' : '<br>Found: No');")
					.append("            }")
					.append("          } else if (expectedResult !== '-') {")
					.append("            if (expectedResult.includes(' - ')) {")
					.append("              let expParts = expectedResult.split(' - ');")
					.append("              let expValue = expParts.length > 1 ? expParts[1] : expectedResult;")
					.append("              expectedResult = expValue;")
					.append("              actualResult = expValue + (testCase.passed ? '' : '<br>Found: No');")
					.append("            } else {")
					.append("              actualResult = expectedResult + (testCase.passed ? '' : '<br>Found: No');")
					.append("            }")
					.append("          } else {")
					.append("            expectedResult = 'Action Completed';")
					.append("            actualResult = testCase.passed ? 'Success' : 'Failed';")
					.append("          }")
					.append("          const status = testCase.passed ? '<span class=\"badge badge-success\">Passed</span>' : '<span class=\"badge badge-danger\">Failed</span>';")
					.append("          const screenshot = testCase.screenshot ? '<i class=\"bi bi-camera\" style=\"cursor:pointer\" onclick=\"showImageFromPath(\\''+testCase.screenshot+'\\')\"</i>' : '-';")
					.append("          const row = tbody.insertRow();")
					.append("          if(!testCase.passed)row.style.fontWeight='bold';")
					.append("          row.innerHTML = `<td>${sno}</td><td>${testCase.name}</td><td>${testData}</td><td>${expectedResult}</td><td>${actualResult}</td><td>${status}</td><td>${screenshot}</td>`;")
					.append("        }")
					.append("      });")
					.append("    });")
					.append("  });")
					.append("}")
					.append("document.getElementById('detailsDrawer').addEventListener('shown.bs.offcanvas', function() { filterDrawerData(currentFilter); });")
					.append("</script>");
			
			// Store regression data for later aggregation
			String regressionId = currentModuleName + "_" + getCurrentTestType();
			List<FeatureGroup> allRegressions = getRegressionData(regressionId);
			
			// Map current features to their regression ID
			for (FeatureGroup fg : featureGroupList) {
				featureToRegressionIdMap.put(fg.getFeatureName() + "_" + System.identityHashCode(fg), sanitizedDisplayId);
			}
			
			allRegressions.addAll(featureGroupList);
			updateRegressionData(regressionId, allRegressions);
			
			// Generate testData file with all accumulated regression data
			generateTestDataJSFile(allRegressions);
			

			
			htmlBuilder.append(
					"<script>"+bootstrapscript+"</script>");
			htmlBuilder.append("</body></html>");

			try (FileWriter writer = new FileWriter(file)) {
				writer.write(htmlBuilder.toString());
			}
			
			// Create URL-safe alias if needed
			createUrlSafeAlias(sanitizedPath);
			
			// Don't send email here - will be sent at the end with all reports

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}