package nl.esciencecenter.visualization.amuse.planetformation;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;

import openglCommon.util.InputHandler;

public class AmuseApp {
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    public static void main(String[] arguments) {
        String cmdlnfileName = null;
        String path = "";

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("-o")) {
                i++;
                cmdlnfileName = arguments[i];
                final File cmdlnfile = new File(cmdlnfileName);
                path = cmdlnfile.getPath().substring(0, cmdlnfile.getPath().length() - cmdlnfile.getName().length());
            } else if (arguments[i].equals("-resume")) {
                i++;
                AmuseApp.settings.setInitial_simulation_frame(Integer.parseInt(arguments[i]));
                i++;
                AmuseApp.settings.setInitial_rotation_x(Float.parseFloat(arguments[i]));
                i++;
                AmuseApp.settings.setInitial_rotation_y(Float.parseFloat(arguments[i]));
            } else {
                cmdlnfileName = null;
                path = System.getProperty("user.dir");
            }
        }

        final JFrame frame = new JFrame("Amuse Visualization");
        frame.setPreferredSize(new Dimension(AmuseApp.settings.getDefaultScreenWidth(), AmuseApp.settings
                .getDefaultScreenHeight()));

        final AmuseWindow amuseWindow = new AmuseWindow(InputHandler.getInstance(), true);
        final AmusePanel amusePanel = new AmusePanel(amuseWindow, path, cmdlnfileName);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    frame.getContentPane().add(amusePanel);

                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent we) {
                            amusePanel.close();
                            System.exit(0);
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        });

        // Display the window.
        frame.pack();

        // center on screen
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}
