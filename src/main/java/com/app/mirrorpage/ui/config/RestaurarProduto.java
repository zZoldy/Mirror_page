package com.app.mirrorpage.ui.config;

import com.app.mirrorpage.app.model.file.CsvModelService;
import com.app.mirrorpage.app.model.file.CsvModelType;
import com.app.mirrorpage.client.net.ApiClient;
import com.app.mirrorpage.ui.Principal;
import javax.swing.JOptionPane;

public class RestaurarProduto extends javax.swing.JPanel {

    ApiClient api;
    Principal principal;

    public RestaurarProduto(ApiClient api, Principal principal) {
        this.api = api;
        this.principal = principal;

        initComponents();
        lbl_info_produto.setVisible(false);
        txt_produto.setVisible(false);
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

        box_produto.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "BDBR", "BDDF", "DF1", "DF2", "GCO", "GE", " ", "OUTRO", "TODOS" }));
        box_produto.setToolTipText("");
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

    private void btn_applyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_applyActionPerformed
        // TODO add your handling code here:
        String produto = box_produto.getSelectedItem().toString().trim();
        if (produto.isEmpty() || produto.equalsIgnoreCase("Selecione")) {
            JOptionPane.showMessageDialog(this, "Selecione um produto.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (produto.equals("OUTRO")) {
            System.out.println("CRIAR PRODUTO:");
            produto = txt_produto.getText().trim();
            criar_produto(produto);
            return;
        }

        if (produto.equals("TODOS")) {
            restaurar_todos_produtos();
            return;
        }

        try {
            CsvModelService service = new CsvModelService(api);
            String baseDir = "/Produtos/" + produto;

            if (principal.tabela != null) {
                String objeto = principal.lbl_arquivo_aberto.getText().split("-")[0].trim();

                if (objeto.equals(produto)) {
                    try {
                        principal.tabela.dispose();     // fecha e remove do Desktop
                        principal.clear_desktop();
                    } finally {
                        principal.tabela = null;        // limpa referência
                    }
                }
            }

            // arquivos padrão
            service.salvar(CsvModelType.PRELIMINAR, baseDir, produto, "Prelim");
            service.salvar(CsvModelType.FINAL, baseDir, produto, "Final");

            // extras de DF2
            if (produto.equals("DF2")) {
                service.salvar(CsvModelType.BOLETIM_CTL1, baseDir, produto, "Boletim_ctl1");
                service.salvar(CsvModelType.BOLETIM_CTL2, baseDir, produto, "Boletim_ctl2");
            }

            JOptionPane.showMessageDialog(this,
                    "Arquivos de " + produto + " verificados/restaurados no servidor.",
                    "OK", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao restaurar no servidor: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }


    }//GEN-LAST:event_btn_applyActionPerformed


    private void box_produtoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_box_produtoActionPerformed
        // TODO add your handling code here:
        if (box_produto.getSelectedItem().toString().equals("OUTRO")) {
            lbl_info_produto.setVisible(true);
            txt_produto.setVisible(true);
        } else {
            lbl_info_produto.setVisible(false);
            txt_produto.setVisible(false);
        }

    }//GEN-LAST:event_box_produtoActionPerformed

    void restaurar_todos_produtos() {

        int total = box_produto.getItemCount();

        for (int i = 0; i < total; i++) {

            String produto = box_produto.getItemAt(i).trim();

            // pular vazio: " " ou ""
            if (produto.isEmpty()) {
                continue;
            }

            // pular "TODOS"
            if (produto.equalsIgnoreCase("TODOS")) {
                continue;
            }

            // tratar "OUTROS"
            if (produto.equalsIgnoreCase("OUTRO")) {
                // 👉 se NÃO tiver pasta específica "OUTROS", só pula:
                // continue;

                // OU, se existir pasta /Produtos/OUTROS, e você quiser restaurar também:
                // (nesse caso, remove o `continue` e deixa cair no restaurar_produto)
                // (por enquanto vou pular)
                continue;
            }

            try {
                CsvModelService service = new CsvModelService(api);
                String baseDir = "/Produtos/" + produto;

                // Fecha a tabela se estiver aberta com este produto
                if (principal.tabela != null) {
                    String objeto = principal.lbl_arquivo_aberto.getText()
                            .split("-")[0]
                            .trim();

                    if (objeto.equals(produto)) {
                        try {
                            principal.tabela.dispose();
                            principal.clear_desktop();
                        } finally {
                            principal.tabela = null;
                        }
                    }
                }

                // Arquivos padrão
                service.salvar(CsvModelType.PRELIMINAR, baseDir, produto, "Prelim");
                service.salvar(CsvModelType.FINAL, baseDir, produto, "Final");

                // Arquivos extras do DF2
                if (produto.equals("DF2")) {
                    service.salvar(CsvModelType.BOLETIM_CTL1, baseDir, produto, "Boletim_ctl1");
                    service.salvar(CsvModelType.BOLETIM_CTL2, baseDir, produto, "Boletim_ctl2");
                }

                System.out.println("✔ Produto restaurado: " + produto);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erro ao restaurar o produto " + produto + ": " + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }

        JOptionPane.showMessageDialog(this,
                "Todos os produtos foram verificados/restaurados.",
                "Concluído", JOptionPane.INFORMATION_MESSAGE);
    }

    void criar_produto(String produto) {
        try {
            CsvModelService service = new CsvModelService(api);
            String baseDir = "/Produtos/" + produto;

            // arquivos padrão
            service.salvar(CsvModelType.PRELIMINAR, baseDir, produto, "Prelim");
            service.salvar(CsvModelType.FINAL, baseDir, produto, "Final");

            JOptionPane.showMessageDialog(this,
                    "Arquivos de " + produto + " Criado no Servidor/App.",
                    "OK", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao criar no servidor: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
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
