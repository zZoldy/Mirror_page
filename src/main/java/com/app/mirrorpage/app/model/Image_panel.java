package com.app.mirrorpage.app.model;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class Image_panel extends JPanel {

    private Image imagemFundo;

    // Construtor com caminho relativo à pasta resources
    public Image_panel(String caminhoImagem) {
        setImagemFundo(caminhoImagem);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (imagemFundo != null) {
            int novoWidth = 300;
            int novoHeight = 300;

            int x = (getWidth() - novoWidth) / 2;
            int y = (getHeight() - novoHeight) / 2;

            g.drawImage(imagemFundo, x, y, novoWidth, novoHeight, this);
        }
    }

    // Permite mudar a imagem de fundo dinamicamente
    public void setImagemFundo(String caminhoImagem) {
        try {
            this.imagemFundo = new ImageIcon(getClass().getResource(caminhoImagem)).getImage();
        } catch (Exception e) {
            System.err.println("⚠ Erro ao carregar imagem: " + caminhoImagem);
            e.printStackTrace();
            this.imagemFundo = null;
        }
        repaint();
    }
}
