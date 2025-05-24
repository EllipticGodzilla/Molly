package gui;

import files.Logger;
import gui.custom.*;
import gui.themes.GraphicsSettings;
import gui.temppanel.TempPanel;
import gui.temppanel.TempPanel_action;
import gui.temppanel.TempPanel_info;
import network.ServerInfo;
import network.ServerInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public abstract class ServerList_panel {

    private static JButton connect;
    private static JButton disconnect;
    private static JButton add_server;
    private static MList server_list;

    private static JPanel serverL_panel = null;
    protected static JPanel init() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        if (serverL_panel == null) {
            serverL_panel = new JPanel();
            serverL_panel.setOpaque(false);
            serverL_panel.setLayout(new GridBagLayout());
            serverL_panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 5));
            serverL_panel.setPreferredSize(new Dimension(250, 200));

            connect = new JButton();
            disconnect = new JButton();
            add_server = new JButton();
            server_list = new MList("server list");
            MScrollPane server_scroller = new MScrollPane(server_list);

            disconnect.setEnabled(false);

            server_scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            server_list.set_popup(new CellPopupMenu());

            //inizializza tutti i componenti della gui
            connect.setBorder(null);
            disconnect.setBorder(null);
            add_server.setBorder(null);

            connect.addActionListener(CONNECT_LISTENER);
            disconnect.addActionListener(disconnect_listener);
            add_server.addActionListener(ADDSERVER_LISTENER);

            connect.setOpaque(false);
            disconnect.setOpaque(false);
            add_server.setOpaque(false);
            connect.setContentAreaFilled(false);
            disconnect.setContentAreaFilled(false);
            add_server.setContentAreaFilled(false);

            //aggiunge tutti i componenti al pannello organizzando la gui
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.VERTICAL;
            c.anchor = GridBagConstraints.FIRST_LINE_START;
            c.weightx = 1;
            c.weighty = 0;
            c.gridx = 0;
            c.gridy = 0;

            serverL_panel.add(disconnect, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.LINE_START;
            c.weightx = 0;
            c.gridx = 1;
            c.insets.right = 10;

            serverL_panel.add(add_server, c);

            c.gridx = 2;
            c.insets.right = 0;

            serverL_panel.add(connect, c);

            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 3;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets.top = 10;

            serverL_panel.add(server_scroller, c);

            update_colors();
        }
        return serverL_panel;
    }

    public static void update_colors() {
        server_list.update_colors();

        ButtonIcons connect_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("server_panel_connect");
        ButtonIcons disconnect_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("server_panel_disconnect");
        ButtonIcons add_server_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("server_panel_add_server");

        connect.setIcon(connect_icons.getStandardIcon());
        connect.setRolloverIcon(connect_icons.getRolloverIcon());
        connect.setPressedIcon(connect_icons.getPressedIcon());
        connect.setDisabledIcon(connect_icons.getDisabledIcon());
        disconnect.setIcon(disconnect_icons.getStandardIcon());
        disconnect.setRolloverIcon(disconnect_icons.getRolloverIcon());
        disconnect.setPressedIcon(disconnect_icons.getPressedIcon());
        disconnect.setDisabledIcon(disconnect_icons.getDisabledIcon());
        add_server.setIcon(add_server_icons.getStandardIcon());
        add_server.setRolloverIcon(add_server_icons.getRolloverIcon());
        add_server.setPressedIcon(add_server_icons.getPressedIcon());
        add_server.setDisabledIcon(add_server_icons.getDisabledIcon());
    }

    public static String get_selected_server() {
        return server_list.getSelectedValue();
    }

    public static void add_server(String name) {
        server_list.add(name);
    }

    public static void rename_server(String old_name, String new_name) {
        server_list.rename_element(old_name, new_name);
    }

    public static void remove_server(String name) {
        server_list.remove(name);
    }

    /*
     * aggiorna la lista di server disponibili nella lista da ServerInterface
     */
    public static void update_gui() {
        server_list.setList(ServerInterface.get_server_list());
    }

    //rimuove ogni elemento dalla lista
    public static void clear() {
        server_list.clear();
    }

    public static void update_buttons() {
        connect.setEnabled(!ServerInterface.is_connected());
        disconnect.setEnabled(ServerInterface.is_connected());
    }

    private static final ActionListener ADDSERVER_LISTENER = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_MSG,
                    true,
                    "nome:",
                    "link:",
                    "ip:",
                    "porta:",
                    "dns:",
                    "encoder:",
                    "connector:"
            ).set_combo_box(
                    new int[] {4, 5, 6},
                    ServerInterface.get_dns_list().toArray(new String[0]),
                    ServerInterface.get_encoder_list().toArray(new String[0]),
                    ServerInterface.get_server_connectors_list().toArray(new String[0])
            ), ADD_SERVER_ACTION);
        }

        private static final TempPanel_action ADD_SERVER_ACTION = new TempPanel_action() {
            @Override
            public void success()  {
                String name = (String) input.elementAt(0);
                String link = (String) input.elementAt(1);
                String ip = (String) input.elementAt(2);
                String port_str = (String) input.elementAt(3);
                int port;
                String dns = (String) input.elementAt(4);
                String encoder = (String) input.elementAt(5);
                String connector = (String) input.elementAt(6);

                try {
                    port = Integer.parseInt(port_str);
                }
                catch (NumberFormatException _) { //non è stato inserito un numero per porta
                    Logger.log("tentativo di aggiungere un server con come porta una stringa: " + port_str, true);

                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "valore per la porta del server non valido, riprovare"
                    ), null);

                    ADDSERVER_LISTENER.actionPerformed(null);
                    return;
                }

                if (dns == null && ip.isEmpty()) { //se non si vuole specificare un dns si deve specificare l'ip del server
                    Logger.log("tentativo di aggiungere un server senza specificare dns e ip", true);

                    TempPanel.show(new TempPanel_info(
                            TempPanel_info.SINGLE_MSG,
                            false,
                            "per aggiungere un server senza dns specificare il suo indirizzo ip"
                    ), null);

                    ADDSERVER_LISTENER.actionPerformed(null);
                    return;
                }

                ServerInfo info = new ServerInfo(name, link, ip, port, dns, encoder, connector);

                ServerInterface.add_server(name, info);
                server_list.add(name);
            }

            @Override
            public void annulla() {} //non si vuole più aggiungere il server
        };
    };

    private static final ActionListener CONNECT_LISTENER = _ -> {
        String server_name = server_list.getSelectedValue();
        if (!server_name.isEmpty()) { //se è effettivamente selezionato un server
            Logger.log("tento la connessione con il server: " + server_name);

            ServerInterface.connect(server_name);
        }
    };

    private static final ActionListener disconnect_listener = _ -> {
        ServerInterface.close(true);
    };
}

