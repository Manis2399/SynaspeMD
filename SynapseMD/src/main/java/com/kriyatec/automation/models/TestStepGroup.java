package com.kriyatec.automation.models;

import java.util.List;
import java.util.UUID;

public class TestStepGroup {
	private final String id;
	private final String stepName;
	private final List<StepsTest> testCases;

	public TestStepGroup(String stepName, List<StepsTest> testCases) {
		this.stepName = stepName;
		this.testCases = testCases;
		this.id= UUID.randomUUID().toString();
	}

	public String getStepName() {
		return stepName;
	}

	public List<StepsTest> getTestCases() {
		return testCases;
	}

	public String getId() {
		return id;
	}
}