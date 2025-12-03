package com.app.mirrorpage.ui.config;

import com.app.mirrorpage.app.model.file.CsvModelService;
import com.app.mirrorpage.app.model.file.CsvModelType;
import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.ui.Principal;
import java.awt.Cursor;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class RestaurarProduto extends javax.swing.JPanel {

    ApiClient api;
    Principal principal;

    public RestaurarProduto(ApiClient api, Principal principal) {
        this.api = api;
        this.principal = principal;

        initComponents();

        lbl_info_produto.setVisible(false);
        txt_produto.setVisible(false);

        carregarCombo();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lbl_info_produto = new javax.swing.JLabel();
        txt_produto = new javax.swing.JTextField();
        box_produto = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        btn_apply = new javax.swing.JButton();

        lbl_info_produto.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 12)); // NOI18N
        lbl_info_produto.setText("Informe o Produto:");

        box_produto.addItem(" ");
        box_produto.setToolTipText("");
        box_produto.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                box_produtoPopupMenuWillBecomeVisible(evt);
            }
        });
        box_produto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                box_produtoActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Globotipo Corporativa Textos", 1, 12)); // NOI18N
        jLabel1.setText("Selecione o Produto:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lbl_info_produto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txt_produto, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(box_produto, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(box_produto, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_info_produto, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txt_produto, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btn_apply.setText("Aplicar");
        btn_apply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_applyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(294, Short.MAX_VALUE)
                .addComponent(btn_apply, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_apply, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void carregarCombo() {
        new Thread(() -> {
            try {
                // Certifique-se que getPastas() existe no ApiClient!
                List<String> pastas = api.getPastas();

                SwingUtilities.invokeLater(() -> {
                    box_produto.removeAllItems();
                    if (pastas != null) {
                        for (String pasta : pastas) {
                            if (pasta.equals("laudas")) {
                                continue;
                            }
                            box_produto.addItem(pasta);
                        }
                    }
                    box_produto.addItem(" ");
                    box_produto.addItem("OUTRO");
                    box_produto.addItem("TODOS");
                    box_produto.setSelectedItem(" "); // Seleciona o vazio por padrão
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> box_produto.addItem("Erro ao carregar"));
            }
        }).start();
    }

    private void btn_applyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_applyActionPerformed
        String selecionado = (String) box_produto.getSelectedItem();
        if (selecionado == null || selecionado.isBlank() || selecionado.equals("Selecione")) {
            JOptionPane.showMessageDialog(this, "Selecione um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Define qual produto será processado
        final String produtoAlvo = selecionado.equals("OUTRO") ? txt_produto.getText().trim().toUpperCase() : selecionado;

        if (produtoAlvo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Prepara UI para carregamento
        btn_apply.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // 🟢 THREAD PARA NÃO TRAVAR A TELA
        new Thread(() -> {
            try {
                if (selecionado.equals("TODOS")) {
                    processarTodos();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Todos os produtos foram verificados/restaurados.", "Concluído", JOptionPane.INFORMATION_MESSAGE));
                } else {
                    processarProduto(produtoAlvo);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Produto " + produtoAlvo + " processado com sucesso.", "OK", JOptionPane.INFORMATION_MESSAGE));
                }
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Erro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
            } finally {
                // Restaura UI
                SwingUtilities.invokeLater(() -> {
                    btn_apply.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                });
            }
        }).start();
    }//GEN-LAST:event_btn_applyActionPerformed


    private void box_produtoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_box_produtoActionPerformed
        Object item = box_produto.getSelectedItem();
        if (item != null) {
            boolean isOutro = "OUTRO".equals(item.toString());
            lbl_info_produto.setVisible(isOutro);
            txt_produto.setVisible(isOutro);
        }

    }//GEN-LAST:event_box_produtoActionPerformed

    private void box_produtoPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_box_produtoPopupMenuWillBecomeVisible
        // TODO add your handling code here:
    }//GEN-LAST:event_box_produtoPopupMenuWillBecomeVisible

    // Lógica unificada para 1 produto
    private void processarProduto(String produto) throws Exception {
        System.out.println("Processando: " + produto);

        // 2. Operações de Rede (Salvar/Restaurar)
        CsvModelService service = new CsvModelService(api);
        String baseDir = "/Produtos/" + produto;

        // 1. Fechar tabela se estiver aberta (Executar na Thread da UI)
        SwingUtilities.invokeLater(() -> {
            if (principal.tabela != null) {
                String aberto = principal.lbl_arquivo_aberto.getText();
                // Verifica se o texto começa com o produto (ex: "DF2 - Prelim")
                if (aberto != null && aberto.startsWith(produto + " -")) {
                    principal.tabela.sync.aplicarCsvNoModelo(baseDir);
                }
            }
        });

        // Padrão
        service.salvar(CsvModelType.PRELIMINAR, baseDir, produto, "Prelim");
        service.salvar(CsvModelType.FINAL, baseDir, produto, "Final");

        // Específico DF2 (Hardcoded por enquanto)
        if ("DF2".equalsIgnoreCase(produto)) {
            service.salvar(CsvModelType.BOLETIM_CTL1, baseDir, produto, "Boletim_ctl1");
            service.salvar(CsvModelType.BOLETIM_CTL2, baseDir, produto, "Boletim_ctl2");
        }
    }

// Loop para todos
    private void processarTodos() {
        // Pega lista de itens na Thread da UI para evitar conflito
        final int count = box_produto.getItemCount();
        for (int i = 0; i < count; i++) {
            String item = box_produto.getItemAt(i);

            // Ignora itens de controle
            if (item == null || item.isBlank() || item.equals("OUTRO") || item.equals("TODOS") || item.equals("Erro ao carregar")) {
                continue;
            }

            try {
                processarProduto(item.trim());
            } catch (Exception e) {
                System.err.println("Falha ao processar " + item + ": " + e.getMessage());
                // Não para o loop, tenta o próximo
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> box_produto;
    private javax.swing.JButton btn_apply;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel lbl_info_produto;
    private javax.swing.JTextField txt_produto;
    // End of variables declaration//GEN-END:variables
}
