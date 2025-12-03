/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.mirrorpage.ui.table;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.text.BreakIterator;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Lauda extends JPanel {

    public JTextPane txtTexto;
    private JLabel lblInfo;
    private JLabel lblTitulo;
    public JLabel lblUsuario;
    private final int WPM_BASE = 150; // Velocidade de leitura

    public Lauda() {
        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        this.setName("pn_lauda");

        // 1. Barra Superior (Título e Info)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setName("pn_superior_lauda");
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        lblTitulo = new JLabel("LINHA 00 - ASSUNTO");
        lblTitulo.setName("lauda_titulo");
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        lblUsuario = new JLabel("Usuário - Editando");
        lblUsuario.setName("lauda_usuario");
        lblUsuario.setHorizontalAlignment(SwingConstants.RIGHT);

        lblInfo = new JLabel("0:00 • 0 palavras");
        lblInfo.setName("lauda_info");

        topPanel.add(lblInfo, BorderLayout.WEST);

        topPanel.add(lblTitulo, BorderLayout.CENTER);

        topPanel.add(lblUsuario, BorderLayout.EAST);

        // 2. Editor de Texto
        txtTexto = new JTextPane();
        txtTexto.setName("txt_lauda");
        txtTexto.setFont(new Font("Monospaced", Font.PLAIN, 16)); // Fonte monoespaçada ajuda na leitura
        txtTexto.setMargin(new Insets(10, 10, 10, 10)); // Margem interna para o texto não colar na borda

        // Adiciona listener para calcular WPM enquanto digita
        txtTexto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                calcularWpm();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                calcularWpm();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                calcularWpm();
            }
        });

        // 3. Montagem
        this.add(topPanel, BorderLayout.NORTH);
        this.add(new JScrollPane(txtTexto), BorderLayout.CENTER);
    }

    // Método para configurar a lauda ao abrir
    public void abrirLauda(String titulo, String conteudoInicial, String usuario) {
        lblTitulo.setText(titulo);
        lblUsuario.setText(usuario + " - Editando");
        txtTexto.setText(conteudoInicial);
        txtTexto.setCaretPosition(txtTexto.getDocument().getLength()); // Cursor no final
        txtTexto.requestFocusInWindow(); // Foco para digitar
        calcularWpm(); // Atualiza contagem inicial
    }

    public JTextPane getEditor() {
        return txtTexto;
    }

    // --- Lógica de Negócio Visual (WPM) que você já tinha ---
    private void calcularWpm() {
        String texto = txtTexto.getText();
        int palavras = contarPalavras(texto);

        double tempoMin = (palavras / (double) WPM_BASE);
        int tempoSeg = (int) Math.round(tempoMin * 60.0);

        String tempoFormatado = String.format("%d:%02d", tempoSeg / 60, tempoSeg % 60);
        lblInfo.setText("Tempo: " + tempoFormatado + " • " + palavras + " palavras");
    }

    private int contarPalavras(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        BreakIterator it = BreakIterator.getWordInstance(new Locale("pt", "BR"));
        it.setText(texto);
        int count = 0;
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String token = texto.substring(start, end).trim();
            if (!token.isEmpty() && Character.isLetterOrDigit(token.codePointAt(0))) {
                count++;
            }
        }
        return count;
    }
}
