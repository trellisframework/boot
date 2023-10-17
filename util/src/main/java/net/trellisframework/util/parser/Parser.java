package net.trellisframework.util.parser;

import net.trellisframework.http.exception.InternalServerException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Parser {

    public static List<Map<String, Object>> parse(File file, char delimiter) {
        try {
            FileReader fileReader = new FileReader(file);
            CSVParser csvParser = new CSVParser(fileReader, CSVFormat.newFormat(delimiter).builder().setHeader().setQuote('"').setSkipHeaderRecord(true).build());
            List<Map<String, Object>> records = parse(csvParser);
            csvParser.close();
            fileReader.close();
            return records;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new InternalServerException("Failed to export records from file: " + ex.getMessage());
        }
    }

    private static List<Map<String, Object>> parse(CSVParser parser) {
        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, String> modifiedHeaders = new HashMap<>();
        for (String header : parser.getHeaderNames()) {
            modifiedHeaders.put(header, sanitize(header));
        }
        for (CSVRecord csvRecord : parser) {
            Map<String, Object> item = new HashMap<>();
            for (String header : parser.getHeaderNames()) {
                String value = csvRecord.get(header);
                item.put(modifiedHeaders.get(header), value);
            }
            records.add(item);
        }
        return records;
    }

    private static String sanitize(String header) {
        return header.replace(" ", "_").toLowerCase();
    }
}
