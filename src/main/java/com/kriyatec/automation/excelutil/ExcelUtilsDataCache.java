package com.kriyatec.automation.excelutil;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelUtilsDataCache {

    private Map<String, List<Map<String, String>>> sheetDataCache;

    public ExcelUtilsDataCache() {
        this.sheetDataCache = new HashMap<>();
    }

    /**
     * Caches data for a specific sheet.
     *
     * @param sheetName The name of the sheet.
     * @param data      The data to cache, represented as a list of maps where each map is a row with column names as keys.
     */
    public void cacheSheetData(String sheetName, List<Map<String, String>> data) {
        sheetDataCache.put(sheetName, data);
    }

    /**
     * Retrieves cached data for a specific sheet.
     *
     * @param sheetName The name of the sheet.
     * @return The cached data, or null if no data is cached for the sheet.
     */
    public List<Map<String, String>> getSheetData(String sheetName) {
        return sheetDataCache.get(sheetName);
    }

    /**
     * Clears the cached data for a specific sheet.
     *
     * @param sheetName The name of the sheet.
     */
    public void clearSheetData(String sheetName) {
        sheetDataCache.remove(sheetName);
    }

    /**
     * Clears all cached data.
     */
    public void clearAllData() {
        sheetDataCache.clear();
    }
}

