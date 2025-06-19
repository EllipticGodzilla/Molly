package gui;

import gui.custom.MFrame;
import gui.custom.MLayeredPane;
import gui.temppanel.TempPanel;
import gui.themes.GraphicsSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class MollyFrame {
    private static MFrame godzilla_frame = null;

    //inizializza la schermata e ritorna il JFrame
    public static JFrame init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        if (godzilla_frame == null) {
            godzilla_frame = new MFrame();

            //inizializza tutti i pannelli che formeranno la gui principale
            //tutti i pannelli da aggiungere al frame
            JPanel server_list = ServerList_panel.init();
            JPanel client_list = ClientList_panel.init();
            JPanel button_topbar = ButtonTopBar_panel.init();
            JPanel central_terminal = CentralPanel.init();
            JPanel temp_panel = TempPanel.init();

            //inizializza la gui principale (tutti i pannelli tranne Temp Panels)
            JPanel content_panel = new JPanel();
            content_panel.setOpaque(false);
            content_panel.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.BOTH;

            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 2;
            c.gridheight = 1;
            c.weighty = 0;
            content_panel.add(server_list, c);

            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.weighty = 1;
            content_panel.add(client_list, c);

            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.weighty = 0;
            content_panel.add(button_topbar, c);

            c.gridx = 1;
            c.gridy = 1;
            c.weighty = 1;
            c.gridheight = 2;
            content_panel.add(central_terminal, c);

            content_panel.setBounds(0, 0, 900, 663);
            MLayeredPane layeredPane = (MLayeredPane) godzilla_frame.getLayeredPane();
            layeredPane.add_fullscreen(content_panel, JLayeredPane.DEFAULT_LAYER);
            layeredPane.add(temp_panel, JLayeredPane.POPUP_LAYER);

            Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
            godzilla_frame.setLocation(
                    screen_size.width/2 - godzilla_frame.getWidth()/2,
                    screen_size.height/2 - godzilla_frame.getHeight()/2
            );

            godzilla_frame.setVisible(true);

            //mantiene gui.temppanel.TempPanel sempre al centro del frame
            godzilla_frame.addComponentListener(new ComponentListener() {
                @Override
                public void componentMoved(ComponentEvent e) {}
                @Override
                public void componentShown(ComponentEvent e) {}
                @Override
                public void componentHidden(ComponentEvent e) {}

                @Override
                public void componentResized(ComponentEvent e) {
                    TempPanel.recenter_in_frame();
                }
            });

            GraphicsSettings.run_at_theme_change(MollyFrame::update_colors);
        }
        return godzilla_frame;
    }

    private static void update_colors() {
        godzilla_frame.update_colors();
        ButtonTopBar_panel.update_colors();
        CentralPanel.update_colors();
        ClientList_panel.update_colors();
        ServerList_panel.update_colors();
    }

    public static Rectangle get_bounds() {
        return godzilla_frame.getBounds();
    }

    public static void request_focus() {
        if (!godzilla_frame.hasFocus()) {
            //anche se già visibile chiamare setVisible(true) porta il frame in primo piano
            godzilla_frame.setVisible(true);
        }
    }
}

