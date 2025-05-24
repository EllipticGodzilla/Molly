package gui.custom;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class MLayeredPane extends JLayeredPane { //permette di ridimensionare componenti in modo che abbiamo sempre la sua stessa dimensione
    Vector<Component> full_screen = new Vector<>();
    private int top_border = 0;

    public MLayeredPane() {
        super();
        this.setOpaque(false);
    }

    public void set_top_border(int top_border) {
        this.top_border = top_border;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) { //in questo modo si elimina il delay che si avrebbe utilizzando un component listener
        super.setBounds(x, y, width, height);

        for (Component cmp : full_screen) {
            cmp.setBounds(0, top_border, width, height - top_border);
        }
    }

    public void add_fullscreen(Component comp, int index) {
        super.add(comp, index);
        full_screen.add(comp);
    }
}