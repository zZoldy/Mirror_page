package com.app.mirrorpage.app.table;

import com.app.mirrorpage.app.model.Tabela;
import com.app.mirrorpage.app.tema.ThemeApplier;
import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.ui.jInternal_tabela;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Z D K
 */
public class SyncAmbiente {

    private final jInternal_tabela tabela;
    private final ApiClient api; // seu cliente HTTP
    private final String sheetPath; // ex: "/BDBR/Prelim.csv"

    public SyncAmbiente(jInternal_tabela tabela, ApiClient api, String sheetPath) {
        this.tabela = tabela;
        this.api = api;
        this.sheetPath = sheetPath;
    }

    /*──────── 1) Edição de célula ────────*/
    public void onCellEdit(int row, int col, String value) {
        try {
            // 2. envia para o servidor
            api.saveCell(sheetPath, row, col, value);

        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: tratar erro, exibir mensagem etc.
        }
    }

    /*──────── 2) Inserção de linha ────────*/
    public void onInsertRow(int afterRow) {
        try {
            // 1. envia comando de inserção
            api.insertRow(sheetPath, afterRow);

            // 2. recarrega CSV completo ou diff
            String csv = api.loadSheet(sheetPath);
            SwingUtilities.invokeLater(() -> {
                aplicarCsvNoModelo(csv);
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void aplicarCsvNoModelo(String csvTexto) {
        // Usa sua lógica já existente de montar o modelo a partir do CSV
        DefaultTableModel novoModelo = Tabela.modeloDeCsv(csvTexto);
        switch (ThemeApplier.tema) {
            case "DEFAULT" ->
                ThemeApplier.jTable_default(tabela.tabela_news);
            case "DARK" ->
                ThemeApplier.jTabel_dark(tabela.tabela_news);
            case "STAR_LIGHT" ->
                ThemeApplier.jTable_star_light(tabela.tabela_news);
        }

        tabela.tabela_news.setModel(novoModelo);
        // Se quiser manter referências:
        // this.model = novoModelo;

        Tabela.ajustarLarguraColuna(tabela.tabela_news);
        Tabela.model_padrao(tabela.tabela_news); // <── AQUI

        tabela.recalcularTodos_tMat();
        tabela.tabela_tempo();
    }

    public void onTrocarLine(int fromModelRow, int toModelRow, String user) {
        try {
            api.moveRow(sheetPath, fromModelRow, toModelRow, user);

            String csv = api.loadSheet(sheetPath);

            SwingUtilities.invokeLater(() -> aplicarCsvNoModelo(csv));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onDeleteLine(int row) {
        try {
            api.deleteRow(sheetPath, row);
            String csv = api.loadSheet(sheetPath);

            SwingUtilities.invokeLater(() -> aplicarCsvNoModelo(csv));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
