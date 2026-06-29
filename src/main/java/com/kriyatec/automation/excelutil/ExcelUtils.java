package com.kriyatec.automation.excelutil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtils {

    private static FakerGenerator fakerGenerator = new FakerGenerator();

    private static ThreadLocal<Map<String, Map<String, List<Map<String, String>>>>> excelCache = ThreadLocal
            .withInitial(HashMap::new);

    public static Map<String, List<Map<String, String>>> getAllData(String excelFilePath) throws IOException {
        var threadCache = excelCache.get();
        if (threadCache.containsKey(excelFilePath)) {
            return threadCache.get(excelFilePath);
        }

        Map<String, List<Map<String, String>>> allData = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(excelFilePath);
                Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<Map<String, String>> sheetData = new ArrayList<>();
                List<String> headers = new ArrayList<>();
                boolean isHeader = true;

                for (Row row : sheet) {
                    if (isHeader) {
                        for (Cell cell : row)
                            headers.add(cell.toString());
                        isHeader = false;
                    } else {
                        Map<String, String> rowData = new HashMap<>();
                        for (Cell cell : row) {
                            if (cell.getColumnIndex() < headers.size()) {
                                rowData.put(headers.get(cell.getColumnIndex()), cell.toString());
                            }
                        }
                        if (!rowData.isEmpty())
                            sheetData.add(rowData);
                    }
                }
                allData.put(sheet.getSheetName(), sheetData);
            }
        }
        threadCache.put(excelFilePath, allData);
        return allData;
    }

    public static void clearCache() {
        excelCache.get().clear();
    }

    // Method to process the Excel data and handle Faker
    public static String getFieldValueRegression(String fieldValue) {
        // Check if the value is a reference to FakerGenerator (e.g., random.name)
        if (fieldValue != null && fieldValue.startsWith("random.")) {
            return fakerGenerator.getFakerData(fieldValue); // Call the appropriate method from FakerGenerator
        }
        return fieldValue;
    }
    // If it's not a reference to Faker, return the value directly

    // Method to process the Excel data and handle Faker
    public static String getFieldValue(String fieldValue) {
        // Check if the value is a reference to FakerGenerator (e.g., random.name)
        if (fieldValue != null && fieldValue.startsWith("random.")) {
            String[] parts = fieldValue.split("\\.");
            if (parts.length == 2) {
                String method = parts[1];
                return fakerGenerator.getFakerData(method); // Call the appropriate method from FakerGenerator
            }
        }
        // If it's not a reference to Faker, return the value directly
        return fieldValue;
    }
}
