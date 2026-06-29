package com.kriyatec.automation.excelutil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.kriyatec.automation.models.ModuleMetrics;

public class MasterReportGenerator {

    public static void generateMasterReport(List<ModuleMetrics> moduleMetricsList, String reportPath) {
        try {
            // Load report title from config
            String reportTitle = "Test Execution Report"; // Default title
            try {
                java.util.Properties props = new java.util.Properties();
                props.load(new java.io.FileInputStream("src/main/resources/conf.env"));
                reportTitle = props.getProperty("REPORT_TITLE", "Test Execution Report");
                
            } catch (Exception e) {
                
            }
            File file = new File(reportPath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            int totalModules = moduleMetricsList.size();
            int totalRegressions = 0, totalSteps = 0, totalStepsPass = 0, totalStepsFail = 0;
            int totalTestCases = 0, totalTestCasesPass = 0, totalTestCasesFail = 0;

            for (ModuleMetrics metrics : moduleMetricsList) {
                totalRegressions += metrics.getRegressionCount();
                totalSteps += metrics.getTotalSteps();
                totalStepsPass += metrics.getStepsPass();
                totalStepsFail += metrics.getStepsFail();
                totalTestCases += metrics.getTotalTestCases();
                totalTestCasesPass += metrics.getTestCasesPass();
                totalTestCasesFail += metrics.getTestCasesFail();
            }

            String bootstrapcss = new String(Files.readAllBytes(Paths.get("src/main/resources/css/bootstrap.txt")));
            String bootstrapicons = new String(Files.readAllBytes(Paths.get("src/main/resources/css/bootstrap-icons.txt")));
            String bootstrapscript = new String(Files.readAllBytes(Paths.get("src/main/resources/script/bootstrap.txt")));

            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>").append(reportTitle).append("</title>")
                .append("<meta charset='UTF-8'>")
                .append("<style>").append(bootstrapcss).append("</style>")
                .append("<style>").append(bootstrapicons).append("</style>")
                .append("<style>")
                .append("body {font-family: Arial, sans-serif; background-color: #f4f7fc; padding: 20px;}")
                .append(".header {font-size: 28px; font-weight: bold; color: #333; margin-bottom: 30px;}")
                .append(".card.clickable {cursor: pointer;}")
                .append(".bi-camera {cursor: pointer; font-size: 1.2rem;}")
                .append(".table td:nth-last-child(-n+2), .table th:nth-last-child(-n+2) {text-align: center; vertical-align: middle;}")
                .append("</style>")
                .append("<script src='testData' type='text/javascript'></script>")
                .append("</head><body>")
                .append("<div class='header'>").append(reportTitle).append("</div>");


            // Summary Cards
            html.append("<div class='row text-center mb-4'>");
            html.append("<div class='col-lg col-md-4 col-sm-6'><div class='card border-primary mb-3'><h5 class='card-title'>Total Modules</h5><h1 class='card-text text-primary'>").append(totalModules).append("</h1></div></div>");
            html.append("<div class='col-lg col-md-4 col-sm-6'><div class='card border-info mb-3'><h5 class='card-title'>Total Regressions</h5><h1 class='card-text text-info'>").append(totalRegressions).append("</h1></div></div>");
            html.append("<div class='col-lg col-md-4 col-sm-6'><div class='card clickable border-primary mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterAllData(\"all\")'><h5 class='card-title'>Total Steps/Test Cases</h5><h1 class='card-text text-primary'>").append(totalSteps).append(" / ").append(totalTestCases).append("</h1></div></div>");
            html.append("<div class='col-lg col-md-4 col-sm-6'><div class='card clickable border-success mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterAllData(\"all\")'><h5 class='card-title'>Steps Pass / Fail</h5><h1 class='card-text text-success'>").append(totalStepsPass).append(" / <span class='text-danger'>").append(totalStepsFail).append("</span></h1></div></div>");
            html.append("<div class='col-lg col-md-4 col-sm-6'><div class='card clickable border-success mb-3' data-bs-toggle='offcanvas' data-bs-target='#detailsDrawer' onclick='filterAllData(\"passed\")'><h5 class='card-title'>Test Cases Pass / Fail</h5><h1 class='card-text text-success'>").append(totalTestCasesPass).append(" / <span class='text-danger'>").append(totalTestCasesFail).append("</span></h1></div></div>");
            html.append("</div>");

            // Table
            html.append("<div class='card'><div class='card-body'><h4>Module-wise Breakdown</h4>")
                .append("<table class='table table-striped table-hover'>")
                .append("<thead><tr>")
                .append("<th>Module Name</th>")
                .append("<th>No of Regressions</th>")
                .append("<th>Total Steps</th>")
                .append("<th>Steps Pass</th>")
                .append("<th>Steps Fail</th>")
                .append("<th>Total Test Cases</th>")
                .append("<th>Test Cases Pass</th>")
                .append("<th>Test Cases Fail</th>")
                .append("<th>Time Taken (min)</th>")
                .append("</tr></thead><tbody>");

            for (ModuleMetrics metrics : moduleMetricsList) {
                html.append("<tr>")
                    .append("<td><a href='").append(metrics.getIndexFileName()).append("' target='_blank'>").append(metrics.getModuleName()).append("</a></td>")
                    .append("<td>").append(metrics.getRegressionCount()).append("</td>")
                    .append("<td>").append(metrics.getTotalSteps()).append("</td>")
                    .append("<td>").append(metrics.getStepsPass()).append("</td>")
                    .append("<td>").append(metrics.getStepsFail()).append("</td>")
                    .append("<td>").append(metrics.getTotalTestCases()).append("</td>")
                    .append("<td>").append(metrics.getTestCasesPass()).append("</td>")
                    .append("<td>").append(metrics.getTestCasesFail()).append("</td>")
                    .append("<td>").append(metrics.getTimeTakenMinutes()).append("</td>")
                    .append("</tr>");
            }

            html.append("</tbody></table></div></div>");
            
            html.append("<div class='offcanvas offcanvas-end' tabindex='-1' id='detailsDrawer' style='width:100vw;'>")
                .append("<div class='offcanvas-header'><h5 class='offcanvas-title' id='drawerTitle'>All Modules - Test Details</h5>")
                .append("<button type='button' class='btn-close' data-bs-dismiss='offcanvas' aria-label='Close'><span aria-hidden='true' style='font-size:2rem;line-height:1;'>&times;</span></button></div>")
                .append("<div class='offcanvas-body' style='max-height: calc(100vh - 100px); overflow-y: auto;'>")
                .append("<div class='mb-3'><span id='statusCount' class='badge bg-secondary'></span></div>")
                .append("<div class='mb-3'>")
                .append("<button class='btn btn-outline-primary btn-sm me-2' onclick='filterAllData(\"all\")'>All</button>")
                .append("<button class='btn btn-outline-success btn-sm me-2' onclick='filterAllData(\"passed\")'>Passed</button>")
                .append("<button class='btn btn-outline-danger btn-sm' onclick='filterAllData(\"failed\")'>Failed</button>")
                .append("</div>")
                .append("<div id='moduleTablesContainer'></div></div></div>");
            
            html.append("<div class='modal modal-lg fade' id='imageModal' tabindex='-1'><div class='modal-dialog modal-dialog-centered'>")
                .append("<div class='modal-content'><div class='modal-header'><h5 class='modal-title'>Screenshot</h5>")
                .append("<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'><span aria-hidden='true' style='font-size:2rem;line-height:1;'>&times;</span></button></div>")
                .append("<div class='modal-body text-center'><img id='base64Image' src='' class='img-fluid'></div></div></div></div>");
            
            html.append("<script>")
                .append("let currentFilter='all';")
                .append("function downloadReports(){")
                .append("const timestamp=new Date().toISOString().slice(0,16).replace('T','_').replace(/:/g,'-');")
                .append("const zipName=timestamp+'_test-reports.zip';")
                .append("fetch('/download-reports',{method:'POST'}).then(response=>response.blob()).then(blob=>{")
                .append("const url=window.URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=zipName;document.body.appendChild(a);a.click();window.URL.revokeObjectURL(url);document.body.removeChild(a);")
                .append("}).catch(error=>{")
                .append("console.error('Download failed:',error);")
                .append("const allFiles=['index.html'];")
                .append("document.querySelectorAll('a[href$=\".html\"]').forEach(link=>{if(link.href.includes('reports/'))allFiles.push(link.href.split('/').pop());});")
                .append("const zip=new JSZip();")
                .append("Promise.all(allFiles.map(file=>fetch(file).then(r=>r.text()).then(content=>zip.file(file,content)))).then(()=>{")
                .append("zip.generateAsync({type:'blob'}).then(blob=>{")
                .append("const url=window.URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=zipName;document.body.appendChild(a);a.click();window.URL.revokeObjectURL(url);document.body.removeChild(a);")
                .append("});});")
                .append("});")
                .append("}")
                .append("function showImageFromPath(path){document.getElementById('base64Image').src=path;new bootstrap.Modal(document.getElementById('imageModal')).show();}")
                .append("function filterAllData(filter){currentFilter=filter;let totalCount=0;")
                .append("document.querySelectorAll('.offcanvas-body .btn').forEach(b=>b.classList.remove('active'));")
                .append("const idx=filter==='all'?0:filter==='passed'?1:2;document.querySelectorAll('.offcanvas-body .btn')[idx]?.classList.add('active');")
                .append("const container=document.getElementById('moduleTablesContainer');container.innerHTML='';")
                .append("if(!window.testDataStore)return;")
                .append("const title=filter==='all'?'All Tests':filter==='passed'?'Passed Tests':'Failed Tests';")
                .append("document.getElementById('drawerTitle').textContent='All Modules - '+title;")
                .append("Object.keys(window.testDataStore).forEach(key=>{const parts=key.split('_');const module=parts.slice(0,-1).join('_');const testType=parts[parts.length-1].charAt(0).toUpperCase()+parts[parts.length-1].slice(1);")
                .append("const data=window.testDataStore[key];if(!data||!data.features)return;let sno=0;let moduleHtml='';")
                .append("data.features.forEach(f=>{f.stepGroups.forEach(sg=>{sg.testCases.forEach(tc=>{")
                .append("if(filter==='all'||(filter==='passed'&&tc.passed)||(filter==='failed'&&!tc.passed)){sno++;totalCount++;")
                .append("let testData=tc.inputValue||'-';let expectedResult=tc.verification||'-';let actualResult='-';")
                .append("if(expectedResult.includes('Captured:')&&expectedResult.includes('Current:')&&expectedResult.includes('Match:')){")
                .append("let capturedMatch=expectedResult.match(/Captured:\\s*([^|]+)/);let currentMatch=expectedResult.match(/Current:\\s*([^|]+)/);let matchStatus=expectedResult.match(/Match:\\s*(\\w+)/);")
                .append("if(capturedMatch&&currentMatch&&matchStatus){expectedResult=capturedMatch[1].trim();actualResult=currentMatch[1].trim()+(matchStatus[1].trim()==='Yes'?'':'<br>Match: No');}testData='-';")
                .append("}else if(expectedResult.includes('Captured Count:')||expectedResult.includes('Captured Text:')){")
                .append("let capturedMatch=expectedResult.match(/Captured (?:Count|Text):\\s*(.+)/);if(capturedMatch){expectedResult=capturedMatch[1].trim();actualResult=capturedMatch[1].trim();}testData='-';")
                .append("}else if(expectedResult.includes('|')&&expectedResult.includes('Found:')){")
                .append("let parts=expectedResult.split('|');let expValue=parts[0].replace(/^Expected:\\s*/i,'').trim();let foundMatch=expectedResult.match(/Found:\\s*(\\w+)/);")
                .append("if(foundMatch){expectedResult=expValue;actualResult=expValue+(foundMatch[1].trim()==='Yes'?'':'<br>Found: No');}")
                .append("}else if(expectedResult!=='-'){")
                .append("if(expectedResult.includes(' - ')){let expParts=expectedResult.split(' - ');let expValue=expParts.length>1?expParts[1]:expectedResult;expectedResult=expValue;actualResult=expValue+(tc.passed?'':'<br>Found: No');")
                .append("}else{actualResult=expectedResult+(tc.passed?'':'<br>Found: No');}")
                .append("}else{expectedResult='Action Completed';actualResult=tc.passed?'Success':'Failed';}")
                .append("const status=tc.passed?'<span class=\"badge bg-success\">Passed</span>':'<span class=\"badge bg-danger\">Failed</span>';")
                .append("const screenshot=tc.screenshot?tc.screenshot.replace('../',''):tc.screenshot;")
                .append("const screenshotHtml=screenshot?'<i class=\"bi bi-camera\" onclick=\"showImageFromPath(\\''+screenshot+'\\')\"</i>':'-';")
                .append("moduleHtml+=`<tr${!tc.passed?' style=\"font-weight:bold\"':''}><td>${sno}</td><td>${tc.name}</td><td>${testData}</td><td>${expectedResult}</td><td>${actualResult}</td><td>${status}</td><td>${screenshotHtml}</td></tr>`;")
                .append("}});});});")
                .append("if(sno>0){container.innerHTML+=`<div style='margin-bottom:20px;'><div style='margin-bottom:10px;'><strong>Module:</strong> ${module} &nbsp;&nbsp;&nbsp; <strong>Type:</strong> ${testType}</div><table class='table table-sm table-bordered'><thead><tr><th style='width:5%'>S.No</th><th style='width:25%'>Test Case</th><th style='width:10%'>Test Data</th><th style='width:22.5%'>Expected Result</th><th style='width:22.5%'>Actual Result</th><th style='width:10%'>Status</th><th style='width:5%'>Screenshot</th></tr></thead><tbody>${moduleHtml}</tbody></table></div>`;}")
                .append("});document.getElementById('statusCount').textContent=totalCount+' test(s) found';}");
            html.append("document.getElementById('detailsDrawer').addEventListener('shown.bs.offcanvas',()=>filterAllData(currentFilter));")
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
}
