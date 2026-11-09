package com.mcpmc.terminal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TerminalWindow {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JFrame frame;
    private final JLabel statusLabel;
    private final JTextArea log;
    private final JLabel portLabel;
    private volatile String currentStatus = "Starting";

    private TerminalWindow(JFrame frame, JLabel statusLabel, JTextArea log, JLabel portLabel) {
        this.frame = frame;
        this.statusLabel = statusLabel;
        this.log = log;
        this.portLabel = portLabel;
    }

    public static TerminalWindow open() {
        if (GraphicsEnvironment.isHeadless()) {
            return headless();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JFrame frame = new JFrame("Minecraft MCP");
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.setSize(720, 420);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel title = new JLabel("Minecraft MCP");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JLabel status = new JLabel("Starting");
        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        status.setForeground(new Color(0xB38F00));
        top.add(title, BorderLayout.WEST);
        top.add(status, BorderLayout.EAST);

        JTextArea log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBackground(new Color(0x111318));
        log.setForeground(new Color(0xE6E6E6));
        log.setCaretColor(Color.WHITE);
        JScrollPane scroll = new JScrollPane(log);

        JLabel port = new JLabel(" ");
        port.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        port.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(port, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { frame.setVisible(false); }
        });
        frame.setVisible(true);

        return new TerminalWindow(frame, status, log, port);
    }

    private static TerminalWindow headless() {
        return new TerminalWindow(null, null, null, null) {
            @Override public void status(String s) { System.out.println("[MCP] status: " + s); }
            @Override public void info(String s)   { System.out.println("[MCP] " + s); }
            @Override public void error(String s)  { System.err.println("[MCP] ERR: " + s); }
        };
    }

    public void status(String s) {
        currentStatus = s;
        if (statusLabel == null) return;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(s);
            statusLabel.setForeground(switch (s) {
                case "Connected" -> new Color(0x2DAA00);
                case "Failed", "Network error" -> new Color(0xD04040);
                default -> new Color(0xB38F00);
            });
            append("[" + ts() + "] status -> " + s);
            if ("Connected".equals(s)) {
                portLabel.setText(" MCP ready — paste the HTTP endpoint below into your AI client ");
                portLabel.setForeground(new Color(0x2DAA00));
            }
        });
    }

    public void info(String msg) {
        if (log == null) return;
        SwingUtilities.invokeLater(() -> append("[" + ts() + "] " + msg));
    }

    public void error(String msg) {
        if (log == null) return;
        SwingUtilities.invokeLater(() -> append("[" + ts() + "] ERROR " + msg));
    }

    public String currentStatus() { return currentStatus; }

    private void append(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static String ts() { return LocalTime.now().format(TS); }
}
