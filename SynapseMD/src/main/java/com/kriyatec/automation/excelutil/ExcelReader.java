package com.kriyatec.automation.excelutil;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExcelReader {

//    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);

    public Map<String, List<Map<String, String>>> getAllData(String excelFilePath) throws InvalidFormatException, IOException {
        Map<String, List<Map<String, String>>> allData = new HashMap<>();
        
        try (Workbook workbook = getWorkbook(excelFilePath)) { // Use try-with-resources for automatic closing
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<Map<String, String>> sheetData = readSheet(sheet);
                allData.put(sheet.getSheetName(), sheetData);
            }
        } catch (IOException | InvalidFormatException e) {
//            logger.error("Error reading Excel file: {}", e.getMessage());
            throw e; // Rethrow to handle it at the caller level
        }
        
        return allData;
    }

    private Workbook getWorkbook(String excelFilePath) throws IOException, InvalidFormatException {
        return WorkbookFactory.create(new File(excelFilePath));
    }

    private List<Map<String, String>> readSheet(Sheet sheet) {
        List<Map<String, String>> data = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();

        // Assuming the first row contains headers
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            headerRow.forEach(cell -> headers.add(getCellValueAsString(cell)));

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) continue; // Skip empty rows
                
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(i), getCellValueAsString(cell));
                }

                data.add(rowData);
            }
        }
        return data;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) {
                return false; // Row is not empty
            }
        }
        return true; // Row is empty
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return String.valueOf(cell.getNumericCellValue()); // Return calculated value for formulas
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