class CellPopupMenu extends MPopupMenu {
    public CellPopupMenu() {
        super();

        MMenuItem rename = new MMenuItem("rename");
        MMenuItem remove = new MMenuItem("remove");
        MMenuItem info = new MMenuItem("info");

        rename.addActionListener(_ -> {

            String selected_name = ServerList_panel.get_selected_server();
            if (selected_name == null) { //nessun server è selezionato dalla lista
                return;
            }

            TempPanel_action result_action = new TempPanel_action() {
                @Override
                public void success() {
                    String new_name = (String) input.elementAt(0);

                    Logger.log("rinomino il server: (" + selected_name + ") in: " + new_name);
                    ServerList_panel.rename_server(selected_name, new_name);

                    //rinomina il server nella lista dei server registrati
                    ServerInterface.rename_server(selected_name, new_name);
                }
            };

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_MSG,
                    true,
                    "nuovo nome per: " + selected_name
            ), result_action);
        });

        remove.addActionListener(_ -> {
            String selected_name = ServerList_panel.get_selected_server();
            if (selected_name == null) { //nessun server è selezionato dalla lista
                return;
            }

            TempPanel_action result_action = new TempPanel_action() {
                @Override
                public void success() {
                    Logger.log("rimuovo il server: " + selected_name + " dalla lista dei server memorizzati");

                    ServerInterface.remove_server(selected_name);
                    ServerList_panel.remove_server(selected_name);
                }
            };

            TempPanel.show(new TempPanel_info( //chiede la conferma prima di rimuovere un server
                    TempPanel_info.SINGLE_MSG,
                    true,
                    "il server " + selected_name + " verrà rimosso"
            ), result_action);
        });

        info.addActionListener(_ -> {
            String selected_name = ServerList_panel.get_selected_server();
            if (selected_name == null) { //nessun server è selezionato dalla lista
                return;
            }

            ServerInfo server_info = ServerInterface.get_server_info(selected_name);

            TempPanel.show(new TempPanel_info(
                    TempPanel_info.DOUBLE_COL_MSG,
                    false,
                    "nome:", selected_name,
                    "ip:", (server_info.SERVER_IP == null)? ">not defined<" : server_info.SERVER_IP,
                    "link:", (server_info.SERVER_LINK == null)? ">not defined<" : server_info.SERVER_LINK,
                    "porta:", Integer.toString(server_info.SERVER_PORT),
                    "dns:", (server_info.DNS_NAME == null)? ">not defined<" : server_info.DNS_NAME,
                    "encoder:", server_info.ENCODER,
                    "connector:", server_info.CONNECTOR
            ), null);
        });

        this.add(rename);
        this.add(remove);
        this.add(info);
    }
}

