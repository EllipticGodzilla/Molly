package gui;

import files.Logger;
import files.Pair;
import gui.custom.ButtonIcons;
import gui.custom.ButtonInfo;
import gui.themes.GraphicsSettings;
import gui.temppanel.TempPanel;
import gui.temppanel.TempPanel_info;
import network.ServerInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/*
 * gestisce la barra in alto a destra con tutti i pulsanti programmabili, per registrare un nuovo pulsante basta specificare
 * ButtonImages e ButtonInfo
 */
public abstract class ButtonTopBar_panel {
    //in ogni momento può esserci solo una mod attiva alla volta, e il suo nome viene inserito in active_mod
    private static String active_mod = "";
    /*
     * per specificare cosa eseguire quando si preme un dato pulsante si deve specificare i ButtonInfo, un wrapper che contiene
     * i metodi press() e stop(), tutti i ButtonInfo dei pulsanti aggiunti sono inseriti nella mappa buttons_info
     */
    private static final Map<String, ButtonInfo> buttons_info = new LinkedHashMap<>();

    /*
     * quando vengono caricate le mod tutte le classi che specificano informazioni per aggiungere un pulsante vengono tradotte
     * in una coppia ButtonIcons e ButtonInfo, e aggiunte a questo vettore, una volta inizializzato il pannello vengono
     * definiti e aggiunti tutti i pulsanti.
     */
    private static final Vector<Pair<ButtonIcons, ButtonInfo>> added_buttons = new Vector<>();

    //componenti della grafica
    private static JPanel buttons_container;
    private static JScrollPane buttons_scroller;

    private static JButton left_shift;
    private static JButton right_shift;
    private static JButton stop_mod;
    private static JPanel buttons_panel;

