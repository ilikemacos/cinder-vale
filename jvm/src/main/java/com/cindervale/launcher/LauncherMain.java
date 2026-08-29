package com.cindervale.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cinder Vale launcher — Lunar-Client-style front end: hero image, left nav,
 * news/dispatches panel, video settings, PLAY button that spawns the game as a
 * separate JVM process. Pure Swing (bundled with JDK), zero extra deps.
 *
 * Exit codes propagated from the game:
 *   0  clean exit (window closed / Quit Game)
 *   2  "Quit to launcher" — relaunch the launcher (handled here)
 */
public final class LauncherMain {

    // HearthLink amber-on-ink palette.
    static final Color AMBER = new Color(0xF5, 0xC2, 0x5B);
    static final Color AMBER_DIM = new Color(0x9E, 0x7E, 0x3D);
    static final Color INK = new Color(0x0F, 0x0F, 0x11);
    static final Color PANEL = new Color(0x18, 0x18, 0x1B);
    static final Color PANEL_DIM = new Color(0x1E, 0x1E, 0x22);
    static final Color TEXT = new Color(0xE6, 0xE6, 0xE8);
    static final Color TEXT_DIM = new Color(0x9E, 0x9E, 0xA4);
    static final Color GREEN = new Color(0x8A, 0xD7, 0x80);

    static Font TITLE_FONT;
    static Font MONO_FONT;

    // Video settings, kept in memory + persisted via prefs (skipped for v1).
    // "Native" auto-detects the display size — actual pixels chosen at launch time.
    private static String[] RES_LABELS = {"Native (auto)", "720p (HD)", "1080p (Full HD)", "1440p (QHD)", "2160p (4K UHD)"};
    private static int[][] RES_VALUES = {{-1,-1}, {1280,720}, {1920,1080}, {2560,1440}, {3840,2160}};
    private static int resIdx = 0;   // default to native
    private static int streamingIdx = 0;  // 0 = Low (stream tiles), 1 = High (whole valley)
    private static boolean fullscreen = false;

    /** Resolve the "Native (auto)" entry into the actual primary-display size. */
    private static int[] currentResolution() {
        int[] r = RES_VALUES[resIdx];
        if (r[0] > 0) return r;
        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        // Clamp to a sensible max so 5K displays don't demolish the GPU budget.
        int w = Math.min((int) s.getWidth(), 2560);
        int h = Math.min((int) s.getHeight(), 1440);
        return new int[]{w, h};
    }

    public static void main(String[] args) {
        // Force cross-platform (Metal) LAF so JButton.setBackground actually paints.
        // macOS's Aqua LAF uses native buttons and IGNORES setBackground — which was
        // making the amber PLAY button invisible on the dark footer panel.
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        // Prefer the game's bundled fonts (Oswald + Share Tech Mono); fall back to system.
        try {
            File oswald = new File("../godot-reference/assets/ui/fonts/Oswald.ttf");
            if (oswald.exists()) TITLE_FONT = Font.createFont(Font.TRUETYPE_FONT, oswald).deriveFont(Font.PLAIN, 48f);
            File mono = new File("../godot-reference/assets/ui/fonts/ShareTechMono.ttf");
            if (mono.exists()) MONO_FONT = Font.createFont(Font.TRUETYPE_FONT, mono).deriveFont(Font.PLAIN, 14f);
        } catch (Exception ignored) {}
        if (TITLE_FONT == null) TITLE_FONT = new Font("Helvetica Neue", Font.BOLD, 44);
        if (MONO_FONT == null) MONO_FONT = new Font("Menlo", Font.PLAIN, 13);

        SwingUtilities.invokeLater(LauncherMain::showLauncher);
    }

