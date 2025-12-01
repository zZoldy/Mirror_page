package com.app.mirrorpage.app.tema;

import com.app.mirrorpage.client.dto.ValorDto;
import com.app.mirrorpage.client.net.ApiClient;
import javax.swing.JFrame;
import javax.swing.JTable;

public class TemaSyncClient {

    private final ApiClient api;

    public TemaSyncClient(ApiClient api) {
        this.api = api;
    }

    public void aplicarTemaDoServidor(JFrame root) {
        try {
            System.out.println("[TemaSync] Buscando tema no servidor...");

            ValorDto dto = api.get("/api/me/tema", ValorDto.class); // { "value": "..." }

            if (dto == null) {
                System.out.println("[TemaSync] DTO nulo, aplicando DEFAULT");
                ThemeManager.get().setTema(TemaNome.DEFAULT);
                ThemeApplier.apply(root.getContentPane(), TemaNome.DEFAULT);
                return;
            }

            String valor = dto.value();
            System.out.println("[TemaSync] Valor recebido do servidor: " + valor);

            TemaNome nome = mapear(valor); // DEFAULT / DARK / STAR_LIGHT (qualquer case)
            System.out.println("[TemaSync] Mapeado para enum: " + nome);

            ThemeManager.get().setTema(nome);
            ThemeApplier.apply(root.getContentPane(), nome);

            System.out.println("[TemaSync] Tema aplicado no login: " + nome);

        } catch (Exception e) {
            System.out.println("[TemaSync] ERRO ao buscar tema do servidor, aplicando DEFAULT");
            e.printStackTrace();
            ThemeManager.get().setTema(TemaNome.DEFAULT);
            ThemeApplier.apply(root.getContentPane(), TemaNome.DEFAULT);
        }
    }

    public void aplicarTemaTable(JTable table) {
        TemaNome nome = ThemeManager.get().temaAtual();
        ThemeApplier.apply_table(table, nome);
    }

    public void aplicarTemaRoot(JFrame root) {
        TemaNome nome = ThemeManager.get().temaAtual();
        ThemeApplier.apply(root, nome);
    }

    public void aplicarTemaGeral(JFrame root, JTable table) {
        aplicarTemaRoot(root);
        if (table != null) {
            aplicarTemaTable(table);
        }

    }

    public void onTrocaTema(String novoValor, JFrame root) {
        // 1) aplica local imediatamente
        TemaNome nome = mapear(novoValor);
        ThemeManager.get().setTema(nome);
        ThemeApplier.apply(root, nome);

        new Thread(() -> {
            try {
                System.out.println("[TemaSync] Enviando tema para servidor: " + novoValor);
                api.put("/api/me/tema", new ValorDto(novoValor));
                System.out.println("[TemaSync] Tema salvo com sucesso no servidor.");
            } catch (Exception ex) {
                System.out.println("[TemaSync] ERRO ao salvar tema no servidor!");
                ex.printStackTrace();
            }
        }).start();
    }

    public String tema_applied() {
        return ThemeManager.get().temaAtual().toString();
    }

    private TemaNome mapear(String valor) {
        if (valor == null) {
            return TemaNome.DEFAULT;
        }
        return switch (valor) {
            case "DEFAULT" ->
                TemaNome.DEFAULT;
            case "DARK" ->
                TemaNome.DARK;
            case "STAR_LIGHT" ->
                TemaNome.STAR_LIGHT;
            // Trate "%" como DEFAULT (ou como você quiser)
            default ->
                TemaNome.DEFAULT;
        };
    }
}
