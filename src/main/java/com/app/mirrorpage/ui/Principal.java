package com.app.mirrorpage.ui;

import com.app.mirrorpage.app.framework.Funcoes;
import com.app.mirrorpage.app.framework.Log;
import com.app.mirrorpage.app.framework.Session;
import com.app.mirrorpage.app.listener.Cliente_listener;
import com.app.mirrorpage.app.model.Image_panel;
import com.app.mirrorpage.app.model.Tabela;
import com.app.mirrorpage.app.tema.TemaSyncClient;
import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.client.net.AppSocketClient;
import com.app.mirrorpage.client.ui.tree.FsTree;
import com.app.mirrorpage.ui.config.Configuracoes;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class Principal extends javax.swing.JFrame implements Cliente_listener {

    public jInternal_tabela tabela;

    private final Session session;
    private FsTree fsTree;
    private ApiClient api;

    ImageIcon icon = new ImageIcon(getClass().getResource("/icons/logo.png"));

// Campos:
    private final JPanel glass = new JPanel(new java.awt.GridBagLayout());
    private volatile boolean offlineShown = false;
    TemaSyncClient temaSync;
    private AppSocketClient appSocket;

    Debug d = new Debug(this);

    /**
     * Creates new form Principal
     *
     * @param session
     */
    public Principal(Session session) {
        this.session = session;
        this.api = new ApiClient(session.baseUrl(), session.accessToken(), session::refreshToken, this::onTokenRefreshed);
        this.temaSync = new TemaSyncClient(api);
        initComponents();

        Funcoes.relogio(horario_atual);

        lbl_show_user.setText("Usuário: " + session.username());

        // ((javax.swing.GroupLayout) pn_logo.getLayout()).setHonorsVisibility(false);
        clear_desktop();

        installBlockingGlassPane();

        // >>> GARANTA LAYOUT DO CONTÊINER DA ÁRVORE
        pn_tree.setLayout(new BorderLayout());

        fsTree = new FsTree(session, api);
        fsTree.setName("fsTree");

        fsTree.addOpenListener(path -> {
            System.out.println("Selecionado: " + path);

            if (path == null) {
                return;
            }

            if (path.toLowerCase().endsWith(".csv")) {
                try {
                    // NOVO: Chama o servidor pedindo o conteúdo do arquivo
                    open_tabela(path);
                } catch (Exception ignored) {

                }
            }
        });

        pn_tree.add(fsTree, BorderLayout.CENTER);
        
        iniciarWebSocket();

        final java.util.concurrent.atomic.AtomicInteger fails = new java.util.concurrent.atomic.AtomicInteger(0);

        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    api.get("/api/ping");
                    if (fails.getAndSet(0) != 0) {
                        System.out.println("UP");
                    }
                    hideOfflineOverlay();
                } catch (Exception e) {
                    if (fails.incrementAndGet() >= 2) { // 2 falhas seguidas => OFF
                        System.out.println("DOWN");
                        Log.registrarErro("Servidor OFF", e);
                        showOfflineOverlay();
                    }
                }
            }
        }, 0, 3000);
        System.out.println("Session: " + session.baseUrl());

        System.out.println("Painel: " + pn_logo.isVisible());
    }

    // [INSERIR NOVO MÉTODO NA CLASSE Principal]
    private void iniciarWebSocket() {
        try {
            // Converte http -> ws (ex: http://localhost:8080 -> ws://localhost:8080/ws)
            String url = session.baseUrl().replace("http", "ws") + "/ws";
            URI uri = new URI(url);

            appSocket = new AppSocketClient(uri, evento -> {
                // Quando receber evento do servidor, aplica na árvore
                SwingUtilities.invokeLater(() -> {
                    fsTree.applyEvents(Collections.singletonList(evento));
                });
            });

            appSocket.connect(); // Inicia conexão em thread separada

        } catch (Exception e) {
            e.printStackTrace();
            Log.registrarErro("Erro ao iniciar WebSocket", e);
        }
    }

    public void paint_tema() {
        temaSync.aplicarTemaDoServidorNoLogin(this);
    }

    // Listeners
    @Override
    public void inicio_jornal(String valor) {
        SwingUtilities.invokeLater(() -> {
            out_entrada_jornal.setText(valor);
        });

    }

    @Override
    public void att_tempo() {
        SwingUtilities.invokeLater(() -> {
            String objeto = lbl_arquivo_aberto.getText().split("-")[1].trim();
            switch (objeto) {
                case "Prelim", "BO_CTL1", "BO_CTL2" ->
                    tabela.tempo_prelim_bo(tabela.tabela_news);
                case "Final" ->
                    tabela.tempo_final(tabela.tabela_news);
            }
        });

    }

    @Override
    public void tempo_producao() {
        SwingUtilities.invokeLater(() -> {
            int r = tabela.tabela_news.getRowCount() - 2;
            String acumulado = "00:00:00";
            for (int c = 1; c < r; c++) {
                String valor_tMat_1 = (String) tabela.tabela_news.getValueAt(c, 10);

                valor_tMat_1 = Funcoes.format_ms_to_hms(valor_tMat_1);
                acumulado = Funcoes.soma_tempo(acumulado, valor_tMat_1);
            }

            out_tempo_producao.setText(acumulado);
        });
    }

    @Override
    public void stts_jornal() {
        SwingUtilities.invokeLater(() -> {

            String tempo_saida = Funcoes.soma_tempo(out_entrada_jornal.getText(), out_tempo_producao.getText());

            LocalTime encerramento = LocalTime.parse(out_encerramento.getText());
            LocalTime saida = LocalTime.parse(tempo_saida);

            Duration diferenca = Duration.between(encerramento, saida).abs();

            String mensagem;

            Color cor;

            if (encerramento.isAfter(saida)) {
                mensagem = "Buraco " + Funcoes.formatarDuracao(diferenca);
                cor = new Color(0, 0, 255); // Azul
            } else if (encerramento.isBefore(saida)) {
                mensagem = "Estouro " + Funcoes.formatarDuracao(diferenca);
                cor = new Color(255, 51, 51); // Vermelho
            } else {
                mensagem = "OK";
                cor = new Color(0, 153, 0); // Verde
            }

            try {
                Font font = new Font("Arial", Font.BOLD, 16);
                out_status_jornal.setText(mensagem);
                out_status_jornal.setForeground(cor);
                out_status_jornal.setFont(font);
            } catch (Exception e) {
                System.err.println("\tErro ao carregar Tempo_encerramento na memória\n");
                e.printStackTrace();
            }
        });

    }

    @Override
    public void tempo_encerramento(String valor) {
        SwingUtilities.invokeLater(() -> {
            out_encerramento.setText(valor);
        });

    }

    public static void clear_desktop() {
        pn_desktop.setVisible(false);
        pn_desktop.setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    void open_desktop(String arquivo) {
        lbl_arquivo_aberto.setText(arquivo);

        pn_desktop.setVisible(true);
    }

    void open_tabela(String nomeArquivoRelativo) {
// Lê o CSV em outra thread pra não travar a UI
        new Thread(() -> {
            try {
                String csvTexto = api.readCsv(nomeArquivoRelativo);

                SwingUtilities.invokeLater(() -> {
                    Path pathVirtual = java.nio.file.Paths.get(nomeArquivoRelativo);
                    String tipo = tipoDeArquivo(pathVirtual); // "Prelim", "Final", etc

                    String produto = (pathVirtual.getParent() != null)
                            ? pathVirtual.getParent().getFileName().toString()
                            : "Raiz";

                    String titulo = produto + " - " + tipo;

                    // 1) Cria o modelo com as regras da tabela (colunas + isCellEditable)
                    // 2) Preenche o modelo com o CSV JÁ LIDOf
                    DefaultTableModel model
                            = Tabela.modeloDeCsv(csvTexto);

                    // 3) Fecha a tabela anterior, se houver
                    if (tabela != null && !tabela.isClosed()) {
                        tabela.dispose();
                        tabela = null;
                    }

                    // 4) Cria o novo jInternal_tabela com o MODEL pronto
                    tabela = new jInternal_tabela(model, api, this, nomeArquivoRelativo, session.username());
                    try {
                        tabela.jScrollPane1.setBorder(new EmptyBorder(0, 0, 0, 0));
                        tabela.setBorder(new EmptyBorder(0, 0, 0, 0));
                    } catch (Exception ignored) {
                    }

                    Tabela.model_padrao(tabela.tabela_news);
                    Tabela.ajustarLarguraColuna(tabela.tabela_news);

                    Desktop.add(tabela);
                    temaSync.aplicarTemaTable(tabela.tabela_news);

                    Tabela.verificarErrosDeTempo(tabela.tabela_news);

                    try {
                        tabela.setMaximum(true);
                    } catch (Exception ignore) {
                    }

                    tabela.setVisible(true);
                    tabela.toFront();

                    // 5) Atualiza o painel superior com o título
                    open_desktop(titulo);

                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> Funcoes.message_error(
                        this,
                        "Erro ao carregar CSV do servidor:\n" + e.getMessage()
                ));
            }
        }).start();
    }

    public void refresh_collumn() {
        if (tabela != null) {

            String coluna_style = item_coluna.getText();

            if (coluna_style.equals("Soltar colunas")) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Thread.sleep(1000);
                        return null;
                    }

                    @Override
                    protected void done() {
                        // Após carregamento, reverter o cursor
                        try {
                            tabela.tabela_news.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

                            item_coluna.setText("Juntar colunas");

                            setCursor(Cursor.getDefaultCursor());

                        } finally {
                            Log.registrarErro_noEx("Coluna - AUTO RESIZE OFF");
                        }

                    }
                }.execute();

            } else if (coluna_style.equals("Juntar colunas")) {

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Thread.sleep(1000);
                        return null;
                    }

                    @Override
                    protected void done() {
                        // Após carregamento, reverter o cursor
                        try {
                            tabela.tabela_news.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

                            item_coluna.setText("Soltar colunas");

                            setCursor(Cursor.getDefaultCursor());

                        } finally {
                            Log.registrarErro_noEx("Coluna - AUTO RESIZE LAST_COLUMN");
                        }
                    }
                }.execute();

            }
            att_coluna_mode();
        }
    }

    public void att_coluna_mode() {
// Captura o path ANTES de destruir o frame
        final String csvPath = tabela.csv_server;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Rodar em background para não travar UI
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                // Se quiser um pequeno delay para UX (opcional):
                Thread.sleep(200);
                return null;
            }

            @Override
            protected void done() {
                try {

                    // Fecha o frame atual com segurança
                    try {
                        clear_desktop();
                    } finally {
                        if (tabela != null) {
                            tabela.dispose();
                            tabela = null;
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Restaura cursor
                    setCursor(Cursor.getDefaultCursor());

                    Log.registrarErro_noEx("Frame Reiniciado");

                    // Abre NOVO frame com o path salvo
                    open_tabela(csvPath);
                }
            }

        }.execute();
    }

    private void installBlockingGlassPane() {
        // consome mouse (impede cliques na UI de fundo)
        var blockerMouse = new java.awt.event.MouseAdapter() {
        };
        glass.addMouseListener(blockerMouse);
        glass.addMouseMotionListener(blockerMouse);
        glass.addMouseWheelListener(blockerMouse);

        // consome teclado enquanto visível
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> glass.isVisible());

        glass.setOpaque(true);
        glass.setBackground(new java.awt.Color(30, 30, 30, 160));

        // conteúdo central
        var box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setSize(getSize());

        var lbl = new JLabel("Servidor offline.");
        lbl.setForeground(java.awt.Color.WHITE);
        lbl.setAlignmentX(0.5f);

        box.add(lbl);
        box.add(Box.createVerticalStrut(8));

        glass.add(box, new java.awt.GridBagConstraints());

        // instala no frame uma única vez e começa oculto
        getRootPane().setGlassPane(glass);
        glass.setVisible(false);
    }

    private static String tipoDeArquivo(Path csvPath) {
        String nome = csvPath.getFileName().toString().replaceFirst("(?i)\\.csv$", "");
        // Normaliza para bater com seu modelos(...):
        // "Boletim_ctl1" -> "BO_CTL1", "Boletim_ctl2" -> "BO_CTL2"
        String up = nome.toUpperCase();
        if (up.startsWith("BOLETIM_CTL1")) {
            return "BO_CTL1";
        }
        if (up.startsWith("BOLETIM_CTL2")) {
            return "BO_CTL2";
        }
        if (up.startsWith("PRELIM")) {
            return "Prelim";   // mantém exatamente como espera
        }
        if (up.startsWith("FINAL")) {
            return "Final";
        }
        if (up.startsWith("FORMATO")) {
            return "Formato";
        }
        return nome; // fallback
    }

    private void showOfflineOverlay() {
        if (offlineShown) {
            return;
        }
        offlineShown = true;

        Runnable r = () -> {
            glass.setVisible(true);
            glass.revalidate();
            glass.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void hideOfflineOverlay() {
        if (!offlineShown) {
            return;
        }
        offlineShown = false;

        Runnable r = () -> glass.setVisible(false);
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    // opcional: se um refresh de token ocorrer em outro ponto
    public void onTokenRefreshed(String newToken) {
        session.setAccessToken(newToken);
    }

    public void reload_tree(String dir) {
        fsTree.reloadDir(dir);
    }

    public void logout() {
        int op = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente sair?",
                "Logout Mirror Page",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (op != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Limpa tokens e sessão local
            session.setAccessToken(null);

            // Fecha a tela principal
            this.dispose();

            // Retorna para a tela de login
            javax.swing.SwingUtilities.invokeLater(() -> {
                new Login().setVisible(true); // ou Form_login, dependendo da sua tela de entrada
            });

            System.out.println("Usuário deslogado com sucesso.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao efetuar logout: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        pn_background = new javax.swing.JPanel();
        pn_lateral = new javax.swing.JPanel();
        pn_tree = new javax.swing.JPanel();
        pn_marca = new javax.swing.JPanel();
        lbl_marca = new javax.swing.JLabel();
        control_pn_lateral = new javax.swing.JLabel();
        pn_logo = new Image_panel("/icons/logo_globinho.png");
        pn_desktop = new javax.swing.JPanel();
        pn_superior_desktop = new javax.swing.JPanel();
        lbl_arquivo_aberto = new javax.swing.JLabel();
        lbl_fechar_arquivo = new javax.swing.JLabel();
        lbl_show_user = new javax.swing.JLabel();
        Desktop = new javax.swing.JDesktopPane();
        pn_inferior = new javax.swing.JPanel();
        pn_baixo = new javax.swing.JPanel();
        horario_atual = new javax.swing.JLabel();
        lbl_status_jornal = new javax.swing.JLabel();
        out_status_jornal = new javax.swing.JLabel();
        pn_alto = new javax.swing.JPanel();
        lbl_entrada_jornal = new javax.swing.JLabel();
        out_entrada_jornal = new javax.swing.JLabel();
        lbl_producao = new javax.swing.JLabel();
        out_tempo_producao = new javax.swing.JLabel();
        out_encerramento = new javax.swing.JLabel();
        lbl_encerramento = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        mn_config = new javax.swing.JMenu();
        item_opcoes = new javax.swing.JMenuItem();
        item_coluna = new javax.swing.JMenuItem();
        item_cronometro = new javax.swing.JMenuItem();
        item_fechar_arquivo = new javax.swing.JMenuItem();
        item_logout = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Mirror Page");
        setIconImage(icon.getImage());
        setName("Principal"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pn_background.setBackground(new java.awt.Color(30, 30, 30));
        pn_background.setName("pn_background"); // NOI18N

        pn_lateral.setName("pn_lateral"); // NOI18N

        pn_tree.setName("pn_tree"); // NOI18N
        pn_tree.setRequestFocusEnabled(false);

        javax.swing.GroupLayout pn_treeLayout = new javax.swing.GroupLayout(pn_tree);
        pn_tree.setLayout(pn_treeLayout);
        pn_treeLayout.setHorizontalGroup(
            pn_treeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 200, Short.MAX_VALUE)
        );
        pn_treeLayout.setVerticalGroup(
            pn_treeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 647, Short.MAX_VALUE)
        );

        pn_marca.setName("pn_marca"); // NOI18N

        lbl_marca.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_marca.setText("© Desenvolvido pela CompilaDev");
        lbl_marca.setName("lbl_marca"); // NOI18N

        javax.swing.GroupLayout pn_marcaLayout = new javax.swing.GroupLayout(pn_marca);
        pn_marca.setLayout(pn_marcaLayout);
        pn_marcaLayout.setHorizontalGroup(
            pn_marcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pn_marcaLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lbl_marca, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pn_marcaLayout.setVerticalGroup(
            pn_marcaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lbl_marca, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout pn_lateralLayout = new javax.swing.GroupLayout(pn_lateral);
        pn_lateral.setLayout(pn_lateralLayout);
        pn_lateralLayout.setHorizontalGroup(
            pn_lateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_lateralLayout.createSequentialGroup()
                .addGroup(pn_lateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pn_marca, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pn_tree, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        pn_lateralLayout.setVerticalGroup(
            pn_lateralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_lateralLayout.createSequentialGroup()
                .addComponent(pn_tree, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pn_marca, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        control_pn_lateral.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        control_pn_lateral.setText("|");
        control_pn_lateral.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        control_pn_lateral.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        control_pn_lateral.setName("control_pn_lateral"); // NOI18N
        control_pn_lateral.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                control_pn_lateralMouseClicked(evt);
            }
        });

        pn_logo.setName("pn_logo"); // NOI18N
        pn_logo.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                pn_logoComponentResized(evt);
            }
        });

        pn_desktop.setName("pn_desktop"); // NOI18N

        pn_superior_desktop.setName("pn_superior_desktop"); // NOI18N

        lbl_arquivo_aberto.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 14)); // NOI18N
        lbl_arquivo_aberto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_arquivo_aberto.setText("Arquivo Aberto");
        lbl_arquivo_aberto.setName("lbl_arquivo_aberto"); // NOI18N
        lbl_arquivo_aberto.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lbl_arquivo_abertoMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lbl_arquivo_abertoMouseExited(evt);
            }
        });

        lbl_fechar_arquivo.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 14)); // NOI18N
        lbl_fechar_arquivo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_fechar_arquivo.setText("X");
        lbl_fechar_arquivo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lbl_fechar_arquivo.setName("lbl_fechar_arquivo"); // NOI18N
        lbl_fechar_arquivo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_fechar_arquivoMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lbl_fechar_arquivoMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lbl_fechar_arquivoMouseExited(evt);
            }
        });

        lbl_show_user.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_show_user.setText("Usuário: @user");
        lbl_show_user.setName("lbl_show_user"); // NOI18N

        javax.swing.GroupLayout pn_superior_desktopLayout = new javax.swing.GroupLayout(pn_superior_desktop);
        pn_superior_desktop.setLayout(pn_superior_desktopLayout);
        pn_superior_desktopLayout.setHorizontalGroup(
            pn_superior_desktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pn_superior_desktopLayout.createSequentialGroup()
                .addComponent(lbl_show_user, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl_arquivo_aberto, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lbl_fechar_arquivo, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pn_superior_desktopLayout.setVerticalGroup(
            pn_superior_desktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lbl_arquivo_aberto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_fechar_arquivo, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
            .addComponent(lbl_show_user, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        Desktop.setName("Desktop"); // NOI18N
        Desktop.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                DesktopComponentResized(evt);
            }
        });

        javax.swing.GroupLayout DesktopLayout = new javax.swing.GroupLayout(Desktop);
        Desktop.setLayout(DesktopLayout);
        DesktopLayout.setHorizontalGroup(
            DesktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        DesktopLayout.setVerticalGroup(
            DesktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 556, Short.MAX_VALUE)
        );

        pn_inferior.setBackground(new java.awt.Color(50, 50, 50));
        pn_inferior.setName("pn_inferior"); // NOI18N

        pn_baixo.setName("pn_baixo"); // NOI18N

        horario_atual.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 14)); // NOI18N
        horario_atual.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        horario_atual.setText("HORÁRIO ATUAL");
        horario_atual.setName("horario_atual"); // NOI18N

        lbl_status_jornal.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 12)); // NOI18N
        lbl_status_jornal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_status_jornal.setText("STATUS");
        lbl_status_jornal.setName("lbl_status_jornal"); // NOI18N

        out_status_jornal.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 14)); // NOI18N
        out_status_jornal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        out_status_jornal.setText("STATUS");
        out_status_jornal.setName("out_status_jornal"); // NOI18N

        javax.swing.GroupLayout pn_baixoLayout = new javax.swing.GroupLayout(pn_baixo);
        pn_baixo.setLayout(pn_baixoLayout);
        pn_baixoLayout.setHorizontalGroup(
            pn_baixoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_baixoLayout.createSequentialGroup()
                .addComponent(lbl_status_jornal, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(out_status_jornal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(horario_atual, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pn_baixoLayout.setVerticalGroup(
            pn_baixoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_baixoLayout.createSequentialGroup()
                .addGroup(pn_baixoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pn_baixoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_status_jornal, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)
                        .addComponent(out_status_jornal, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(horario_atual, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 5, Short.MAX_VALUE))
        );

        pn_alto.setName("pn_alto"); // NOI18N

        lbl_entrada_jornal.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 12)); // NOI18N
        lbl_entrada_jornal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_entrada_jornal.setText("ENTRADA");
        lbl_entrada_jornal.setName("lbl_entrada_jornal"); // NOI18N

        out_entrada_jornal.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 14)); // NOI18N
        out_entrada_jornal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        out_entrada_jornal.setText("00:00:00");
        out_entrada_jornal.setName("out_entrada_jornal"); // NOI18N

        lbl_producao.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 12)); // NOI18N
        lbl_producao.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_producao.setText("PRODUÇÃO");
        lbl_producao.setName("lbl_producao"); // NOI18N

        out_tempo_producao.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 14)); // NOI18N
        out_tempo_producao.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        out_tempo_producao.setText("00:00:00");
        out_tempo_producao.setName("out_tempo_producao"); // NOI18N

        out_encerramento.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        out_encerramento.setText("00:00:00");
        out_encerramento.setName("out_encerramento"); // NOI18N

        lbl_encerramento.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 12)); // NOI18N
        lbl_encerramento.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_encerramento.setText("ENCERRAMENTO");
        lbl_encerramento.setName("lbl_encerramento"); // NOI18N

        javax.swing.GroupLayout pn_altoLayout = new javax.swing.GroupLayout(pn_alto);
        pn_alto.setLayout(pn_altoLayout);
        pn_altoLayout.setHorizontalGroup(
            pn_altoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_altoLayout.createSequentialGroup()
                .addComponent(lbl_entrada_jornal, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(out_entrada_jornal, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 185, Short.MAX_VALUE)
                .addComponent(lbl_producao, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(out_tempo_producao, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 186, Short.MAX_VALUE)
                .addComponent(lbl_encerramento, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(out_encerramento, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        pn_altoLayout.setVerticalGroup(
            pn_altoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(out_tempo_producao, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_entrada_jornal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(pn_altoLayout.createSequentialGroup()
                .addComponent(lbl_producao, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 1, Short.MAX_VALUE))
            .addComponent(out_entrada_jornal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_encerramento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(out_encerramento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout pn_inferiorLayout = new javax.swing.GroupLayout(pn_inferior);
        pn_inferior.setLayout(pn_inferiorLayout);
        pn_inferiorLayout.setHorizontalGroup(
            pn_inferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pn_baixo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pn_alto, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pn_inferiorLayout.setVerticalGroup(
            pn_inferiorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pn_inferiorLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(pn_alto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pn_baixo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout pn_desktopLayout = new javax.swing.GroupLayout(pn_desktop);
        pn_desktop.setLayout(pn_desktopLayout);
        pn_desktopLayout.setHorizontalGroup(
            pn_desktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pn_inferior, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pn_superior_desktop, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(Desktop, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        pn_desktopLayout.setVerticalGroup(
            pn_desktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_desktopLayout.createSequentialGroup()
                .addComponent(pn_superior_desktop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(Desktop)
                .addGap(0, 0, 0)
                .addComponent(pn_inferior, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout pn_logoLayout = new javax.swing.GroupLayout(pn_logo);
        pn_logo.setLayout(pn_logoLayout);
        pn_logoLayout.setHorizontalGroup(
            pn_logoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pn_desktop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pn_logoLayout.setVerticalGroup(
            pn_logoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_logoLayout.createSequentialGroup()
                .addComponent(pn_desktop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout pn_backgroundLayout = new javax.swing.GroupLayout(pn_background);
        pn_background.setLayout(pn_backgroundLayout);
        pn_backgroundLayout.setHorizontalGroup(
            pn_backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pn_backgroundLayout.createSequentialGroup()
                .addComponent(pn_lateral, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(control_pn_lateral, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pn_logo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        pn_backgroundLayout.setVerticalGroup(
            pn_backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pn_lateral, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(control_pn_lateral, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(pn_logo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        mn_config.setText("Configurações");
        mn_config.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mn_configActionPerformed(evt);
            }
        });

        item_opcoes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        item_opcoes.setText("Opções");
        item_opcoes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                item_opcoesActionPerformed(evt);
            }
        });
        mn_config.add(item_opcoes);

        item_coluna.setText("Soltar colunas");
        item_coluna.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                item_colunaActionPerformed(evt);
            }
        });
        mn_config.add(item_coluna);

        item_cronometro.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        item_cronometro.setText("Cronômetro");
        item_cronometro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                item_cronometroActionPerformed(evt);
            }
        });
        mn_config.add(item_cronometro);

        item_fechar_arquivo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        item_fechar_arquivo.setText("Fechar arquivo");
        item_fechar_arquivo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                item_fechar_arquivoActionPerformed(evt);
            }
        });
        mn_config.add(item_fechar_arquivo);

        item_logout.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        item_logout.setText("Sair");
        item_logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                item_logoutActionPerformed(evt);
            }
        });
        mn_config.add(item_logout);

        jMenuBar1.add(mn_config);

        jMenu1.setText("DEV");

        jMenuItem2.setText("Abrir Console");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pn_background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pn_background, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void item_opcoesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_item_opcoesActionPerformed
        // TODO add your handling code here:
        Configuracoes tela_config = new Configuracoes(this, true, fsTree, api, temaSync, this);
        tela_config.setVisible(true);
    }//GEN-LAST:event_item_opcoesActionPerformed

    private void mn_configActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mn_configActionPerformed
        // TODO add your handling code here:


    }//GEN-LAST:event_mn_configActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        if (!d.isVisible()) {
            d.setVisible(true);
        } else {
            d.toFront();
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        if (appSocket != null) {
            appSocket.close();
        }
    }//GEN-LAST:event_formWindowClosing

    private void control_pn_lateralMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_control_pn_lateralMouseClicked
        // TODO add your handling code here:
        if (pn_lateral.isVisible()) {
            pn_lateral.setVisible(false);
        } else {
            pn_lateral.setVisible(true);
        }
        pn_background.revalidate(); // força recalcular
        pn_background.repaint();
    }//GEN-LAST:event_control_pn_lateralMouseClicked

    private void item_logoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_item_logoutActionPerformed
        // TODO add your handling code here:
        logout();
    }//GEN-LAST:event_item_logoutActionPerformed

    private void item_fechar_arquivoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_item_fechar_arquivoActionPerformed
        // TODO add your handling code here:
        clear_desktop();
    }//GEN-LAST:event_item_fechar_arquivoActionPerformed

    private void pn_logoComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_pn_logoComponentResized
        // TODO add your handling code here:

        if (pn_desktop.isVisible()) {
            pn_desktop.setSize(pn_logo.getSize());
        }
    }//GEN-LAST:event_pn_logoComponentResized

    private void DesktopComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_DesktopComponentResized
        // TODO add your handling code here:
        if (pn_desktop.isVisible()) {
            tabela.setSize(Desktop.getSize());
        }

    }//GEN-LAST:event_DesktopComponentResized

    private void lbl_fechar_arquivoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_fechar_arquivoMouseClicked
        // TODO add your handling code here:
        try {
            clear_desktop();
        } finally {
            tabela.dispose();
            tabela = null;        // limpa referência
        }
    }//GEN-LAST:event_lbl_fechar_arquivoMouseClicked

    private void lbl_arquivo_abertoMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_arquivo_abertoMouseEntered
        // TODO add your handling code here:
        switch (temaSync.tema_applied()) {
            case "DEFAULT":
                lbl_arquivo_aberto.setBackground(new Color(200, 210, 230));
                break;
            case "DARK":
                lbl_arquivo_aberto.setBackground(new Color(50, 50, 50));
                break;
            case "STAR_LIGHT":
                lbl_arquivo_aberto.setBackground(new Color(50, 50, 50));
                break;
        }
    }//GEN-LAST:event_lbl_arquivo_abertoMouseEntered

    private void lbl_arquivo_abertoMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_arquivo_abertoMouseExited
        // TODO add your handling code here:
        switch (temaSync.tema_applied()) {
            case "DEFAULT":
                lbl_arquivo_aberto.setBackground(new Color(180, 190, 200));
                break;
            case "DARK":
                lbl_arquivo_aberto.setBackground(new Color(30, 30, 30));
                break;
            case "STAR_LIGHT":
                lbl_arquivo_aberto.setBackground(new Color(30, 30, 30));
                break;
        }
    }//GEN-LAST:event_lbl_arquivo_abertoMouseExited

    private void lbl_fechar_arquivoMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_fechar_arquivoMouseEntered
        // TODO add your handling code here:
        switch (temaSync.tema_applied()) {
            case "DEFAULT":
                lbl_fechar_arquivo.setBackground(new Color(200, 210, 230));
                break;
            case "DARK":
                lbl_fechar_arquivo.setBackground(new Color(50, 50, 50));
                break;
            case "STAR_LIGHT":
                lbl_fechar_arquivo.setBackground(new Color(50, 50, 50));
                break;
        }
    }//GEN-LAST:event_lbl_fechar_arquivoMouseEntered

    private void lbl_fechar_arquivoMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_fechar_arquivoMouseExited
        // TODO add your handling code here:
        switch (temaSync.tema_applied()) {
            case "DEFAULT":
                lbl_fechar_arquivo.setBackground(new Color(180, 190, 200));
                break;
            case "DARK":
                lbl_fechar_arquivo.setBackground(new Color(30, 30, 30));
                break;
            case "STAR_LIGHT":
                lbl_fechar_arquivo.setBackground(new Color(30, 30, 30));
                break;
        }
    }//GEN-LAST:event_lbl_fechar_arquivoMouseExited

    private void item_cronometroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_item_cronometroActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_item_cronometroActionPerformed

    private void item_colunaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_item_colunaActionPerformed
        // TODO add your handling code here:
        refresh_collumn();
    }//GEN-LAST:event_item_colunaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDesktopPane Desktop;
    private javax.swing.JLabel control_pn_lateral;
    private javax.swing.JLabel horario_atual;
    private javax.swing.JMenuItem item_coluna;
    private javax.swing.JMenuItem item_cronometro;
    private javax.swing.JMenuItem item_fechar_arquivo;
    private javax.swing.JMenuItem item_logout;
    private javax.swing.JMenuItem item_opcoes;
    private javax.swing.JButton jButton1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    public javax.swing.JLabel lbl_arquivo_aberto;
    private javax.swing.JLabel lbl_encerramento;
    private javax.swing.JLabel lbl_entrada_jornal;
    private javax.swing.JLabel lbl_fechar_arquivo;
    private javax.swing.JLabel lbl_marca;
    private javax.swing.JLabel lbl_producao;
    private javax.swing.JLabel lbl_show_user;
    private javax.swing.JLabel lbl_status_jornal;
    private javax.swing.JMenu mn_config;
    private javax.swing.JLabel out_encerramento;
    private javax.swing.JLabel out_entrada_jornal;
    private javax.swing.JLabel out_status_jornal;
    private javax.swing.JLabel out_tempo_producao;
    private javax.swing.JPanel pn_alto;
    private javax.swing.JPanel pn_background;
    private javax.swing.JPanel pn_baixo;
    public static javax.swing.JPanel pn_desktop;
    private javax.swing.JPanel pn_inferior;
    private javax.swing.JPanel pn_lateral;
    private javax.swing.JPanel pn_logo;
    private javax.swing.JPanel pn_marca;
    private javax.swing.JPanel pn_superior_desktop;
    private javax.swing.JPanel pn_tree;
    // End of variables declaration//GEN-END:variables

}
