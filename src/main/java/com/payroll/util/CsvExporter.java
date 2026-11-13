package com.payroll.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class CsvExporter {

    /**
     * Exports a Swing JTable directly to a CSV file.
     */
    public static void exportTableToCsv(JTable table, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        TableModel model = table.getModel();
        int columnCount = model.getColumnCount();
        int rowCount = model.getRowCount();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            // Write Header
            for (int col = 0; col < columnCount; col++) {
                writer.write(escapeCsv(model.getColumnName(col)));
                if (col < columnCount - 1) writer.write(",");
            }
            writer.newLine();

            // Write Data Rows
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < columnCount; col++) {
                    Object val = model.getValueAt(row, col);
                    writer.write(escapeCsv(val != null ? val.toString() : ""));
                    if (col < columnCount - 1) writer.write(",");
                }
                writer.newLine();
            }
        }
    }

    /**
     * Exports raw tabular data rows to a CSV file.
     */
    public static void exportDataToCsv(String[] headers, List<String[]> rows, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            // Write Headers
            for (int i = 0; i < headers.length; i++) {
                writer.write(escapeCsv(headers[i]));
                if (i < headers.length - 1) writer.write(",");
            }
            writer.newLine();

            // Write Rows
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    writer.write(escapeCsv(row[i]));
                    if (i < row.length - 1) writer.write(",");
                }
                writer.newLine();
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "\"\"";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
