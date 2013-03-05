package nl.esciencecenter.visualization.amuse.planetformation;

import javax.swing.JFrame;

import nl.esciencecenter.visualization.amuse.planetformation.glue.PointGas;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Star;
import nl.esciencecenter.visualization.amuse.planetformation.glue.data.GlueTimedPlayer;
import nl.esciencecenter.visualization.amuse.planetformation.input.AmuseInputHandler;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.TimedPlayer;
import nl.esciencecenter.visualization.openglCommon.NewtWindow;
import nl.esciencecenter.visualization.openglCommon.util.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseLib {
    private final static Settings settings = Settings.getInstance();
    private final static Logger   log      = LoggerFactory
                                                   .getLogger(AmuseLib.class);

    private static AmusePanel     amusePanel;
    private static AmuseWindow    amuseWindow;

    public AmuseLib() {
        // Create the Swing interface elements
        amusePanel = new AmusePanel();

        // Create the GLEventListener
        amuseWindow = new AmuseWindow(AmuseInputHandler.getInstance());

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

    public static void main(String[] args) {
        AmuseLib lib = new AmuseLib();

        Star[] stars1 = new Star[10];
        for (int i = 0; i < stars1.length; i++) {
            float[] coordinates = new float[] {
                    (float) Math.random(), (float) Math.random(), (float) Math.random()
            };
            float[] color = new float[] {
                    (float) Math.random(), (float) Math.random(), (float) Math.random(), 1f
            };
            stars1[i] = new Star(i, coordinates, (float) Math.random() * 0.2f, color);
        }

        PointGas[] pGas1 = new PointGas[1000];
        for (int i = 0; i < pGas1.length; i++) {
            float[] coordinates = new float[] {
                    (float) Math.random(), (float) Math.random(), (float) Math.random()
            };
            float[] color = new float[] {
                    (float) Math.random(), (float) Math.random(), (float) Math.random(), 1f
            };
            pGas1[i] = new PointGas(i, coordinates, color);
        }

        Scene scene1 = new Scene(0, null, stars1, null, null, pGas1);

        lib.addScene(scene1);

        // Star[] stars2 = new Star[10];
        // for (int i = 0; i < stars2.length; i++) {
        // float[] coordinates = new float[] {
        // (float) Math.random(), (float) Math.random(), (float) Math.random()
        // };
        // float[] color = new float[] {
        // (float) Math.random(), (float) Math.random(), (float) Math.random(),
        // 1f
        // };
        // stars2[i] = new Star(i, coordinates, (float) Math.random() * 0.2f,
        // color);
        // }
        //
        // PointGas[] pGas2 = new PointGas[1000];
        // for (int i = 0; i < pGas2.length; i++) {
        // float[] coordinates = new float[] {
        // (float) Math.random(), (float) Math.random(), (float) Math.random()
        // };
        // float[] color = new float[] {
        // (float) Math.random(), (float) Math.random(), (float) Math.random(),
        // 1f
        // };
        // pGas2[i] = new PointGas(i, coordinates, color);
        // }
        //
        // Scene scene2 = new Scene(0, null, stars2, null, null, pGas2);

        // lib.addScene(scene2);

    }

    public void addScene(Scene scene) {
        TimedPlayer timer = AmusePanel.getTimer();
        ((GlueTimedPlayer) timer).addScene(scene);

        if (!timer.isInitialized()) {
            ((GlueTimedPlayer) timer).init();

            new Thread(timer).start();
        }
    }
}
