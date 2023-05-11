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
import net.trellisframework.http.exception.NotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
            try {
                Workbook wb = getWorkbook(new File(FilenameUtils.concat(file.getParent(), FilenameUtils.removeExtension(file.getName()) + ".xlsx")), "Sheet1", list, append);
                if (wb == null)
                    return null;
                File dir = new File(file.getParent());
                if (!(dir.exists() || dir.mkdirs()) || !(file.exists() || file.createNewFile()))
                    return null;
                DataFormatter formatter = new DataFormatter();
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
                Logger.error("ExportToExcel", e.getMessage(), e);
                throw new InternalServerException(e.getMessage());
            }
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
                for (Field field : currentRow.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    columnIndex = columnList.indexOf(field.getName());
                    dataRow.createCell(columnIndex).setCellValue(String.valueOf(field.get(currentRow)));
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