//class CellPopupMenu extends JPopupMenu {
//    private String cell_name;
//    private GList parent_list;
//
//    public CellPopupMenu(String name, GList list) {
//        super();
//        this.cell_name = name;
//        this.parent_list = list;
//
//        JMenuItem rename = new JMenuItem("rename");
//        JMenuItem remove = new JMenuItem("remove");
//        JMenuItem info = new JMenuItem("info");
//
//        Border item_border = BorderFactory.createCompoundBorder(
//                BorderFactory.createLineBorder(new Color(78, 81, 83)),
//                BorderFactory.createEmptyBorder(4, 2, 0, 0)
//        );
//        this.setBorder(BorderFactory.createLineBorder(new Color(28, 31, 33)));
//        rename.setBorder(item_border);
//        remove.setBorder(item_border);
//        info.setBorder(item_border);
//
//        rename.setBackground(new Color(88, 91, 93));
//        remove.setBackground(new Color(88, 91, 93));
//        info.setBackground(new Color(88, 91, 93));
//        rename.setForeground(Color.lightGray);
//        remove.setForeground(Color.lightGray);
//        info.setForeground(Color.lightGray);
//
//        rename.addActionListener(RENAME_LISTENER);
//        remove.addActionListener(REMOVE_LISTENER);
//        info.addActionListener(INFO_LISTENER);
//
//        this.add(rename);
//        this.add(remove);
//        this.add(info);
//    }
//
//    private final ActionListener RENAME_LISTENER = _ -> {
//        Vector<Object> result = TempPanel.show(new TempPanel_info(
//                TempPanel_info.INPUT_REQ,
//                true,
//                "nuovo nome per: " + cell_name
//        ), Thread.currentThread());
//
//        if (result != null) {
//            String new_name = (String) result.elementAt(0);
//
//            Logger.log("rinomino il server: " + cell_name + " in: " + new_name);
//            parent_list.rename_element(cell_name, new_name); //modifica il nome nella lista visibile
//
//            //rinomina il server nella lista dei server registrati
//            ServerInterface.rename_server(cell_name, new_name);
//
//            cell_name = new_name; //modifica il nome per questo popup
//        }
//    };
//
//    private final ActionListener REMOVE_LISTENER = _ -> {
//        Vector<Object> result = TempPanel.show(new TempPanel_info( //chiede la conferma prima di rimuovere un server
//                TempPanel_info.SINGLE_MSG,
//                true,
//                "il server " + cell_name + " verrà rimosso"
//        ), Thread.currentThread());
//
//        //se viene premuto annulla result = null, altrimenti è un vettore vuoto
//        if (result != null) {
//            Logger.log("rimuovo il server: " + cell_name + " dalla lista dei server memorizzati");
//
//            ServerInterface.remove_server(cell_name);
//            parent_list.remove(cell_name); //rimuove il server dalla lista visibile
//        }
//    };
//
//    private final ActionListener INFO_LISTENER = _ -> {
//        ServerInfo info = ServerInterface.get_server_info(cell_name);
//
//        TempPanel.show(new TempPanel_info(
//                TempPanel_info.DOUBLE_COL_MSG,
//                false,
//                "nome:", cell_name,
//                "ip:", (info.SERVER_IP == null)? ">not defined<" : info.SERVER_IP,
//                "link:", (info.SERVER_LINK == null)? ">not defined<" : info.SERVER_LINK,
//                "porta:", Integer.toString(info.SERVER_PORT),
//                "dns:", (info.DNS_NAME == null)? ">nod defined<" : info.DNS_NAME,
//                "encoder:", info.ENCODER,
//                "connector:", info.CONNECTOR
//        ), null);
//    };
//}