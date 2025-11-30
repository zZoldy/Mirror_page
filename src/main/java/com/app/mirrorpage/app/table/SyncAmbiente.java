package com.app.mirrorpage.app.table;

import com.app.mirrorpage.app.model.Tabela;
import com.app.mirrorpage.app.tema.ThemeApplier;
import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.ui.jInternal_tabela;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

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

        } catch (ApiClient.ApiHttpException e) {
            // Tratamento específico para erros HTTP da API
            if (e.isNotFound()) {
                // Erro 404
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(tabela,
                            "ERRO CRÍTICO:\nO arquivo não foi encontrado no servidor.\n"
                            + "Ele pode ter sido excluído ou movido.",
                            "Falha ao Salvar",
                            JOptionPane.ERROR_MESSAGE);
                });
            } else {
                // Outros erros (500, 403, etc)
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(tabela,
                            "Erro ao salvar: " + e.getMessage(),
                            "Erro de Servidor",
                            JOptionPane.WARNING_MESSAGE);
                });
            }
        } catch (Exception ex) {
            // Erros genéricos de conexão ou lógica
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(tabela,
                        "Erro inesperado: " + ex.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    /*──────── 2) Inserção de linha ────────*/
    public void onInsertRow(int afterRow, int col) {
        try {
            // 1. envia comando de inserção
            api.insertRow(sheetPath, afterRow, tabela.usuarioLogado);

            // 2. recarrega CSV completo ou diff
            String csv = api.loadSheet(sheetPath);
            SwingUtilities.invokeLater(() -> {
                aplicarCsvNoModelo(csv);

                int viewRow = tabela.tabela_news.convertRowIndexToView(afterRow);
                int viewCol = tabela.tabela_news.convertColumnIndexToView(col);
                tabela.selecao_line(viewRow, viewCol);
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

    public void onTrocarLine(int fromModelRow, int toModelRow, int col, String user) {
        try {
            api.moveRow(sheetPath, fromModelRow, toModelRow, user);

            String csv = api.loadSheet(sheetPath);

            SwingUtilities.invokeLater(() -> {
                aplicarCsvNoModelo(csv);

                int viewRow = tabela.tabela_news.convertRowIndexToView(toModelRow);
                int viewCol = tabela.tabela_news.convertColumnIndexToView(col);
                tabela.selecao_line(viewRow, viewCol);
            });

        } catch (ApiClient.ApiHttpException e) {
            // [CORREÇÃO] Captura o 409 (Lock) especificamente
            if (e.getStatusCode() == 409) {
                // Mensagem vinda do servidor: "Movimento bloqueado: A linha X está em uso..."
                String msgErro = e.getMessage();

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(tabela,
                            msgErro,
                            "Ação Bloqueada",
                            JOptionPane.WARNING_MESSAGE);
                });
            } else {
                e.printStackTrace();
            }

            // [IMPORTANTE] Como o movimento falhou, precisamos desfazer a alteração visual local.
            // Recarregamos o arquivo original do servidor.
            recarregarParaDesfazerErro();

        } catch (Exception ex) {
            ex.printStackTrace();
            // Erro genérico também força recarga para garantir integridade
            recarregarParaDesfazerErro();
        }
    }

    // Método auxiliar para evitar duplicação de código no catch
    private void recarregarParaDesfazerErro() {
        try {
            String csvOriginal = api.loadSheet(sheetPath);
            SwingUtilities.invokeLater(() -> aplicarCsvNoModelo(csvOriginal));
        } catch (Exception loadEx) {
            System.err.println("Erro ao tentar restaurar tabela após falha: " + loadEx.getMessage());
        }
    }

    public void onDeleteLine(int row, int col) {
        try {
            api.deleteRow(sheetPath, row, tabela.usuarioLogado);
            String csv = api.loadSheet(sheetPath);

            SwingUtilities.invokeLater(() -> {
                aplicarCsvNoModelo(csv);
                int viewRow = tabela.tabela_news.convertRowIndexToView(row);
                int viewCol = tabela.tabela_news.convertColumnIndexToView(col);
                tabela.selecao_line(viewRow, viewCol);
            });

        } catch (Exception e) {

            new Thread(() -> {
                String msg = e.getMessage();
                if (msg != null && (msg.contains("HTTP 409") || msg.contains("Linha bloqueada"))) {
                    String aviso = "Esta linha está sendo editada por outro usuário.";
                    if (msg.contains("Linha bloqueada")) {
                        try {
                            aviso = msg.substring(msg.indexOf("Linha bloqueada"));
                        } catch (Exception ex) {
                        }
                    }
                    final String avisoFinal = aviso;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(tabela,
                                "Não foi possível excluir:\n" + avisoFinal,
                                "Ação Bloqueada",
                                JOptionPane.WARNING_MESSAGE);
                    });
                } else {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(tabela, "Erro técnico: " + e.getMessage());
                    });
                }

            }).start();
        }

    }

}
