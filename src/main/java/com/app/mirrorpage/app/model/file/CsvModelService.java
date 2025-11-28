package com.app.mirrorpage.app.model.file;

import com.app.mirrorpage.client.net.ApiClient;

public class CsvModelService {

    private final ApiClient api;

    public CsvModelService(ApiClient api) {
        this.api = api;
    }

    public void salvar(CsvModelType tipo, String produtoDir, String nomeArquivo, String produto, String arquivo) throws Exception {
        String dir = normalize(produtoDir);
        String dst = dir + "/" + (nomeArquivo == null || nomeArquivo.isBlank() ? tipo.defaultFileName() : nomeArquivo);
        String content = tipo.model().generateCsv(produto, arquivo);
        api.saveFile(dst, content);     // POST /api/tree/file { path, content }
    }

    public void salvar(CsvModelType tipo, String produtoDir, String produto, String arquivo) throws Exception {
        salvar(tipo, produtoDir, null, produto, arquivo);
    }

    private static String normalize(String p) {
        if (p == null || p.isBlank()) {
            return "/";
        }
        if (p.equals("/Produtos")) {
            return "/";
        }
        if (p.startsWith("/Produtos/")) {
            p = p.substring("/Produtos".length());
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p.replaceAll("/{2,}", "/");
    }

}
