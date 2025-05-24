package gui.custom;

import files.FileInterface;
import gui.settingsFrame.*;
import gui.themes.GraphicsSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MFrame extends JFrame {
    private final JMenuBar menu_bar = new JMenuBar();
    private final Map<String, MMenu> available_menu = new LinkedHashMap<>();
    private final JButton iconize = new JButton(),
                          fullscreen = new JButton(),
                          exit = new JButton();

    private final FileManagerPanel file_manager_panel = new FileManagerPanel();
    private final MollySettingsPanel settings_panel = new MollySettingsPanel();
    private final ModManagerPanel mod_settings_panel = new ModManagerPanel();
    private final ServerSettingsPanel server_settings_panel = new ServerSettingsPanel();
    private final DnsSettingsPanel dns_settings_panel = new DnsSettingsPanel();

    /*
     * inizializza il frame, viene richiesto un layered pane poiché se si imposta la manu bar prima di impostare il layered
     * pane questa viene cancellata
     */
    public MFrame(MLayeredPane layeredPane) {
        super();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(900, 500);
        this.setUndecorated(true);
        this.setLayeredPane(layeredPane);
        menu_bar.addMouseListener(menu_bar_mouse_listener);
        menu_bar.setBorderPainted(false);
        menu_bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        FrameResizer resizer = new FrameResizer();
        this.addMouseListener(resizer);
        this.addMouseMotionListener(resizer);

        update_colors();

        //inizializza tutti i componenti
        iconize.setOpaque(false);
        fullscreen.setOpaque(false);
        exit.setOpaque(false);
        iconize.setContentAreaFilled(false);
        fullscreen.setContentAreaFilled(false);
        exit.setContentAreaFilled(false);
        iconize.setBorder(null);
        fullscreen.setBorder(null);
        exit.setBorder(null);

        iconize.addActionListener(_ -> this.setState(JFrame.ICONIFIED));
        fullscreen.addActionListener(_ -> this.setExtendedState(this.getExtendedState() ^ JFrame.MAXIMIZED_BOTH));
        exit.addActionListener(_ -> System.exit(0));

        menu_bar.add(exit);
        menu_bar.add(fullscreen);
        menu_bar.add(iconize);

        menu_bar.add(Box.createHorizontalGlue());

        init_menu_bar();

        this.setJMenuBar(menu_bar);
        layeredPane.set_top_border(menu_bar.getPreferredSize().height);
    }

    public void update_colors() {
        this.setBackground((Color) GraphicsSettings.active_theme().get_value("frame_background"));
        menu_bar.setBackground((Color) GraphicsSettings.active_theme().get_value("title_bar_background"));

        for (Component menu : menu_bar.getComponents()) {
            if (menu instanceof MMenu m) {
                m.update_colors();
            }
            else if (menu instanceof MMenuItem i) {
                i.update_colors();
            }
        }

        update_buttons_icons();
    }

    private void update_buttons_icons() {
        ButtonIcons max_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("title_bar_maximize");
        ButtonIcons min_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("title_bar_iconize");
        ButtonIcons close_icons = (ButtonIcons) GraphicsSettings.active_theme().get_value("title_bar_close");

        fullscreen.setIcon(max_icons.getStandardIcon());
        fullscreen.setRolloverIcon(max_icons.getRolloverIcon());
        fullscreen.setPressedIcon(max_icons.getPressedIcon());
        iconize.setIcon(min_icons.getStandardIcon());
        iconize.setRolloverIcon(min_icons.getRolloverIcon());
        iconize.setPressedIcon(min_icons.getPressedIcon());
        exit.setIcon(close_icons.getStandardIcon());
        exit.setRolloverIcon(close_icons.getRolloverIcon());
        exit.setPressedIcon(close_icons.getPressedIcon());
    }

    private void init_menu_bar() {
        add_menu("connection/server manager", () -> SettingsFrame.show(server_settings_panel, "server settings"));
        add_menu("connection/dns manager", () -> SettingsFrame.show(dns_settings_panel, "dns settings"));

        add_menu("mod/manager", () -> SettingsFrame.show(mod_settings_panel, "mod manager"));

        add_menu("file/manage files", () -> SettingsFrame.show(file_manager_panel, "file manager"));
        add_menu("file/reload all", FileInterface::load_from_disk);
        add_menu("file/update files", FileInterface::update_files);
        add_menu("file/settings", () -> SettingsFrame.show(settings_panel, "settings"));
        add_menu("file/exit", () -> System.exit(0));
    }

    private final MouseListener menu_bar_mouse_listener = new MouseListener() {
        private boolean follow = false;
        private boolean first_click = false;
        private boolean double_click = false;
        private Point click_point;

        private final ScheduledExecutorService scheduled_executor = Executors.newScheduledThreadPool(2);
        private ScheduledFuture<?> mouse_schedule;

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {}
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {}
        @Override
        public void mouseExited(MouseEvent mouseEvent) {}

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            //inizia a muovere il frame per seguire il mouse
            follow = true;
            click_point = mouseEvent.getPoint();

            mouse_schedule = scheduled_executor.scheduleAtFixedRate(follow_cursor,0, 10, TimeUnit.MILLISECONDS);

            //controlla per il doppio click
            if (first_click) {
                double_click = true;
            }
            else {
                first_click = true;
                scheduled_executor.schedule(check_double_click, 200, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) { //smette di seguire il mouse
            follow = false;
        }

        private final Runnable check_double_click = () -> {
            if (double_click) {
                setExtendedState(getExtendedState() ^ JFrame.MAXIMIZED_BOTH);
            }

            first_click = double_click = false; //resetta tutto
        };

        private final Runnable follow_cursor = () -> {
            if (follow) {
                Point mouse_position = MouseInfo.getPointerInfo().getLocation();

                setLocation(
                        mouse_position.x - click_point.x,
                        mouse_position.y - click_point.y
                );
            }
            else {
                mouse_schedule.cancel(true);
            }
        };
    };

    /*
     * aggiunge un elemento al menu specificando la sua path, "<menu1>/<menu2>/.." e quale azione eseguire quando si preme
     * ritorna true se è riuscito ad aggiungere il nuovo menu, false altrimenti
     */
    public boolean add_menu(String menu_path, Runnable action) {
        int root_menu_len = menu_path.indexOf('/');
        if (root_menu_len == -1) { //vuole aggiungere un menu item direttamente alla menu bar
            MMenuItem new_item = new MMenuItem(menu_path);
            new_item.addActionListener((_) -> action.run());
            new_item.setBorder(BorderFactory.createEmptyBorder(4, 0, 0,0));

            menu_bar.add(new_item);
            return true;
        }
        else { //aggiunge un menu item in uno dei menu nella menu bar
            String root_menu_name = menu_path.substring(0, root_menu_len);
            String remaining_path = menu_path.substring(root_menu_len + 1);

            MMenu root_menu = available_menu.get(root_menu_name);
            if (root_menu == null) { //non è ancora stato aggiunto un menu con quel nome alla menu bar
                root_menu = new MMenu(root_menu_name);
                available_menu.put(root_menu_name, root_menu);
                root_menu.setBorder(BorderFactory.createEmptyBorder(4, 0, 0,0));

                menu_bar.add(root_menu);
            }

            return root_menu.add(remaining_path, action);
        }
    }
}

//    LET THE USER RESIZE THE JFRAME, revisit of https://github.com/tips4java/tips4java/blob/main/source/ComponentResizer.java
class FrameResizer extends MouseAdapter {
    private final Dimension MIN_SIZE = new Dimension(900, 500);

    private int direction;
    protected static final int NORTH = 1;
    protected static final int WEST = 2;
    protected static final int SOUTH = 4;
    protected static final int EAST = 8;

    private static final int RESIZE_EVENT_BORDER = 5;

    private Cursor sourceCursor;
    private boolean resizing;
    private Rectangle bounds;
    private Point pressed;

    private static final Map<Integer, Integer> CURSORS = new HashMap<>();
    static {
        CURSORS.put(1, Cursor.N_RESIZE_CURSOR);
        CURSORS.put(2, Cursor.W_RESIZE_CURSOR);
        CURSORS.put(4, Cursor.S_RESIZE_CURSOR);
        CURSORS.put(8, Cursor.E_RESIZE_CURSOR);
        CURSORS.put(3, Cursor.NW_RESIZE_CURSOR);
        CURSORS.put(9, Cursor.NE_RESIZE_CURSOR);
        CURSORS.put(6, Cursor.SW_RESIZE_CURSOR);
        CURSORS.put(12, Cursor.SE_RESIZE_CURSOR);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        Component source = e.getComponent();
        Point location = e.getPoint();
        direction = 0;

        if (location.x < RESIZE_EVENT_BORDER)
            direction += WEST;

        if (location.x > source.getWidth() - RESIZE_EVENT_BORDER - 1)
            direction += EAST;

        if (location.y < RESIZE_EVENT_BORDER)
            direction += NORTH;

        if (location.y > source.getHeight() - RESIZE_EVENT_BORDER - 1)
            direction += SOUTH;

        //  Mouse is no longer over a resizable border

        if (direction == 0)
        {
            source.setCursor( sourceCursor );
        }
        else  // use the appropriate resizable cursor
        {
            int cursorType = CURSORS.get( direction );
            Cursor cursor = Cursor.getPredefinedCursor( cursorType );
            source.setCursor( cursor );
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        if (! resizing)
        {
            Component source = e.getComponent();
            sourceCursor = source.getCursor();
        }
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        if (! resizing)
        {
            Component source = e.getComponent();
            source.setCursor( sourceCursor );
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        //	The mouseMoved event continually updates this variable

        if (direction == 0) return;

        //  Setup for resizing. All future dragging calculations are done based
        //  on the original bounds of the component and mouse pressed location.

        resizing = true;

        Component source = e.getComponent();
        pressed = e.getPoint();
        SwingUtilities.convertPointToScreen(pressed, source);
        bounds = source.getBounds();
    }

    /**
     *  Restore the original state of the Component
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
        resizing = false;

        Component source = e.getComponent();
        source.setCursor( sourceCursor );

        Component parent = source.getParent();

        if (parent != null)
        {
            if (parent instanceof JComponent)
            {
                ((JComponent)parent).revalidate();
            }
            else
            {
                parent.validate();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!resizing) return;

        Component source = e.getComponent();
        Point dragged = e.getPoint();
        SwingUtilities.convertPointToScreen(dragged, source);

        changeBounds(source, direction, bounds, pressed, dragged);
    }

    protected void changeBounds(Component source, int direction, Rectangle bounds, Point pressed, Point current)
    {
        //  Start with original locaton and size

        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;

        //  Resizing the West or North border affects the size and location

        if (WEST == (direction & WEST))
        {
            int drag = pressed.x - current.x;
            int maximum = Math.min(width + x - 10, Integer.MAX_VALUE);
            drag = getDragBounded(drag, width, MIN_SIZE.width, maximum);

            x -= drag;
            width += drag;
        }

        if (NORTH == (direction & NORTH))
        {
            int drag = pressed.y - current.y;
            int maximum = Math.min(height + y - 10, Integer.MAX_VALUE);
            drag = getDragBounded(drag, height, MIN_SIZE.height, maximum);

            y -= drag;
            height += drag;
        }

        //  Resizing the East or South border only affects the size

        if (EAST == (direction & EAST))
        {
            int drag = current.x - pressed.x;
            Dimension boundingSize = getBoundingSize( source );
            int maximum = Math.min(boundingSize.width - x, Integer.MAX_VALUE);
            drag = getDragBounded(drag, width, MIN_SIZE.width, maximum);
            width += drag;
        }

        if (SOUTH == (direction & SOUTH))
        {
            int drag = current.y - pressed.y;
            Dimension boundingSize = getBoundingSize( source );
            int maximum = Math.min(boundingSize.height - y, Integer.MAX_VALUE);
            drag = getDragBounded(drag, height, MIN_SIZE.height, maximum);
            height += drag;
        }

        source.setBounds(x, y, width, height);
        source.validate();
    }

    /*
     *  Adjust the drag value to be within the minimum and maximum range.
     */
    private int getDragBounded(int drag, int dimension, int minimum, int maximum)
    {
        if (dimension + drag < minimum)
            drag = minimum - dimension;

        if (dimension + drag > maximum)
            drag = maximum - dimension;

        return drag;
    }

    /*
     *  Keep the size of the component within the bounds of its parent.
     */
    private Dimension getBoundingSize(Component source)
    {
        if (source instanceof Window)
        {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = env.getMaximumWindowBounds();
            return new Dimension(bounds.width, bounds.height);
        }
        else
        {
            Dimension d = source.getParent().getSize();
            d.width += -10;
            d.height += -10;
            return d;
        }
    }
}