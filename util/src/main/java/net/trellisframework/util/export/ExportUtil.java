package net.trellisframework.util.export;

import com.google.common.collect.Lists;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import net.trellisframework.core.log.Logger;
import net.trellisframework.http.exception.InternalServerException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExportUtil {

    public static class PDF {

        public static File export(String path, List<?> list) {
            return export(path, list, false);
        }

        public static File export(String path, List<?> list, boolean append) {
            return export(path, list, String.valueOf(System.currentTimeMillis()), append);
        }

        public static File export(String path, List<?> list, String fileName, boolean append) {
            return export(path, list, fileName, Optional.ofNullable(Thread.currentThread().getContextClassLoader().getResource("arial.ttf")).map(URL::getPath).orElse("resources/arial.ttf"), append);
        }

        public static File export(String path, List<?> list, String fileName, String font, boolean append) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".pdf")), list, font, append);
        }

        public static File export(File file, List<?> list, String font) {
            return export(file, list, font, false);
        }

        public static File export(File file, List<?> list, String font, boolean append) {
            try {
                String sheet_name = "Sheet1";
                Font normal = FontFactory.getFont(font, BaseFont.IDENTITY_H, 8);
                File excel = Excel.export(file.getParent(), file.getName(), sheet_name, list, append);
                FileInputStream input_document = new FileInputStream(excel);
                XSSFWorkbook workbook = new XSSFWorkbook(input_document);
                XSSFSheet sheet = workbook.getSheet(sheet_name);
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                PdfPTable pdfPTable = new PdfPTable(getColumnName(list).size());
                pdfPTable.setWidthPercentage(100);
                pdfPTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                for (Row row : Lists.newArrayList(sheet.iterator())) {
                    for (Cell cell : Lists.newArrayList(row.cellIterator())) {
                        pdfPTable.addCell(new PdfPCell(new Phrase(cell.getStringCellValue(), normal)));
                    }
                }
                document.add(pdfPTable);
                document.close();
                input_document.close();
                return file;
            } catch (Exception e) {
                Logger.error("ExportToPdf", e.getMessage(), e);
                throw new InternalServerException(e.getMessage());
            }
        }

    }

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
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".xlsx")), sheetName, list, append);
        }

        public static File export(File file, String sheet, List<?> list) {
            return export(file, sheet, list, false);
        }

        public static File export(File file, String sheet, List<?> list, boolean append) {
            getWorkbook(file, sheet, list, append);
            return file;
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
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".csv")), list, append);
        }

        public static File export(File file, List<?> list) {
            return export(file, list, false);
        }

        public static File export(File file, List<?> list, boolean append) {
            return fromWorkbook(file, getWorkbook(new File(FilenameUtils.concat(file.getParent(), FilenameUtils.removeExtension(file.getName()) + ".xlsx")), "Result", list, append), "Result", CSVFormat.DEFAULT);
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
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".tsv")), list, append);
        }

        public static File export(File file, List<?> list) {
            return export(file, list, false);
        }

        public static File export(File file, List<?> list, boolean append) {
            return fromWorkbook(file, getWorkbook(new File(FilenameUtils.concat(file.getParent(), FilenameUtils.removeExtension(file.getName()) + ".xlsx")), "Result", list, append), "Result", CSVFormat.TDF);
        }

        public static File fromExcel(File file, File excel, String sheetName) {
            return ExportUtil.fromExcel(file, excel, sheetName, CSVFormat.TDF);
        }
    }


    private static Workbook getWorkbook(File file, String sheetName, List<?> list, boolean append) {
        try {
            if (list == null || list.isEmpty())
                return null;
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


}