package com.kriyatec.automation.models;

import java.util.ArrayList;
import java.util.List;

public class FeatureGroup {
        private final String featureName;
        private final List<TestStepGroup> stepGroups;

        public FeatureGroup(String featureName) {
            this.featureName = featureName;
            this.stepGroups = new ArrayList<>();
        }

        public String getFeatureName() {
            return featureName;
        }

        public List<TestStepGroup> getStepGroups() {
            return stepGroups;
        }
    }