    protected static JPanel init() throws IOException {
        if (buttons_container != null) { //se è già stato inizializzato non ha bisogno di ripetere tutto
            return buttons_container;
        }

        buttons_panel = new JPanel();
        buttons_panel.setOpaque(false);
        buttons_panel.setLayout(new GridBagLayout());
        buttons_panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        //inizializza tutti i componenti della gui
        right_shift = new JButton();
        left_shift = new JButton();
        stop_mod = new JButton();
        buttons_container = new JPanel();
        buttons_scroller = new JScrollPane(buttons_container, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        buttons_container.add(stop_mod);

        update_colors();

        right_shift.setBorder(null);
        left_shift.setBorder(null);
        stop_mod.setBorder(null);
        buttons_scroller.setBorder(null);

        right_shift.addActionListener(RIGHTSHIFT_LISTENER);
        left_shift.addActionListener(LEFTSHIFT_LISTENER);
        stop_mod.addActionListener(STOP_LISTENER);

        right_shift.setOpaque(false);
        left_shift.setOpaque(false);
        stop_mod.setOpaque(false);
        right_shift.setContentAreaFilled(false);
        left_shift.setContentAreaFilled(false);
        stop_mod.setContentAreaFilled(false);

        buttons_container.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        add_buttons_to_panel();

        //aggiunge tutti i componenti al pannello organizzandoli nella griglia
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weightx = 0; //i due pulsanti non vengono ridimensionati

        c.gridx = 0;
        buttons_panel.add(left_shift, c);

        c.gridx = 2;
        buttons_panel.add(right_shift, c);

        c.weightx = 1;

        c.gridx = 1;
        buttons_panel.add(buttons_scroller, c);

        return buttons_panel;
    }

    //quando si cambia theme aggiorna tutti i colori dei componenti del ButtonTopBar
    public static void update_colors() {
        buttons_container.setBackground((Color) GraphicsSettings.active_theme().get_value("frame_background"));

        ButtonIcons right_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("button_top_bar_right_shift");
        ButtonIcons left_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("button_top_bar_left_shift");
        ButtonIcons stop_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("button_top_bar_stop_mod");

        right_shift.setIcon(right_icons.getStandardIcon());
        right_shift.setRolloverIcon(right_icons.getRolloverIcon());
        right_shift.setPressedIcon(right_icons.getPressedIcon());
        right_shift.setDisabledIcon(right_icons.getDisabledIcon());
        left_shift.setIcon(left_icons.getStandardIcon());
        left_shift.setRolloverIcon(left_icons.getRolloverIcon());
        left_shift.setPressedIcon(left_icons.getPressedIcon());
        left_shift.setDisabledIcon(left_icons.getDisabledIcon());
        stop_mod.setIcon(stop_icons.getStandardIcon());
        stop_mod.setRolloverIcon(stop_icons.getRolloverIcon());
        stop_mod.setPressedIcon(stop_icons.getPressedIcon());
        stop_mod.setDisabledIcon(stop_icons.getDisabledIcon());
    }

    /*
     * registrando un nuovo pulsante si può decidere quando è attivo e quando disabilitarlo, si assicura che tutti i
     * pulsanti sia nello stato corretto
     */
    public static void update_active_buttons() {
        for (Component button_obj : buttons_container.getComponents()) {
            String name = button_obj.getName();
            ButtonInfo info = buttons_info.get(name);

            if (info != null) {
                switch (info.ActiveWhen) {
                    case ButtonInfo.ALWAYS -> button_obj.setEnabled(true);
                    case ButtonInfo.CONNECTED -> button_obj.setEnabled(ServerInterface.is_connected());
                    case ButtonInfo.INCLASS -> button_obj.setEnabled(ServerInterface.is_in_class());
                }
            }
        }
    }

    public static void add_button(ButtonIcons icons, ButtonInfo info) {
        if (buttons_container != null) {
            Logger.log("impossibile aggiungere pulsanti alla ButtonTopBar_panel una volta inizializzato il pannello, nome: " + info.name, true);
            return;
        }

        added_buttons.add(new Pair<>(icons, info));
    }

    /*
     * aggiunge tutti i pulsanti specificati in added_buttons al pannello
     */
    private static void add_buttons_to_panel() {
        for (Pair<ButtonIcons, ButtonInfo> button_pair : added_buttons) {
            JButton button  = init_button(button_pair.el1, button_pair.el2);

            buttons_container.add(button);
            buttons_info.put(button_pair.el2.name, button_pair.el2);
        }
    }

    private static JButton init_button(ButtonIcons icons, ButtonInfo info) {
        JButton button = new JButton();
        init_button_appearance(button, icons);

        button.setName(info.name);
        button.addActionListener(_ -> {
            //non è permesso attivare più di una mod alla volta
            if (active_mod.isEmpty()) {
                Logger.log("faccio partire la mod: " + info.name);
                active_mod = info.name;

                info.pressed();
            }
            else {
                Logger.log("tentativo di far partire la mod: " + info.name + " mentre la mod: " + active_mod + " è attiva", true);
                TempPanel.show(new TempPanel_info(
                        TempPanel_info.SINGLE_MSG,
                        false,
                        "impossibile fare partire la mod: " + info.name + " mentre la mod: " + active_mod + " è attiva"
                ), null);
            }
        });

        switch (info.ActiveWhen) {
            case ButtonInfo.CONNECTED -> button.setEnabled(ServerInterface.is_connected());
            case ButtonInfo.INCLASS -> button.setEnabled(ServerInterface.is_in_class());
        }

        return button;
    }

    private static void init_button_appearance(JButton button, ButtonIcons icons) {
        button.setIcon(icons.getStandardIcon());
        button.setRolloverIcon(icons.getRolloverIcon());
        button.setPressedIcon(icons.getPressedIcon());
        button.setDisabledIcon(icons.getDisabledIcon());

        button.setBorder(null);

        button.setContentAreaFilled(false);
        button.setOpaque(false);
    }

    private static final ActionListener LEFTSHIFT_LISTENER = _ -> {
        buttons_scroller.getHorizontalScrollBar().setValue(
                buttons_scroller.getHorizontalScrollBar().getValue() - 30
        );
    };

    private static final ActionListener RIGHTSHIFT_LISTENER = _ -> {
        buttons_scroller.getHorizontalScrollBar().setValue(
                buttons_scroller.getHorizontalScrollBar().getValue() + 30
        );
    };

    private static final ActionListener STOP_LISTENER = _ -> {
        if (!active_mod.isEmpty()) {
            Logger.log("stoppo la mod: " + active_mod);
            buttons_info.get(active_mod).stop();
        }
    };
}
