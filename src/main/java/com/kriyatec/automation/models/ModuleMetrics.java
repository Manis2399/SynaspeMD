package com.kriyatec.automation.models;

public class ModuleMetrics {
    private String moduleName;
    private String indexFileName;
    private int regressionCount;
    private int totalSteps;
    private int stepsPass;
    private int stepsFail;
    private int totalTestCases;
    private int testCasesPass;
    private int testCasesFail;
    private long timeTakenMinutes;

    public ModuleMetrics(String moduleName, String indexFileName) {
        this.moduleName = moduleName;
        this.indexFileName = indexFileName;
    }

    public String getModuleName() { return moduleName; }
    public String getIndexFileName() { return indexFileName; }
    public int getRegressionCount() { return regressionCount; }
    public void setRegressionCount(int regressionCount) { this.regressionCount = regressionCount; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public int getStepsPass() { return stepsPass; }
    public void setStepsPass(int stepsPass) { this.stepsPass = stepsPass; }
    public int getStepsFail() { return stepsFail; }
    public void setStepsFail(int stepsFail) { this.stepsFail = stepsFail; }
    public int getTotalTestCases() { return totalTestCases; }
    public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }
    public int getTestCasesPass() { return testCasesPass; }
    public void setTestCasesPass(int testCasesPass) { this.testCasesPass = testCasesPass; }
    public int getTestCasesFail() { return testCasesFail; }
    public void setTestCasesFail(int testCasesFail) { this.testCasesFail = testCasesFail; }
    public long getTimeTakenMinutes() { return timeTakenMinutes; }
    public void setTimeTakenMinutes(long timeTakenMinutes) { this.timeTakenMinutes = timeTakenMinutes; }
}
