package com.kriyatec.automation.excelutil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ModuleReportGenerator {

    public static void generateModuleReport(String moduleName, List<String> regressionReports, 
                                           List<String> functionalReports, List<String> validationReports,
                                           Map<String, Integer> metrics, String reportPath) {
        try {
            // Load report title from config
            String reportTitle = "Test Execution Report"; // Default title
            try {
                java.util.Properties props = new java.util.Properties();
                props.load(new java.io.FileInputStream("src/main/resources/conf.env"));
                reportTitle = props.getProperty("REPORT_TITLE", "Test Execution Report");
            } catch (Exception e) {
                // Use default if config not found
            }
            
            // Sanitize the report path
            File originalFile = new File(reportPath);
            String sanitizedFilename = sanitizeFilename(originalFile.getName());
            String sanitizedPath = originalFile.getParent() + File.separator + sanitizedFilename;
            
            File file = new File(sanitizedPath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            String bootstrapcss = new String(Files.readAllBytes(Paths.get("src/main/resources/css/bootstrap.txt")));
            String bootstrapicons = new String(Files.readAllBytes(Paths.get("src/main/resources/css/bootstrap-icons.txt")));
            String bootstrapscript = new String(Files.readAllBytes(Paths.get("src/main/resources/script/bootstrap.txt")));
            String testType = "Functional";
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>").append(reportTitle).append("</title>")
                .append("<meta charset='UTF-8'>")
                .append("<style>").append(bootstrapcss).append("</style>")
                .append("<style>").append(bootstrapicons).append("</style>")
                .append("<style>")
                .append("body {font-family: Arial, sans-serif; background-color: #f4f7fc; padding: 20px;}")
                .append(".breadcrumb {background-color: #fff; padding: 10px; border-radius: 5px; margin-bottom: 20px;}")
                .append(".header {font-size: 24px; font-weight: bold; color: #333; margin-bottom: 20px;}")
                .append(".card.clickable {cursor: pointer;}")
                .append(".bi-camera {cursor: pointer; font-size: 1.2rem;}")
                .append(".table td:nth-last-child(-n+2), .table th:nth-last-child(-n+2) {text-align: center; vertical-align: middle;}")
                .append("</style>")
                .append("<script src='testData'></script>")
                .append("</head><body>");

            // Breadcrumb
            html.append("<nav aria-label='breadcrumb'><ol class='breadcrumb'>")
                .append("<li class='breadcrumb-item'><a href='index.html'>Home</a></li>")
                .append("<li class='breadcrumb-item active' aria-current='page'>").append(moduleName).append("</li>")
                .append("</ol></nav>");

            html.append("<div class='header'>").append(reportTitle).append("</div>")
                .append("<div style='font-size: 20px; font-weight: 600; color: #555; margin-bottom: 20px;'>").append(moduleName).append("</div>");


            // Summary Cards
            html.append("<div class='row text-center mb-4'>");
            html.append("<div class='col-lg-2 col-md-6'><div class='card border-info mb-3'><h5 class='card-title'>Regressions</h5><h1 class='card-text text-info'>").append(metrics.get("regressions")).append("</h1></div></div>");
            html.append("<div class='col-lg-3 col-md-6'><div class='card clickable border-primary mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterModuleData(\"all\",\"").append(moduleName).append("\")'><h5 class='card-title'>Total Steps/Test Cases</h5><h1 class='card-text text-primary'>").append(metrics.get("totalSteps")).append(" / ").append(metrics.get("totalTestCases")).append("</h1></div></div>");
            html.append("<div class='col-lg-3 col-md-6'><div class='card clickable border-success mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterModuleData(\"all\",\"").append(moduleName).append("\")'><h5 class='card-title'>Steps Pass / Fail</h5><h1 class='card-text text-success'>").append(metrics.get("totalSteps") - metrics.get("stepsFailed")).append(" / <span class='text-danger'>").append(metrics.get("stepsFailed")).append("</span></h1></div></div>");
            html.append("<div class='col-lg-4 col-md-6'><div class='card clickable border-success mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterModuleData(\"passed\",\"").append(moduleName).append("\")'><h5 class='card-title'>Test Cases Pass / Fail</h5><h1 class='card-text text-success'>").append(metrics.get("testCasesPass")).append(" / <span class='text-danger'>").append(metrics.get("testCasesFailed")).append("</span></h1></div></div>");
            html.append("</div>");

            // Regression Reports Table
            if (!regressionReports.isEmpty()) {
                html.append("<div class='card mb-3'><div class='card-body'><h4>Regression Reports</h4>")
                    .append("<table class='table table-striped'><thead><tr><th>#</th><th>Report</th><th>Total Steps</th><th>Steps Pass</th><th>Steps Failed</th><th>Total Test Cases</th><th>Test Cases Pass</th><th>Test Cases Failed</th></tr></thead><tbody>");
                testType = "Regression";
                for (int i = 0; i < regressionReports.size(); i++) {
                    String reportFilePath = regressionReports.get(i);
                    String reportName = new File(reportFilePath).getName();
                    String key = "regression_" + i;
                    html.append("<tr><td>").append(i + 1).append("</td>")
                        .append("<td><a href='").append(reportFilePath).append("' target='_blank'>").append(reportName).append("</a></td>")
                        .append("<td>").append(metrics.getOrDefault(key + "_totalSteps", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault(key + "_stepsPass", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault(key + "_stepsFailed", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault(key + "_totalTestCases", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault(key + "_testCasesPass", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault(key + "_testCasesFailed", 0)).append("</td></tr>");
                }

                html.append("</tbody></table></div></div>");
            }
            
            // Functional Reports Table
            if (!functionalReports.isEmpty()) {
                html.append("<div class='card mb-3'><div class='card-body'><h4>Functional Reports</h4>")
                    .append("<table class='table table-striped'><thead><tr><th>#</th><th>Report</th><th>Total Steps</th><th>Steps Pass</th><th>Steps Failed</th><th>Total Test Cases</th><th>Test Cases Pass</th><th>Test Cases Failed</th></tr></thead><tbody>");

                for (int i = 0; i < functionalReports.size(); i++) {
                    String reportFilePath = functionalReports.get(i);
                    String reportName = new File(reportFilePath).getName();
                    html.append("<tr><td>").append(i + 1).append("</td>")
                        .append("<td><a href='").append(reportFilePath).append("' target='_blank'>").append(reportName).append("</a></td>")
                        .append("<td>").append(metrics.getOrDefault("functional_totalSteps", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("functional_stepsPass", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("functional_stepsFailed", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("functional_totalTestCases", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("functional_testCasesPass", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("functional_testCasesFailed", 0)).append("</td></tr>");
                }

                html.append("</tbody></table></div></div>");
            }
            
            // Validation Reports Table
            if (!validationReports.isEmpty()) {
                html.append("<div class='card mb-3'><div class='card-body'><h4>Validation Reports</h4>")
                    .append("<table class='table table-striped'><thead><tr><th>#</th><th>Report</th><th>Total Steps</th><th>Steps Pass</th><th>Steps Failed</th><th>Total Test Cases</th><th>Test Cases Pass</th><th>Test Cases Failed</th></tr></thead><tbody>");
                testType = "Validation";

                for (int i = 0; i < validationReports.size(); i++) {
                    String reportFilePath = validationReports.get(i);
                    String reportName = new File(reportFilePath).getName();
                    html.append("<tr><td>").append(i + 1).append("</td>")
                        .append("<td><a href='").append(reportFilePath).append("' target='_blank'>").append(reportName).append("</a></td>")
                        .append("<td>").append(metrics.getOrDefault("validation_totalSteps", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("validation_stepsPass", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("validation_stepsFailed", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("validation_totalTestCases", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("validation_testCasesPass", 0)).append("</td>")
                        .append("<td>").append(metrics.getOrDefault("validation_testCasesFailed", 0)).append("</td></tr>");
                }

                html.append("</tbody></table></div></div>");
            }
            
            html.append("<div class='offcanvas offcanvas-end' tabindex='-1' id='detailsDrawer' style='width:100vw;'>")
                .append("<div class='offcanvas-header'><h5 class='offcanvas-title' id='drawerTitle'>").append(moduleName).append(" - Test Details</h5>")
                .append("<button type='button' class='btn-close' data-bs-dismiss='offcanvas' aria-label='Close'><span aria-hidden='true' style='font-size:2rem;line-height:1;'>&times;</span></button></div>")
                .append("<div class='offcanvas-body' style='max-height: calc(100vh - 100px); overflow-y: auto;'>")
                .append("<div class='mb-3'><span id='statusCount' class='badge bg-secondary'></span></div>")
                .append("<div class='mb-3'>")
                .append("<button class='btn btn-outline-primary btn-sm me-2' onclick='filterModuleData(\"all\",\"").append(moduleName).append("\")'>All</button>")
                .append("<button class='btn btn-outline-success btn-sm me-2' onclick='filterModuleData(\"passed\",\"").append(moduleName).append("\")'>Passed</button>")
                .append("<button class='btn btn-outline-danger btn-sm' onclick='filterModuleData(\"failed\",\"").append(moduleName).append("\")'>Failed</button>")
                .append("</div>")
                .append("<div style='margin-bottom:15px;'><strong>Module:</strong> ").append(moduleName).append(" &nbsp;&nbsp;&nbsp; <strong>Type:</strong> <span id='testType'></span></div>")
                .append("<div class='table-responsive' style='max-height: calc(100vh - 250px); overflow-y: auto;'>")
                .append("<table class='table table-sm table-bordered'>")
                .append("<thead style='position: sticky; top: 0; background-color: white; z-index: 10;'><tr><th style='width:5%'>S.No</th><th style='width:20%'>Test Case</th><th style='width:10%'>Test Data</th><th style='width:22.5%'>Expected Result</th><th style='width:22.5%'>Actual Result</th><th style='width:10%'>Status</th><th style='width:10%'>Screenshot</th></tr></thead>")
                .append("<tbody id='drawerTableBody'></tbody></table></div></div></div>");
            
            html.append("<div class='modal modal-lg fade' id='imageModal' tabindex='-1'><div class='modal-dialog modal-dialog-centered'>")
                .append("<div class='modal-content'><div class='modal-header'><h5 class='modal-title'>Screenshot</h5>")
                .append("<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'><span aria-hidden='true' style='font-size:2rem;line-height:1;'>&times;</span></button></div>")
                .append("<div class='modal-body text-center'><img id='base64Image' src='' class='img-fluid'></div></div></div></div>");
            
            html.append("<script>")
                .append("let currentFilter='all',currentModule='';")
                .append("function downloadModuleReports(){")
                .append("const timestamp=new Date().toISOString().slice(0,16).replace('T','_').replace(/:/g,'-');")
                .append("const zipName=timestamp+'_module-reports.zip';")
                .append("const zip=new JSZip();")
                .append("const allFiles=[window.location.pathname.split('/').pop()];")
                .append("document.querySelectorAll('a[href$=\".html\"]').forEach(link=>{if(!link.href.includes('index.html'))allFiles.push(link.getAttribute('href'));});")
                .append("Promise.all(allFiles.map(file=>fetch(file).then(r=>r.text()).then(content=>zip.file(file.split('/').pop(),content)))).then(()=>{")
                .append("zip.generateAsync({type:'blob'}).then(blob=>{")
                .append("const url=window.URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=zipName;document.body.appendChild(a);a.click();window.URL.revokeObjectURL(url);document.body.removeChild(a);")
                .append("});")
                .append("}).catch(error=>console.error('Download failed:',error));")
                .append("}")
                .append("function showImageFromPath(path){document.getElementById('base64Image').src=path;new bootstrap.Modal(document.getElementById('imageModal')).show();}")
                .append("function filterModuleData(filter,module){currentFilter=filter;currentModule=module;let count=0,sno=0;")
                .append("document.querySelectorAll('.offcanvas-body .btn').forEach(b=>b.classList.remove('active'));")
                .append("const idx=filter==='all'?0:filter==='passed'?1:2;document.querySelectorAll('.offcanvas-body .btn')[idx]?.classList.add('active');")
                .append("const tbody=document.getElementById('drawerTableBody');tbody.innerHTML='';")
                .append("if(!window.testDataStore)return;")
                .append("const title=filter==='all'?'All Tests':filter==='passed'?'Passed Tests':'Failed Tests';")
                .append("document.getElementById('drawerTitle').textContent=module+' - '+title;")
                .append("const testTypeKey=Object.keys(window.testDataStore).find(k=>k.startsWith(module+'_'));")
                .append("const testType=testTypeKey?testTypeKey.split('_').pop().charAt(0).toUpperCase()+testTypeKey.split('_').pop().slice(1):'';")
                .append("document.getElementById('testType').textContent=testType;")
                .append("Object.keys(window.testDataStore).filter(k=>k.startsWith(module+'_')).forEach(key=>{")
                .append("const data=window.testDataStore[key];if(!data||!data.features)return;data.features.forEach(f=>{f.stepGroups.forEach(sg=>{sg.testCases.forEach(tc=>{")
                .append("if(filter==='all'||(filter==='passed'&&tc.passed)||(filter==='failed'&&!tc.passed)){count++;sno++;")
                .append("let testData=tc.inputValue||'-';let expectedResult=tc.verification||'-';let actualResult='-';")
                .append("if(expectedResult.includes('Captured:')&&expectedResult.includes('Current:')&&expectedResult.includes('Match:')){")
                .append("let capturedMatch=expectedResult.match(/Captured:\\s*([^|]+)/);")
                .append("let currentMatch=expectedResult.match(/Current:\\s*([^|]+)/);")
                .append("let matchStatus=expectedResult.match(/Match:\\s*(\\w+)/);")
                .append("if(capturedMatch&&currentMatch&&matchStatus){")
                .append("expectedResult=capturedMatch[1].trim();")
                .append("actualResult=currentMatch[1].trim()+(matchStatus[1].trim()==='Yes'?'':'<br>Match: No');}testData='-';")
                .append("}else if(expectedResult.includes('Captured Count:')||expectedResult.includes('Captured Text:')){")
                .append("let capturedMatch=expectedResult.match(/Captured (?:Count|Text):\\s*(.+)/);")
                .append("if(capturedMatch){expectedResult=capturedMatch[1].trim();actualResult=capturedMatch[1].trim();}testData='-';")
                .append("}else if(expectedResult.includes('|')&&expectedResult.includes('Found:')){")
                .append("let parts=expectedResult.split('|');")
                .append("let expValue=parts[0].replace(/^Expected:\\s*/i,'').trim();")
                .append("let foundMatch=expectedResult.match(/Found:\\s*(\\w+)/);")
                .append("if(foundMatch){expectedResult=expValue;actualResult=expValue+(foundMatch[1].trim()==='Yes'?'':'<br>Found: No');}")
                .append("}else if(expectedResult!=='-'){")
                .append("if(expectedResult.includes(' - ')){")
                .append("let expParts=expectedResult.split(' - ');")
                .append("let expValue=expParts.length>1?expParts[1]:expectedResult;")
                .append("expectedResult=expValue;actualResult=expValue+(tc.passed?'':'<br>Found: No');")
                .append("}else{actualResult=expectedResult+(tc.passed?'':'<br>Found: No');}")
                .append("}else{expectedResult='Action Completed';actualResult=tc.passed?'Success':'Failed';}")
                .append("const status=tc.passed?'<span class=\"badge bg-success\">Passed</span>':'<span class=\"badge bg-danger\">Failed</span>';")
                .append("const screenshot=tc.screenshot?tc.screenshot.replace('../',''):tc.screenshot;")
                .append("const screenshotHtml=screenshot?'<i class=\"bi bi-camera\" onclick=\"showImageFromPath(\\''+screenshot+'\\')\"</i>':'-';")
                .append("const row=tbody.insertRow();")
                .append("if(!tc.passed)row.style.fontWeight='bold';")
                .append("row.innerHTML=`<td>${sno}</td><td>${tc.name}</td><td>${testData}</td><td>${expectedResult}</td><td>${actualResult}</td><td>${status}</td><td>${screenshotHtml}</td>`;")
                .append("}});});});});document.getElementById('statusCount').textContent=count+' test(s) found';}");
            html.append("document.getElementById('detailsDrawer').addEventListener('shown.bs.offcanvas',()=>filterModuleData(currentFilter,currentModule));")
                .append("</script>");
            
            html.append("<script>").append(bootstrapscript).append("</script>");
            html.append("</body></html>");

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(html.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sanitizes filename by removing/replacing problematic characters
     * and ensuring proper .html extension
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "report.html";
        }
        
        // Remove or replace problematic characters
        String sanitized = filename
            .trim()
            .replaceAll("[\\s]+", "_")           // Replace spaces with underscores
            .replaceAll("[–—]", "-")             // Replace em/en dashes with hyphens
            .replaceAll("[^a-zA-Z0-9._-]", "")   // Remove other special characters
            .replaceAll("_{2,}", "_")            // Replace multiple underscores with single
            .replaceAll("^_+|_+$", "");          // Remove leading/trailing underscores
        
        // Ensure .html extension
        if (!sanitized.toLowerCase().endsWith(".html")) {
            sanitized += ".html";
        }
        
        return sanitized;
    }
}
