package com.app.mirrorpage.app.tema;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

// nomes de tema locais
enum TemaNome {
    DEFAULT, DARK, STAR_LIGHT
}

class ThemeManager {

    public static final String PROP_THEME = "theme";
    private static final ThemeManager INSTANCE = new ThemeManager();

    public static ThemeManager get() {
        return INSTANCE;
    }

    private TemaNome atual = TemaNome.DEFAULT;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public TemaNome temaAtual() {
        return atual;
    }

    public void setTema(TemaNome novo) {
        var old = atual;
        atual = novo;
        pcs.firePropertyChange(PROP_THEME, old, novo);
    }

    public void addListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }
}
