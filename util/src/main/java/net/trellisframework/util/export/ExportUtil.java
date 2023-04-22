package net.trellisframework.util.export;

import net.trellisframework.core.log.Logger;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ExportUtil {

    public static class PDF {

        public static File export(String path, List<?> list) {
            return export(path, list, String.valueOf(System.currentTimeMillis()));
        }

        public static File export(String path, List<?> list, String fileName) {
            return export(path, list, fileName ,Optional.ofNullable(Thread.currentThread().getContextClassLoader().getResource("arial.ttf")).map(URL::getPath).orElse("resources/arial.ttf"));
        }

        public static File export(String path, List<?> list, String fileName, String font) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".pdf")), list, font);
        }

        public static File export(File file, List<?> list, String font) {
            try {
                Font normal = FontFactory.getFont(font, BaseFont.IDENTITY_H, 8);
                File excel = Excel.export(file.getParent(), file.getName(), "Sheet1", list);
                FileInputStream input_document = new FileInputStream(excel);
                XSSFWorkbook my_xls_workbook = new XSSFWorkbook(input_document);
                XSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
                Iterator<Row> rowIterator = my_worksheet.iterator();
                Document iText_xls_2_pdf = new Document();
                PdfWriter.getInstance(iText_xls_2_pdf, new FileOutputStream(file));
                iText_xls_2_pdf.open();
                PdfPTable my_table = new PdfPTable(getColumnName(list).size());
                my_table.setWidthPercentage(100);
                my_table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                PdfPCell table_cell;
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        table_cell = new PdfPCell(new Phrase(cell.getStringCellValue(), normal));
                        my_table.addCell(table_cell);
                    }
                }
                iText_xls_2_pdf.add(my_table);
                iText_xls_2_pdf.close();
                input_document.close();
                return file;
            } catch (Exception e) {
                Logger.error("ExportToPdf", e.getMessage());
                return null;
            }
        }

    }

    public static class Excel {

        public static File export(String path, List<?> list) {
            return export(path, "Sheet1", list);
        }

        public static File export(String path, String sheetName, List<?> list) {
            return export(path, String.valueOf(System.currentTimeMillis()), sheetName, list);
        }

        public static File export(String path, String fileName, String sheetName, List<?> list) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".xlsx")), sheetName, list);
        }

        public static File export(File file, String sheet, List<?> list) {
            getWorkbook(file, sheet, list);
            return file;
        }

    }

    public static class CSV {

        public static File export(String path, List<?> list) {
            return export(path, String.valueOf(System.currentTimeMillis()), list);
        }

        public static File export(String path, String fileName, List<?> list) {
            return export(new File(FilenameUtils.concat(path, FilenameUtils.removeExtension(fileName) + ".csv")), list);
        }

        public static File export(File file, List<?> list) {
            try {
                String path = file.getParent();
                String filename = file.getName();
                String excelFullPath = FilenameUtils.concat(path, FilenameUtils.removeExtension(filename) + ".csv");
                File excelFile = new File(excelFullPath);
                Workbook wb = getWorkbook(excelFile, "Sheet1", list);
                if (wb == null)
                    return null;
                DataFormatter formatter = new DataFormatter();
                String fullPath = FilenameUtils.concat(path, FilenameUtils.removeExtension(filename) + ".csv");
                String dirPath = file.getParent();
                File dirFile = new File(dirPath);
                if (!dirFile.exists()) dirFile.mkdirs();
                PrintStream printStream = new PrintStream(file, StandardCharsets.UTF_8);
                for (Sheet sheet : wb) {
                    for (Row row : sheet) {
                        boolean firstCell = true;
                        for (Cell cell : row) {
                            if (!firstCell) printStream.print(',');
                            String text = formatter.formatCellValue(cell);
                            printStream.print(text);
                            firstCell = false;
                        }
                        printStream.println();
                    }
                }
                printStream.close();
                return file;
            } catch (Exception e) {
                Logger.error("ExportToExcel", e.getMessage());
                return null;
            }
        }
    }

    private static Workbook getWorkbook(File file, String sheetName, List<?> list) {
        try {
            if (list == null || list.isEmpty())
                return null;
            List<String> columnList = getColumnName(list);
            if (columnList == null || columnList.isEmpty())
                return null;
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(sheetName);
            int rowIndex = 0, columnIndex = 0;
            Row headerRow = sheet.createRow(rowIndex++);
            for (String column : columnList) {
                Cell cell = headerRow.createCell(columnIndex++);
                cell.setCellValue(column);
            }
            for (Object currentRow : list) {
                Row dataRow = sheet.createRow(rowIndex++);
                for (Field field : currentRow.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    columnIndex = columnList.indexOf(field.getName());
                    dataRow.createCell(columnIndex).setCellValue(String.valueOf(field.get(currentRow)));
                }
            }
            for (int i = 0; i < columnList.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            File dirFile = new File(file.getParent());
            if (!dirFile.exists()) dirFile.mkdirs();
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            return workbook;
        } catch (Exception e) {
            Logger.error("ExportToExcel", e.getMessage());
            return null;
        }
    }

    private static List<String> getColumnName(List<?> list) {
        List<String> result = new ArrayList<>();
        if (list == null || list.isEmpty())
            return result;
        Object first = list.get(0);
        for (Field field : first.getClass().getDeclaredFields()) {
            result.add(field.getName());
        }
        return result;
    }

}