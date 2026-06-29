package com.kriyatec.automation.models;

public class StepsTest {
	private String name;
	private Boolean passed; 
	private String timestamp;
	private String details;
	private String screenshot;

    private String inputValue; 
    private String scenario;
    private String startTime;
    private String endTime;
    private String verification;
	
	
	public StepsTest(
			String name, 
			Boolean passed, 
			String details, 
            String inputValue, 
            String scenario
			) 
	{
		this.name = name;
		this.passed = passed; 
		this.details = details; 
	      this.inputValue = inputValue;
	      this.scenario = scenario;
	}

	
	
    public String getInputValue() {
        return inputValue;
    }
	
    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }
    
    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }
    
    
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public String getVerification() {
        return verification;
    }
    
    public void setVerification(String verification) {
        this.verification = verification;
    }
    
    
    
	
	
	
	public String getName() {
		return name;
	}

	public Boolean isPassed() {
		return passed;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}
 
	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}
}