    private static void showLauncher() {
        JFrame f = new JFrame("Cinder Vale — Launcher");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1180, 720);
        f.setLocationRelativeTo(null);
        f.setContentPane(buildContent(f));
        f.setVisible(true);
    }

    private static JComponent buildContent(JFrame frame) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(INK);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(frame), BorderLayout.CENTER);
        root.add(buildFooter(frame), BorderLayout.SOUTH);
        return root;
    }

    private static JComponent buildHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(INK);
        p.setBorder(new EmptyBorder(24, 40, 8, 40));

        JLabel title = new JLabel("CINDER  VALE");
        title.setFont(TITLE_FONT);
        title.setForeground(AMBER);
        p.add(title);

        JLabel sub = new JLabel("> hearthlink launcher · pacific northwest wasteland_");
        sub.setFont(MONO_FONT);
        sub.setForeground(GREEN);
        sub.setBorder(new EmptyBorder(4, 2, 0, 0));
        p.add(sub);
        return p;
    }

    private static JComponent buildBody(JFrame frame) {
        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setBackground(INK);
        body.setBorder(new EmptyBorder(16, 40, 16, 40));

        body.add(buildNav(), BorderLayout.WEST);
        body.add(buildContentTabs(), BorderLayout.CENTER);
        return body;
    }

    private static JComponent buildNav() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(INK);
        nav.setPreferredSize(new Dimension(220, 400));

        for (String label : new String[]{"HOME", "SETTINGS", "ABOUT"}) {
            JButton b = navButton("▸  " + label);
            b.addActionListener(e -> switchTab(label));
            nav.add(b);
            nav.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        return nav;
    }

    private static JTabbedPane tabs;

    private static JComponent buildContentTabs() {
        tabs = new JTabbedPane();
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI());   // strip system chrome
        tabs.setBackground(PANEL);
        tabs.setForeground(TEXT);
        tabs.setFont(MONO_FONT);

        tabs.addTab("HOME", buildHomePanel());
        tabs.addTab("SETTINGS", buildSettingsPanel());
        tabs.addTab("ABOUT", buildAboutPanel());
        // Hide the tab bar — nav on the left drives it.
        tabs.setUI(new HiddenTabsUI());
        return tabs;
    }

    private static void switchTab(String label) {
        int i = switch (label) {
            case "SETTINGS" -> 1;
            case "ABOUT" -> 2;
            default -> 0;
        };
        tabs.setSelectedIndex(i);
    }

    private static JComponent buildHomePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel prompt = new JLabel("root@cindervale:~ $ cat dispatches");
        prompt.setFont(MONO_FONT);
        prompt.setForeground(GREEN);
        p.add(prompt);

        JLabel h = new JLabel("DISPATCHES FROM CINDER VALE");
        h.setFont(TITLE_FONT.deriveFont(26f));
        h.setForeground(AMBER);
        h.setBorder(new EmptyBorder(6, 0, 12, 0));
        p.add(h);

        for (String line : new String[]{
                "• The mill turbine is dead. Vale Salvage needs the exciter coil.",
                "• Ash Dogs raiders sighted at the quarry and the overpass camp.",
                "• Feral irradiated dogs nesting around the roadside clinic.",
                "• Red Cordon holds the dam. They are not friendly to scavvers.",
                "",
                "Press PLAY to wake in the bus wreck on the highway.",
                "WASD move · LMB fire · R reload · Shift sprint · Tab HearthLink · Esc menu",
        }) {
            JLabel l = new JLabel(line);
            l.setFont(MONO_FONT.deriveFont(15f));
            l.setForeground(TEXT);
            l.setBorder(new EmptyBorder(2, 0, 2, 0));
            p.add(l);
        }
        p.add(Box.createVerticalGlue());
        return wrapPanel(p);
    }

    private static JComponent buildSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 0, 8, 24);

        JLabel h = new JLabel("VIDEO SETTINGS");
        h.setFont(TITLE_FONT.deriveFont(26f));
        h.setForeground(AMBER);
        c.gridwidth = 2;
        p.add(h, c);
        c.gridwidth = 1;
        c.gridy++;

        addSettingRow(p, c, "Resolution", resDropdown());
        addSettingRow(p, c, "World streaming", streamingDropdown());
        addSettingRow(p, c, "Fullscreen", fullscreenToggle());

        c.gridy++;
        JLabel note = new JLabel("Tuned for Apple M1 / 8 GB. Lower resolution if fps dips below 30.");
        note.setFont(MONO_FONT);
        note.setForeground(TEXT_DIM);
        c.gridwidth = 2;
        p.add(note, c);

        return wrapPanel(p);
    }

    private static void addSettingRow(JPanel p, GridBagConstraints c, String label, JComponent field) {
        c.gridx = 0;
        JLabel l = new JLabel(label);
        l.setFont(MONO_FONT.deriveFont(15f));
        l.setForeground(TEXT);
        l.setPreferredSize(new Dimension(220, 28));
        p.add(l, c);
        c.gridx = 1;
        p.add(field, c);
        c.gridy++;
    }

    private static JComboBox<String> resDropdown() {
        // Show the detected native size next to "Native (auto)" so the user
        // can see what will actually be launched.
        Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
        String[] labels = RES_LABELS.clone();
        labels[0] = String.format("Native (auto) — %d × %d", (int) s.getWidth(), (int) s.getHeight());
        JComboBox<String> b = new JComboBox<>(labels);
        b.setSelectedIndex(resIdx);
        b.setBackground(PANEL_DIM);
        b.setForeground(TEXT);
        b.setFont(MONO_FONT);
        b.addActionListener(e -> resIdx = b.getSelectedIndex());
        return b;
    }

    private static JComboBox<String> streamingDropdown() {
        JComboBox<String> b = new JComboBox<>(new String[]{"Low  (stream tiles)", "High (whole valley)"});
        b.setSelectedIndex(streamingIdx);
        b.setBackground(PANEL_DIM);
        b.setForeground(TEXT);
        b.setFont(MONO_FONT);
        b.addActionListener(e -> streamingIdx = b.getSelectedIndex());
        return b;
    }

    private static JCheckBox fullscreenToggle() {
        JCheckBox b = new JCheckBox();
        b.setBackground(PANEL);
        b.setForeground(TEXT);
        b.setSelected(fullscreen);
        b.addActionListener(e -> fullscreen = b.isSelected());
        return b;
    }

    private static JComponent buildAboutPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel h = new JLabel("ABOUT");
        h.setFont(TITLE_FONT.deriveFont(26f));
        h.setForeground(AMBER);
        p.add(h);

        for (String line : new String[]{
                "Cinder Vale — an original open-world RPG.",
                "Rewritten on the JVM with jMonkeyEngine on Metal-backed OpenGL.",
                "",
                "Original IP. Not affiliated with any existing franchise.",
                "Characters & animations: Mixamo. Environment: Poly Haven (CC0).",
        }) {
            JLabel l = new JLabel(line);
            l.setFont(MONO_FONT.deriveFont(15f));
            l.setForeground(TEXT);
            l.setBorder(new EmptyBorder(4, 0, 4, 0));
            p.add(l);
        }
        return wrapPanel(p);
    }

    private static JComponent wrapPanel(JComponent inner) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(PANEL);
        wrap.setBorder(BorderFactory.createLineBorder(AMBER_DIM, 1));
        wrap.add(inner, BorderLayout.CENTER);
        return wrap;
    }

    private static JComponent buildFooter(JFrame frame) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0x08, 0x08, 0x0A));
        p.setBorder(new EmptyBorder(16, 40, 24, 40));

        JLabel v = new JLabel("v0.5-jvm  ·  build: wasteland-pbr");
        v.setFont(MONO_FONT);
        v.setForeground(TEXT_DIM);
        p.add(v, BorderLayout.WEST);

        JButton play = new PlayButton();
        play.setFont(TITLE_FONT.deriveFont(28f));
        play.addActionListener(e -> launchGame(frame));
        p.add(play, BorderLayout.EAST);
        return p;
    }

    /**
     * PLAY button that paints its own amber background so it's LAF-proof.
     * (macOS Aqua's native JButton ignores setBackground, which was making the
     * original invisible on the dark footer.)
     */
    private static final class PlayButton extends JButton {
        private boolean hover = false;
        PlayButton() {
            super("▶   PLAY");
            setForeground(new Color(0x08, 0x08, 0x0A));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setPreferredSize(new Dimension(300, 56));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? AMBER.brighter() : AMBER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static void launchGame(JFrame frame) {
        int[] res = currentResolution();
        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + "/bin/java");
        cmd.add("-XstartOnFirstThread");
        cmd.add("-Xmx2g");
        cmd.add("-Dcindervale.width=" + res[0]);
        cmd.add("-Dcindervale.height=" + res[1]);
        cmd.add("-Dcindervale.fullscreen=" + fullscreen);
        cmd.add("-Dcindervale.streaming=" + (streamingIdx == 1 ? "high" : "low"));
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add("com.cindervale.Main");

        try {
            frame.setVisible(false);
            ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
            Process proc = pb.start();
            int exit = proc.waitFor();
            if (exit == 2) {
                // Quit to launcher — reshow the launcher.
                frame.setVisible(true);
            } else {
                System.exit(exit);
            }
        } catch (IOException | InterruptedException ex) {
            frame.setVisible(true);
            JOptionPane.showMessageDialog(frame, "Failed to launch game:\n" + ex.getMessage(),
                    "Cinder Vale", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** JButton styled for the left nav. */
    private static JButton navButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(INK);
        b.setForeground(TEXT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(new Font("Helvetica Neue", Font.BOLD, 18));
        b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(AMBER); }
            @Override public void mouseExited(MouseEvent e) { b.setForeground(TEXT); }
        });
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    /** JTabbedPane UI that hides the tab strip — nav on the left drives selection. */
    private static final class HiddenTabsUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        @Override protected int calculateTabAreaHeight(int placement, int hRunCount, int maxTabHeight) { return 0; }
        @Override protected void paintTabArea(Graphics g, int placement, int selected) {}
    }
}
