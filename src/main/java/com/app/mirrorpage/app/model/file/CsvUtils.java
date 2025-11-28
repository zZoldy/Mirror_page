package com.app.mirrorpage.app.model.file;

import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static String toCsv(String[] header, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(join(header)).append('\n');
        for (String[] r : rows) {
            sb.append(join(r)).append('\n');
        }
        return sb.toString();
    }

    public static String toCsvSemHeader(java.util.List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            sb.append(join(r));
            if (i < rows.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String join(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(escape(arr[i]));
            if (i < arr.length - 1) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            s = "";
        }
        boolean needQuotes = s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String v = s.replace("\"", "\"\"");
        return needQuotes ? "\"" + v + "\"" : v;
    }
}
