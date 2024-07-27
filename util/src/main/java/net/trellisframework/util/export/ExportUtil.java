package net.trellisframework.util.export;

import net.trellisframework.core.log.Logger;
import net.trellisframework.http.exception.InternalServerException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.util.ZipInputStreamZipEntrySource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class ExportUtil {
    private static final String EXCEL_EXTENSION = ".xlsx";
    private static final String CSV_EXTENSION = ".csv";
    private static final String TSV_EXTENSION = ".tsv";
    private static final String RESULT_SHEET_NAME = "Result";

    public static class Excel {
        public static File export(String path, List<?> list) {
            return export(path, list, false);
        }

        public static File export(String path, List<?> list, boolean append) {
            return export(path, "Sheet1", list, append);
        }

        public static File export(String path, String sheetName, List<?> list, boolean append) {
            return export(path, String.valueOf(System.currentTimeMillis()), sheetName, list, append);
        }

        public static File export(String path, String fileName, String sheetName, List<?> list, boolean append) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + EXCEL_EXTENSION)), sheetName, list, append);
        }

        public static File export(File file, String sheet, List<?> list) {
            return export(file, sheet, list, false);
        }

        public static File export(File file, String sheet, List<?> list, boolean append) {
            getWorkbook(file, sheet, list, append);
            return file;
        }

        public static File from(File file, char delimiter, File dest, String sheet) {
            try (FileReader reader = new FileReader(file);
                 CSVParser parser = new CSVParser(reader, CSVFormat.newFormat(delimiter).builder().setHeader().setQuote('"').setSkipHeaderRecord(true).build())) {
                return fromParser(parser, dest, sheet);
            } catch (IOException e) {
                Logger.error("from", e.getMessage(), e);
                throw new InternalServerException(e.getMessage());
            }
        }

        public static File from(String string, char delimiter, File dest, String sheet) {
            try (StringReader reader = new StringReader(string);
                 CSVParser parser = new CSVParser(reader, CSVFormat.newFormat(delimiter).builder().setHeader().setQuote('"').setSkipHeaderRecord(true).build())) {
                return fromParser(parser, dest, sheet);
            } catch (IOException e) {
                Logger.error("from", e.getMessage(), e);
                throw new InternalServerException(e.getMessage());
            }
        }

        private static File fromParser(CSVParser csvParser, File excelFile, String sheetName) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet(sheetName);
                Row headerRow = sheet.createRow(0);
                int columnIndex = 0;
                for (String header : csvParser.getHeaderNames()) {
                    headerRow.createCell(columnIndex++).setCellValue(header);
                }
                int rowIndex = 1;
                for (CSVRecord record : csvParser) {
                    Row row = sheet.createRow(rowIndex++);
                    columnIndex = 0;
                    for (String header : csvParser.getHeaderNames()) {
                        String value = record.get(header);
                        row.createCell(columnIndex++).setCellValue(value != null ? value : "N/A");
                    }
                }
                for (int i = 0; i < csvParser.getHeaderNames().size(); i++) {
                    sheet.autoSizeColumn(i);
                }
                FileOutputStream fileOut = new FileOutputStream(excelFile);
                workbook.write(fileOut);

                return excelFile;
            } catch (IOException e) {
                Logger.error("fromCSVParser", e.getMessage(), e);
                throw new InternalServerException(e.getMessage());
            }
        }

    }

    public static class CSV {
        public static File export(String path, List<?> list) {
            return export(path, list, false);
        }

        public static File export(String path, List<?> list, boolean append) {
            return export(path, String.valueOf(System.currentTimeMillis()), list, append);
        }

        public static File export(String path, String fileName, List<?> list, boolean append) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + CSV_EXTENSION)), list, append);
        }

        public static File export(File file, List<?> list) {
            return export(file, list, false);
        }

        public static File export(File file, List<?> list, boolean append) {
            return ExportUtil.export(file, list, ",", append);
        }

        public static File fromExcel(File file, File excel, String sheetName) {
            return ExportUtil.fromExcel(file, excel, sheetName, CSVFormat.DEFAULT);
        }
    }

    public static class TSV {
        public static File export(String path, List<?> list) {
            return export(path, list, false);
        }

        public static File export(String path, List<?> list, boolean append) {
            return export(path, String.valueOf(System.currentTimeMillis()), list, append);
        }

        public static File export(String path, String fileName, List<?> list, boolean append) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + TSV_EXTENSION)), list, append);
        }

        public static File export(File file, List<?> list) {
            return export(file, list, false);
        }

        public static File export(File file, List<?> list, boolean append) {
            return ExportUtil.export(file, list, "\t", append);
        }

        public static File fromExcel(File file, File excel, String sheetName) {
            return ExportUtil.fromExcel(file, excel, sheetName, CSVFormat.TDF);
        }
    }


    private static Workbook getWorkbook(File file, String sheetName, List<?> list, boolean append) {
        try {
            if (list == null || list.isEmpty())
                return null;
            IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
            ZipInputStreamZipEntrySource.setThresholdBytesForTempFiles(Integer.MAX_VALUE);
            List<String> columnList = getColumnName(list);
            if (ObjectUtils.isEmpty(columnList))
                return null;
            if (!append && file.exists() && !file.delete())
                return null;
            File dirFile = new File(file.getParent());
            if (!(dirFile.exists() || dirFile.mkdirs()) || !(file.exists() || file.createNewFile()))
                return null;
            InputStream stream = new FileInputStream(file);
            Workbook workbook = file.length() > 0 ? WorkbookFactory.create(stream) : new XSSFWorkbook();
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null)
                sheet = workbook.createSheet(sheetName);
            int headerRowIndex = 0, columnIndex = 0;
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null)
                headerRow = sheet.createRow(headerRowIndex);
            for (String column : columnList) {
                Cell cell = headerRow.getCell(columnIndex);
                if (cell == null)
                    cell = headerRow.createCell(columnIndex);
                cell.setCellValue(column);
                columnIndex++;
            }
            int rowIndex = sheet.getPhysicalNumberOfRows();
            for (Object currentRow : list) {
                Row dataRow = sheet.createRow(rowIndex++);
                if (currentRow instanceof Map<?, ?>) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) currentRow).entrySet()) {
                        columnIndex = columnList.indexOf(entry.getKey());
                        String value = Optional.ofNullable(entry.getValue()).map(Object::toString).orElse(null);
                        dataRow.createCell(columnIndex).setCellValue(StringUtils.isBlank(value) ? "N/A" : value);
                    }
                } else {
                    for (Field field : currentRow.getClass().getDeclaredFields()) {
                        field.setAccessible(true);
                        columnIndex = columnList.indexOf(field.getName());
                        String value = Optional.ofNullable(field.get(currentRow)).map(Object::toString).orElse(null);
                        dataRow.createCell(columnIndex).setCellValue(StringUtils.isBlank(value) ? "N/A" : value);
                    }
                }
            }
            for (int i = 0; i < columnList.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            return workbook;
        } catch (Exception e) {
            Logger.error("ExportToExcel", e.getMessage(), e);
            throw new InternalServerException(e.getMessage());
        }
    }

    private static File fromWorkbook(File file, Workbook workbook, String sheetName, CSVFormat format) {
        try {

            File dir = new File(file.getParent());
            if ((workbook == null) || (!(dir.exists() || dir.mkdirs())))
                return null;

            Sheet sheet = workbook.getSheet(sheetName);
            FileWriter fileWriter = new FileWriter(file);
            CSVPrinter csvPrinter = new CSVPrinter(fileWriter, format);

            for (Row row : sheet) {
                for (Cell cell : row) {
                    String cellValue = cell.toString();
                    csvPrinter.print(cellValue);
                }
                csvPrinter.println();
            }

            csvPrinter.close();
            fileWriter.close();
            workbook.close();
            return file;
        } catch (Exception e) {
            Logger.error("FromExcelException", e.getMessage(), e);
            throw new InternalServerException(e.getMessage());
        }
    }

    private static File fromExcel(File file, File excel, String sheetName, CSVFormat format) {
        try {
            return fromWorkbook(file, new XSSFWorkbook(new FileInputStream(excel)), sheetName, format);
        } catch (Exception e) {
            Logger.error("FromExcelException", e.getMessage(), e);
            throw new InternalServerException(e.getMessage());
        }
    }

    private static List<String> getColumnName(List<?> list) {
        List<String> result = new ArrayList<>();
        if (list == null || list.isEmpty())
            return result;
        Object first = list.get(0);
        if (first instanceof Map<?, ?>) {
            result.addAll(((Map<String, Object>) first).keySet().stream().toList());
        } else {
            for (Field field : first.getClass().getDeclaredFields()) {
                result.add(field.getName());
            }
        }
        return result;
    }

    public static File export(File file, List<?> list, String delimiter, boolean append) {
        File dir = new File(file.getParent());
        if (!(dir.exists() || dir.mkdirs())) {
            return null;
        }

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(file, append), CSVFormat.EXCEL.builder()
                .setDelimiter(delimiter)
                .setQuote('"')
                .setRecordSeparator("\r\n")
                .setIgnoreEmptyLines(false)
                .setAllowMissingColumnNames(true)
                .build())) {

            boolean shouldPrintHeader = !append || FileUtils.sizeOf(file) == 0;

            if (ObjectUtils.isNotEmpty(list)) {
                Object first = list.get(0);

                if (first instanceof Map<?, ?> map && shouldPrintHeader) {
                    for (Object key : map.keySet()) {
                        printer.print(key);
                    }
                    printer.println();
                }

                for (Object element : list) {
                    if (element instanceof Map<?, ?> mapElement) {
                        for (Object key : mapElement.keySet()) {
                            printer.print(mapElement.get(key));
                        }
                        printer.println();
                    } else {
                        Field[] fields = element.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            printer.print(field.get(element));
                        }
                        printer.println();
                    }
                }
            }
            return file;
        } catch (IllegalAccessException | IOException e) {
            Logger.error("ExportException", e.getMessage(), e);
            throw new InternalServerException(e.getMessage());
        }
    }

}