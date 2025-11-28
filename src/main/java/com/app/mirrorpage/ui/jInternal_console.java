
package com.app.mirrorpage.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class jInternal_console extends javax.swing.JInternalFrame {

    public enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    private static volatile jInternal_console INSTANCE;

    public static jInternal_console getInstance() {
        if (INSTANCE == null) {
            synchronized (jInternal_console.class) {
                if (INSTANCE == null) {
                    INSTANCE = new jInternal_console();
                }
            }
        }
        return INSTANCE;
    }

    public void append(Level level, String message) {
        append(level, message, null);
    }

    public void append(Level level, String message, Throwable t) {
        if (message == null) {
            message = "";
        }
        final String msg = (t == null) ? message : message + "\n" + stackToString(t);
        final LogEntry e = new LogEntry(new Date(), level, Thread.currentThread().getName(), msg);

        // Armazena no buffer e renderiza respeitando filtros
        SwingUtilities.invokeLater(() -> {
            buffer.add(e);
            trimBufferIfNeeded();
            if (passesFilters(e)) {
                appendStyled(e);
            }
        });
    }

    /**
     * Handler p/ integrar ao java.util.logging
     */
    public Handler asJavaUtilHandler() {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                Level lvl = mapJulLevel(record.getLevel());
                String msg = getFormatter() != null ? getFormatter().format(record) : record.getMessage();
                append(lvl, msg, record.getThrown());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
    }

    // --------- Implementação ---------
    private static final int MAX_BUFFER = 5000; // linhas protegendo memória
    private final ArrayDeque<LogEntry> buffer = new ArrayDeque<>(Math.min(MAX_BUFFER, 1024));
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    private final JTextPane textPane = new JTextPane();
    private final StyledDocument doc = textPane.getStyledDocument();

    private final JComboBox<Level> cbLevel = new JComboBox<>(Level.values());
    private final JTextField txtBusca = new JTextField();
    private final JCheckBox chkAutoscroll = new JCheckBox("Auto‑scroll", true);
    private final JButton btnLimpar = new JButton("Limpar");
    private final JButton btnExportar = new JButton("Exportar...");
    private final JButton btnCopiar = new JButton("Copiar seleção");

    private jInternal_console() {
        super("Log", true, false, false, false);
        initComponents();
        setBorder(BorderFactory.createLineBorder(new Color(100, 150, 220), 1, true));

// Remove apenas o título (barra de cima), mantendo a moldura
        ((javax.swing.plaf.basic.BasicInternalFrameUI) this.getUI()).setNorthPane(null);

        setFrameIcon(UIManager.getIcon("OptionPane.informationIcon"));
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        // Fonte monoespaçada
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textPane.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Estilos por nível
        initStyles();

        // Top bar
        JPanel top = new JPanel(new BorderLayout(8, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JLabel lblLevel = new JLabel("Nível:");
        JLabel lblBusca = new JLabel("Buscar:");

        Dimension txtBuscaDim = new Dimension(240, 28);
        txtBusca.setPreferredSize(txtBuscaDim);
        txtBusca.setToolTipText("Filtra por texto (regex simples). Enter para aplicar.");

        left.add(lblLevel);
        left.add(cbLevel);
        left.add(lblBusca);
        left.add(txtBusca);

        right.add(chkAutoscroll);
        right.add(btnCopiar);
        right.add(btnLimpar);
        right.add(btnExportar);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(textPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Listeners
        cbLevel.addActionListener(e -> rebuildFromBuffer());
        txtBusca.addActionListener(e -> rebuildFromBuffer());
        txtBusca.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // aplica ao digitar (leve)
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    txtBusca.setText("");
                    rebuildFromBuffer();
                } else if (!e.isActionKey()) {
                    // debounce leve
                    SwingUtilities.invokeLater(() -> rebuildFromBuffer());
                }
            }
        });

        btnLimpar.addActionListener(e -> {
            buffer.clear();
            clearDoc();
        });

        btnExportar.addActionListener(e -> exportar());
        btnCopiar.addActionListener(e -> textPane.copy());

        // Atalho Ctrl+L para limpar
        textPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "limpar");
        textPane.getActionMap().put("limpar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnLimpar.doClick();
            }
        });
    }

    // Reconstroi a visão aplicando filtros/busca sobre o buffer
    private void rebuildFromBuffer() {
        clearDoc();
        for (LogEntry e : buffer) {
            if (passesFilters(e)) {
                appendStyled(e);
            }
        }
    }

    private boolean passesFilters(LogEntry e) {
        Level nivelFiltro = (Level) cbLevel.getSelectedItem();
        String busca = txtBusca.getText();
        boolean nivelOk = compareLevel(e.level, nivelFiltro) >= 0;
        boolean buscaOk = (busca == null || busca.isBlank())
                || containsIgnoreCase(e.message, busca)
                || containsIgnoreCase(e.thread, busca);
        return nivelOk && buscaOk;
    }

    // INFO passa se filtro <= INFO, etc.
    private int compareLevel(Level a, Level b) {
        return Integer.compare(levelOrdinal(a), levelOrdinal(b));
    }

    private int levelOrdinal(Level l) {
        // ordem gravidade
        return switch (l) {
            case TRACE ->
                0;
            case DEBUG ->
                1;
            case INFO ->
                2;
            case WARN ->
                3;
            case ERROR ->
                4;
        };
    }

    private void appendStyled(LogEntry e) {
        try {
            String ts = sdf.format(e.timestamp);
            String header = String.format("%s %-5s [%s] ", ts, e.level, e.thread);
            Style style = doc.getStyle(styleNameFor(e.level));

            doc.insertString(doc.getLength(), header, style);
            doc.insertString(doc.getLength(), e.message, doc.getStyle("body"));
            if (!e.message.endsWith("\n")) {
                doc.insertString(doc.getLength(), "\n", doc.getStyle("body"));
            }

            if (chkAutoscroll.isSelected()) {
                textPane.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    private void clearDoc() {
        textPane.setText("");
    }

    private void trimBufferIfNeeded() {
        while (buffer.size() > MAX_BUFFER) {
            buffer.pollFirst();
        }
    }

    private boolean containsIgnoreCase(String hay, String needle) {
        if (hay == null || needle == null) {
            return false;
        }
        return hay.toLowerCase().contains(needle.toLowerCase());
    }

    // ----- estilos -----
    private void initStyles() {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style body = doc.addStyle("body", def);
        StyleConstants.setFontFamily(body, Font.MONOSPACED);
        StyleConstants.setFontSize(body, 13);

        Style trace = doc.addStyle("TRACE", body);
        StyleConstants.setForeground(trace, new Color(130, 130, 130));

        Style debug = doc.addStyle("DEBUG", body);
        StyleConstants.setForeground(debug, new Color(80, 80, 160));

        Style info = doc.addStyle("INFO", body);
        StyleConstants.setForeground(info, new Color(30, 120, 30));

        Style warn = doc.addStyle("WARN", body);
        StyleConstants.setForeground(warn, new Color(180, 120, 0));

        Style error = doc.addStyle("ERROR", body);
        StyleConstants.setForeground(error, new Color(180, 0, 0));
        StyleConstants.setBold(error, true);
    }

    private String styleNameFor(Level l) {
        return l.name();
    }

    // ----- util -----
    private static String stackToString(Throwable t) {
        StringWriter sw = new StringWriter(2048);
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static Level mapJulLevel(java.util.logging.Level lv) {
        if (lv == null) {
            return Level.INFO;
        }
        int v = lv.intValue();
        if (v >= java.util.logging.Level.SEVERE.intValue()) {
            return Level.ERROR;
        }
        if (v >= java.util.logging.Level.WARNING.intValue()) {
            return Level.WARN;
        }
        if (v >= java.util.logging.Level.INFO.intValue()) {
            return Level.INFO;
        }
        if (v >= java.util.logging.Level.FINE.intValue()) {
            return Level.DEBUG;
        }
        return Level.TRACE;
    }

    // ----- modelo -----
    private record LogEntry(Date timestamp, Level level, String thread, String message) {

        LogEntry    {
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(level);
            if (thread == null) {
                thread = "";
            }
            if (message == null) {
                message = "";
            }
        }
    }

    // ----- exportação -----
    private void exportar() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar log");
        fc.setFileFilter(new FileNameExtensionFilter("Arquivo de texto (*.log, *.txt)", "log", "txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().contains(".")) {
                f = new File(f.getParentFile(), f.getName() + ".log");
            }
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8))) {
                for (LogEntry e : buffer) {
                    // respeita filtros atuais
                    if (!passesFilters(e)) {
                        continue;
                    }
                    bw.write(String.format("%s %-5s [%s] %s",
                            sdf.format(e.timestamp), e.level, e.thread, e.message));
                    if (!e.message.endsWith("\n")) {
                        bw.write("\n");
                    }
                }
                JOptionPane.showMessageDialog(this, "Log exportado para:\n" + f.getAbsolutePath(),
                        "Exportar", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Falha ao exportar: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 394, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 274, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
