package com.app.mirrorpage.app.model.file;

import java.util.ArrayList;
import java.util.List;

public class Prelim implements CsvModel {

    String[] colunas = {"PG", "EDICAO", "TIPO", "SUBTIPO", "ORI", "RETRANCA", "REP", "LOC", "tCab", "tVT", "tMat", "MODI", "APV", "TEMPO", "ASSUNTO"};

    private static List<String[]> linhas(String produto, String arquivo) {
        List<String[]> l = new ArrayList<>();

        l.add(new String[]{"", "", "", "", "", produto + "  -  " + arquivo, "", "", "", "", "", "", "", "00:00:00", ""});
        l.add(new String[]{"1", "", "", "", "", "", "", "", "00:00", "00:00", "00:00", "", "", "00:00:00", ""});
        l.add(new String[]{"2", "", "", "", "", "", "", "", "00:00", "00:00", "00:00", "", "", "00:00:00", ""});
        l.add(new String[]{"3", "", "", "", "", "", "", "", "00:00", "00:00", "00:00", "", "", "00:00:00", ""});
        l.add(new String[]{"4", "", "", "", "", "", "", "", "00:00", "00:00", "00:00", "", "", "00:00:00", ""});
        l.add(new String[]{"", "", "", "", "", "Encerramento", "", "", "", "", "", "", "", "00:00:00", ""});

        return l;
    }

    @Override
    public String generateCsv(String produto, String arquivo) {
        return CsvUtils.toCsv(colunas, linhas(produto, arquivo));
    }

    // Mantém construtor vazio por compatibilidade
    public Prelim() {
    }

}
