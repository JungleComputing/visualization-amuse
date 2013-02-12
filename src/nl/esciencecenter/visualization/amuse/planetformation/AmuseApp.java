package nl.esciencecenter.visualization.amuse.planetformation;

import java.io.File;

import javax.swing.JFrame;

import nl.esciencecenter.visualization.openglCommon.NewtWindow;
import nl.esciencecenter.visualization.openglCommon.input.InputHandler;
import nl.esciencecenter.visualization.openglCommon.util.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseApp {
    private final static Settings settings = Settings.getInstance();
    private final static Logger   log      = LoggerFactory
                                                   .getLogger(AmuseApp.class);

    private static AmusePanel     amusePanel;
    private static AmuseWindow    amuseWindow;

    public static void main(String[] arguments) {
        String cmdlnfileName = null;
        String path = "";

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("-o")) {
                i++;
                cmdlnfileName = arguments[i];
                final File cmdlnfile = new File(cmdlnfileName);
                path = cmdlnfile.getPath().substring(
                        0,
                        cmdlnfile.getPath().length()
                                - cmdlnfile.getName().length());
            } else if (arguments[i].equals("-resume")) {
                i++;
                AmuseApp.settings.setInitial_simulation_frame(Integer
                        .parseInt(arguments[i]));
                i++;
                AmuseApp.settings.setInitial_rotation_x(Float
                        .parseFloat(arguments[i]));
                i++;
                AmuseApp.settings.setInitial_rotation_y(Float
                        .parseFloat(arguments[i]));
            } else {
                cmdlnfileName = null;
                path = System.getProperty("user.dir");
            }
        }

        // Create the Swing interface elements
        amusePanel = new AmusePanel(path, cmdlnfileName);

        // Create the GLEventListener
        amuseWindow = new AmuseWindow(InputHandler.getInstance());

        NewtWindow window = new NewtWindow(true, amuseWindow.getInputHandler(),
                amuseWindow, settings.getDefaultScreenWidth(),
                settings.getDefaultScreenHeight(), "Amuse Visualization");

        // Create the frame
        final JFrame frame = new JFrame("Amuse Visualization");
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent arg0) {
                System.exit(0);
            }
        });

        frame.setSize(settings.getInterfaceWidth(),
                settings.getInterfaceHeight());

        frame.setResizable(false);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    frame.getContentPane().add(amusePanel);
                } catch (final Exception e) {
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        });

        frame.setVisible(true);
    }
}
