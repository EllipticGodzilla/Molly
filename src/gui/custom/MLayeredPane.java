package gui.custom;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class MLayeredPane extends JLayeredPane { //permette di ridimensionare componenti in modo che abbiamo sempre la sua stessa dimensione
    Vector<Component> full_screen = new Vector<>();
    private JMenuBar menuBar = null;
    private int menuBar_height = 0;

    public MLayeredPane() {
        super();
        this.setOpaque(false);
    }

    public void set_menu_bar(JMenuBar menuBar) {
        //è possibile aggiungere una sola menu bar
        if (this.menuBar != null) {
            return;
        }

        this.add(menuBar, JLayeredPane.FRAME_CONTENT_LAYER);

        this.menuBar = menuBar;
        this.menuBar_height = menuBar.getPreferredSize().height;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) { //in questo modo si elimina il delay che si avrebbe utilizzando un component listener
        super.setBounds(x, y, width, height);

        if (menuBar != null) {
            menuBar.setSize(width, menuBar_height);
        }

        for (Component cmp : full_screen) {
            cmp.setBounds(0, menuBar_height, width, height - menuBar_height);
        }
    }

    public void add_fullscreen(Component comp, int index) {
        super.add(comp, index);
        full_screen.add(comp);
    }
}