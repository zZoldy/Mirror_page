/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.app.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class Tabela {

    // Conjunto de linhas com erros detectados na coluna de tempo (coluna 10)
    public static Set<Integer> celulasComErro = new HashSet<>();

    public Tabela() {

    }

    @Override
    public String toString() {
        return "";
    }

    public static void model_padrao(JTable table) {
        // Define editores de célula personalizados para colunas específicas
        int[] colunasComEditor = {8, 9, 13};
        for (int col : colunasComEditor) {
            // Aplica editor de tempo (HH:mm ou HH:mm:ss conforme coluna)
            if (col < table.getColumnCount()) {
                table.getColumnModel().getColumn(col).setCellEditor(new TimeCellEditor(col == 13 ? "##:##:##" : "##:##"));
            }
        }
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);

        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(40);

        table.setFillsViewportHeight(true);
        table.setDoubleBuffered(true); // garantir pintura rápida
        //table.setOpaque(true);
    }

    public static DefaultTableModel modelos(DefaultTableModel model) {

        model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                int ultimaLinha = getRowCount() - 1;

                // Verifica se é a primeira OU a última linha
                if (row == 0) {
                    return column == 5 || column == 13;
                }

                if (row == ultimaLinha) {
                    return column == 5 || column == 13;
                }

                // Outras linhas, tudo exceto colunas 10 e 13
                return column != 1 && column != 10 && column != 13;
            }
        };

        return model;
    }

    public static void renderer_header_table(JTable tabela, Font font, Color background, Color foreground) {
        JTableHeader header = tabela.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                label.setBackground(background);  // Cor de fundo
                label.setForeground(foreground);             // Cor do texto
                label.setFont(font);
                label.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setOpaque(true); // Necessário para aplicar a cor

                return label;
            }
        });
    }

    public static void renderer_line_table(JTable tabela, Font font, Color background, Color foreground, Color background_2, Color foreground_2, Color background_selected_1, Color foreground_selected_1, Color background_selected_2, Color foreground_selected_2) {
        tabela.setOpaque(false);
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                // Se for coluna 5, usa JTextArea
                // Demais colunas usam JLabel padrão
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                label.setFont(font);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);

                renderer_lines(isSelected, label, row, background, foreground, background_2, foreground_2,
                        background_selected_1, foreground_selected_1, background_selected_2, foreground_selected_2);
                error_calc_tMat(isSelected, label, column, row);

                return label;
            }
        });
    }

    static void renderer_lines(boolean isSelected, Component cell, int row, Color background_1, Color foreground_1, Color background_2, Color foreground_2, Color background_selected_1, Color foreground_selected_1, Color background_selected_2, Color foreground_selected_2) {
        if (!isSelected) {
            if (row % 2 == 0) {
                cell.setBackground(background_1); // linhas pares
                cell.setForeground(foreground_1);
            } else {
                cell.setBackground(background_2); // linhas ímpares
                cell.setForeground(foreground_2);
            }
        } else {
            if (row % 2 == 0) {
                cell.setBackground(background_selected_1); // Selecionado linha par
                cell.setForeground(foreground_selected_1);
            } else {
                cell.setBackground(background_selected_2);// Selecionado linha ímpar
                cell.setForeground(foreground_selected_2);
            }
        }
    }

    static void error_calc_tMat(boolean isSelected, Component cell, int column, int row) {
        // Só mexe nas cores se a célula NÃO estiver selecionada
        if (column == 10) {
            if (celulasComErro.contains(row)) {
                cell.setBackground(Color.RED);// Marca com vermelho
            }
        }

    }

    public static void verificarErrosDeTempo(JTable tabela) {
        DefaultTableModel model = (DefaultTableModel) tabela.getModel();

        celulasComErro.clear(); // zera antes

        int rowCount = model.getRowCount();

        for (int row = 0; row < rowCount; row++) {

            Object valTCab = model.getValueAt(row, 8);  // coluna 8
            Object valTVT = model.getValueAt(row, 9);  // coluna 9

            int tCabSeg = parseTempo(valTCab != null ? valTCab.toString() : "");
            int tVTSeg = parseTempo(valTVT != null ? valTVT.toString() : "");

            // Se QUALQUER um estourar 59:59
            if (tCabSeg > 3599 || tVTSeg > 3599) {
                celulasComErro.add(row);
            } else {
                celulasComErro.remove(row);
            }
        }

        tabela.repaint(); // pra renderer repintar com as cores certas
    }

    public static int parseTempo(String tempo) {
        if (tempo == null || tempo.isBlank()) {
            return 0;
        }

        tempo = tempo.trim();

        // Esperado: "MM:SS"
        String[] partes = tempo.split(":");
        if (partes.length != 2) {
            return 0;
        }

        try {
            int min = Integer.parseInt(partes[0]);
            int sec = Integer.parseInt(partes[1]);
            return min * 60 + sec;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void ajustarLarguraColuna(JTable tabela) {
        TableColumnModel columnModel = tabela.getColumnModel();

        for (int col = 0; col < tabela.getColumnCount(); col++) {
            int larguraMax = 50;
            boolean encontrouConteudo = false;

            // Verifica conteúdo das células
            for (int row = 0; row < tabela.getRowCount(); row++) {
                Object value = tabela.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    TableCellRenderer renderer = tabela.getCellRenderer(row, col);
                    Component comp = tabela.prepareRenderer(renderer, row, col);
                    larguraMax = Math.max(larguraMax, comp.getPreferredSize().width + 40);
                    encontrouConteudo = true;
                }
            }

            // Se não encontrou conteúdo, usa o cabeçalho
            if (!encontrouConteudo) {
                TableCellRenderer headerRenderer = tabela.getTableHeader().getDefaultRenderer();
                Component compHeader = headerRenderer.getTableCellRendererComponent(
                        tabela, tabela.getColumnName(col), false, false, 0, col
                );
                larguraMax = Math.max(larguraMax, compHeader.getPreferredSize().width + 20);
            }

            // Aplica largura
            columnModel.getColumn(col).setPreferredWidth(larguraMax);
        }
    }

    public static DefaultTableModel modeloDeCsv(String csvTexto) {
        DefaultTableModel modelo = modelos(new DefaultTableModel());

        if (csvTexto == null || csvTexto.isBlank()) {
            return modelo;
        }

        String separador = ";";

        String[] linhas = csvTexto.split("\r?\n", -1);

        if (linhas.length == 0) {
            return modelo;
        }

        // Primeira linha = cabeçalho
        String[] header = linhas[0].split(separador, -1);
        modelo.setColumnIdentifiers(header);

        // Demais linhas = dados
        for (int i = 1; i < linhas.length; i++) {
            String linha = linhas[i];
            if (linha.isEmpty()) {
                continue;
            }

            String[] cols = linha.split(separador, -1);
            if (cols.length < header.length) {
                cols = java.util.Arrays.copyOf(cols, header.length);
            }
            modelo.addRow(cols);
        }

        return modelo;
    }
}
