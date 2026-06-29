package com.kriyatec.automation;

import com.kriyatec.automation.excelutil.ExcelUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DynamicValueResolver {
    private final Map<String, String> generatedValues = new HashMap<>();
    private String lastGeneratedName = null;
    private final Map<String, Integer> counters = new HashMap<>();

    public String resolve(String text) {
        if (text == null || text.isBlank())
            return "";

        // 1. Auto-generate random data
        if (text.toLowerCase().contains("random.")) {
            return handleRandomGeneration(text);
        }

        // 2. Reuse placeholders: {{USER_1}}, {{EMAIL_1}}, {{LAST_NAME}}
        if (text.startsWith("{{") && text.endsWith("}}")) {
            return handlePlaceholder(text);
        }

        return text;
    }

    private String handleRandomGeneration(String text) {
        String type = text;
        Integer explicitIndex = null;
        String dateFormat = null;
        Integer dayOffset = null;

        if (text.contains(":")) {
            String[] parts = text.split(":", 3);
            type = parts[0];
            if (type.equals("random.date")) {
                try {
                    dayOffset = Integer.parseInt(parts[1]);
                    if (parts.length >= 3)
                        dateFormat = parts[2];
                } catch (NumberFormatException e) {
                    dateFormat = parts[1];
                    dayOffset = 0;
                }
            } else if (parts.length >= 2) {
                try {
                    explicitIndex = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (type.equals("random.date")) {
            dayOffset = 0;
            dateFormat = "dd MMMM yyyy";
        }

        String realValue;
        if (type.equals("random.date")) {
            realValue = generateRandomDate(dayOffset, dateFormat);
        } else {
            realValue = ExcelUtils.getFieldValueRegression(type);
        }

        storeGenerated(type, realValue, explicitIndex);
        return realValue;
    }

    private void storeGenerated(String type, String value, Integer explicitIndex) {
        String originalCategory = type.replace("random.", "").toUpperCase();
        String category = originalCategory;
        if (category.equals("NAME") || category.equals("LONGNAME"))
            category = "USER";

        int index = explicitIndex != null ? explicitIndex : incrementCounter(category);
        generatedValues.put(category + "_" + index, value);

        if (!originalCategory.equals(category)) {
            generatedValues.put(originalCategory + "_" + index, value);
        }

        if (category.equals("USER")) {
            lastGeneratedName = value;
        }
        System.out.println("[GENERATED] " + category + "_" + index + " = " + value);
    }

    private String handlePlaceholder(String text) {
        String key = text.substring(2, text.length() - 2).toUpperCase();
        if (key.equals("LAST_NAME")) {
            if (lastGeneratedName == null)
                throw new RuntimeException("No name generated yet!");
            return lastGeneratedName;
        }
        String value = generatedValues.get(key);
        if (value != null)
            return value;
        throw new RuntimeException("Placeholder not found: " + text);
    }

    private int incrementCounter(String category) {
        int count = counters.getOrDefault(category, 0) + 1;
        counters.put(category, count);
        return count;
    }

    public String generateRandomDate(Integer dayOffset, String format) {
        if (format == null || format.isEmpty())
            format = "dd MMMM yyyy";
        if (dayOffset == null)
            dayOffset = 0;
        LocalDate date = LocalDate.now().plusDays(dayOffset);
        return date.format(DateTimeFormatter.ofPattern(format));
    }
}
