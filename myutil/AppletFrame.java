package myutil;

import java.awt.*;

/**
 * Frame wrapper for SimApp panels.
 * Replaces the Applet container functionality.
 */
public class AppletFrame extends Frame {
    private SimApp app;

    public AppletFrame(Panel panel, int x, int y) {
        if (!(panel instanceof SimApp)) {
            throw new IllegalArgumentException("Panel must implement SimApp interface");
        }
        this.app = (SimApp) panel;

        setTitle(panel.getClass().getSimpleName());
        setSize(x, y);
        add("Center", panel);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                app.stop();
                System.exit(0);
            }
        });

        app.init();
        setVisible(true);
        app.start();
    }
}
