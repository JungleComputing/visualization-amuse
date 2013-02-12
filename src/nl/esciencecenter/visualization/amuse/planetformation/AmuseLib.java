package nl.esciencecenter.visualization.amuse.planetformation;

import javax.swing.JFrame;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.openglCommon.NewtWindow;
import nl.esciencecenter.visualization.openglCommon.input.InputHandler;
import nl.esciencecenter.visualization.openglCommon.util.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseLib {
    private final static Settings settings = Settings.getInstance();
    private final static Logger   log      = LoggerFactory
                                                   .getLogger(AmuseLib.class);

    private static AmusePanel     amusePanel;
    private static AmuseWindow    amuseWindow;

    public static void main(String[] arguments) {
        // Create the Swing interface elements
        amusePanel = new AmusePanel();

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

    public void addScene(Scene scene) {
        AmusePanel.getTimer().getDatasetManager().addScene(scene);
    }
}
