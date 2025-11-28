package com.app.mirrorpage.app;

import com.app.mirrorpage.ui.Login;
import javax.swing.SwingUtilities;

public class MirrorPage {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login l = new Login();
            l.setVisible(true);
        });
    }
}
