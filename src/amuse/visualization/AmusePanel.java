package amuse.visualization;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import openglCommon.CommonPanel;
import openglCommon.util.InputHandler;
import util.GoggleSwing;
import amuse.visualization.amuseAdaptor.Hdf5TimedPlayer;

public class AmusePanel extends CommonPanel {
    public static enum TweakState {
        NONE, VISUAL, MOVIE
    }

    private final AmuseSettings settings         = AmuseSettings.getInstance();

    private static final long   serialVersionUID = 1L;

    public static Hdf5TimedPlayer getTimer() {
        return AmusePanel.timer;
    }

    public static void startAnimation() {
        AmusePanel.timer.init();
        new Thread(AmusePanel.timer).start();
    };

    public static void stopAnimation() {
        if (AmusePanel.timer.isInitialized()) {
            AmusePanel.timer.close();
        }
    }

    protected JSlider             timeBar;

    protected JFormattedTextField frameCounter;
    private TweakState            currentConfigState = TweakState.NONE;

    public static Hdf5TimedPlayer timer;

    private final JPanel          configPanel;

    private final JPanel          visualConfig, movieConfig;

    private final AmuseWindow     amuseWindow;

    public AmusePanel(AmuseWindow amuseWindow, String path, String cmdlnfileName) {
        super(amuseWindow);
        this.amuseWindow = amuseWindow;

        // Read command line file information
        if (cmdlnfileName != null) {
            final File cmdlnfile = new File(cmdlnfileName);
            handleFile(cmdlnfile);
        }

        timeBar = new openglCommon.util.CustomJSlider();
        timeBar.setValue(0);
        timeBar.setMajorTickSpacing(5);
        timeBar.setMinorTickSpacing(1);
        timeBar.setMaximum(0);
        timeBar.setMinimum(0);
        timeBar.setPaintTicks(true);
        timeBar.setSnapToTicks(true);

        AmusePanel.timer = new Hdf5TimedPlayer(amuseWindow, timeBar, frameCounter);

        // Make the menu bar
        final JMenuBar menuBar = new JMenuBar();
        final JMenu file = new JMenu("File");
        final JMenuItem open = new JMenuItem("Open");
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                final File file = openFile();
                handleFile(file);
            }
        });
        file.add(open);
        menuBar.add(file);
        final JMenu options = new JMenu("Options");

        final JMenuItem makeMovie = new JMenuItem("Make movie.");
        makeMovie.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setTweakState(TweakState.MOVIE);
            }
        });
        options.add(makeMovie);

        final JMenuItem showTweakPanel = new JMenuItem("Show configuration panel.");
        showTweakPanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setTweakState(TweakState.VISUAL);
            }
        });
        options.add(showTweakPanel);
        menuBar.add(options);

        add(menuBar, BorderLayout.NORTH);

        // Make the "media player" panel
        final JPanel bottomPanel = createBottomPanel();

        // Add the tweaks panels
        configPanel = new JPanel();
        add(configPanel, BorderLayout.WEST);
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setPreferredSize(new Dimension(200, 0));
        configPanel.setVisible(false);

        visualConfig = new JPanel();
        visualConfig.setLayout(new BoxLayout(visualConfig, BoxLayout.Y_AXIS));
        visualConfig.setMinimumSize(configPanel.getPreferredSize());
        createVisualTweakPanel();

        movieConfig = new JPanel();
        movieConfig.setLayout(new BoxLayout(movieConfig, BoxLayout.Y_AXIS));
        movieConfig.setMinimumSize(configPanel.getPreferredSize());
        createMovieTweakPanel();

        add(bottomPanel, BorderLayout.SOUTH);
    }

    void close() {
        amuseWindow.dispose(glCanvas);
    }

    private JPanel createBottomPanel() {
        final JPanel bottomPanel = new JPanel();
        bottomPanel.setFocusCycleRoot(true);
        bottomPanel.setFocusTraversalPolicy(new FocusTraversalPolicy() {
            @Override
            public Component getComponentAfter(Container aContainer, Component aComponent) {
                return null;
            }

            @Override
            public Component getComponentBefore(Container aContainer, Component aComponent) {
                return null;
            }

            @Override
            public Component getDefaultComponent(Container aContainer) {
                return null;
            }

            @Override
            public Component getFirstComponent(Container aContainer) {
                return null;
            }

            // No focus traversal here, as it makes stuff go bad (some things
            // react on focus).
            @Override
            public Component getLastComponent(Container aContainer) {
                return null;
            }
        });

        final JButton oneForwardButton = GoggleSwing.createImageButton("images/media-playback-oneforward.png", "Next",
                null);
        final JButton oneBackButton = GoggleSwing.createImageButton("images/media-playback-onebackward.png",
                "Previous", null);
        final JButton rewindButton = GoggleSwing.createImageButton("images/media-playback-rewind.png", "Rewind", null);
        final JButton screenshotButton = GoggleSwing.createImageButton("images/camera.png", "Screenshot", null);
        final JButton playButton = GoggleSwing.createImageButton("images/media-playback-start.png", "Start", null);
        final ImageIcon playIcon = GoggleSwing.createImageIcon("images/media-playback-start.png", "Start");
        final ImageIcon stopIcon = GoggleSwing.createImageIcon("images/media-playback-stop.png", "Start");

        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        screenshotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // timer.stop();
                final InputHandler inputHandler = InputHandler.getInstance();
                final String fileName = "" + AmusePanel.timer.getFrame() + " {" + inputHandler.getRotation().get(0)
                        + "," + inputHandler.getRotation().get(1) + " - " + Float.toString(inputHandler.getViewDist())
                        + "} ";
                amuseWindow.makeSnapshot(fileName);
            }
        });
        bottomPanel.add(screenshotButton);

        rewindButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AmusePanel.timer.rewind();
                playButton.setIcon(playIcon);
            }
        });
        bottomPanel.add(rewindButton);

        oneBackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AmusePanel.timer.oneBack();
                playButton.setIcon(playIcon);
            }
        });
        bottomPanel.add(oneBackButton);

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (AmusePanel.timer.isPlaying()) {
                    AmusePanel.timer.stop();
                    playButton.setIcon(playIcon);
                } else {
                    AmusePanel.timer.start();
                    playButton.setIcon(stopIcon);
                }
            }
        });
        bottomPanel.add(playButton);

        oneForwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AmusePanel.timer.oneForward();
                playButton.setIcon(playIcon);
            }
        });
        bottomPanel.add(oneForwardButton);

        timeBar.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    AmusePanel.timer.setFrame(timeBar.getValue(), false);
                    playButton.setIcon(playIcon);
                }
            }
        });
        bottomPanel.add(timeBar);

        frameCounter = new JFormattedTextField();
        frameCounter.setValue(new Integer(1));
        frameCounter.setColumns(4);
        frameCounter.setMaximumSize(new Dimension(40, 20));
        frameCounter.setValue(0);
        frameCounter.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                final JFormattedTextField source = (JFormattedTextField) e.getSource();
                if (source.hasFocus()) {
                    if (source == frameCounter) {
                        if (AmusePanel.timer.isInitialized()) {
                            AmusePanel.timer.setFrame(((Number) frameCounter.getValue()).intValue(), false);
                        }
                        playButton.setIcon(playIcon);
                    }
                }
            }
        });

        bottomPanel.add(frameCounter);

        return bottomPanel;
    }

    private void createMovieTweakPanel() {
        final ItemListener listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                setTweakState(TweakState.NONE);
            }
        };
        movieConfig.add(GoggleSwing.titleBox("Movie Creator", listener));

        final ItemListener checkBoxListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setMovieRotate(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        movieConfig.add(GoggleSwing.checkboxBox("", new GoggleSwing.CheckBoxItem("Rotation", settings.getMovieRotate(),
                checkBoxListener)));

        final JLabel rotationSetting = new JLabel("" + settings.getMovieRotationSpeedDef());
        final ChangeListener movieRotationSpeedListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setMovieRotationSpeed(source.getValue() * .25f);
                    rotationSetting.setText("" + settings.getMovieRotationSpeedDef());
                }
            }
        };
        movieConfig.add(GoggleSwing.sliderBox("Rotation Speed", movieRotationSpeedListener,
                (int) (settings.getMovieRotationSpeedMin() * 4f), (int) (settings.getMovieRotationSpeedMax() * 4f), 1,
                (int) (settings.getMovieRotationSpeedDef() * 4f), rotationSetting));

        movieConfig.add(GoggleSwing.buttonBox("", new String[] { "Start Recording" },
                new ActionListener[] { new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AmusePanel.timer.movieMode();
                    }
                } }));
    }

    private void createVisualTweakPanel() {
        final ItemListener listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                setTweakState(TweakState.NONE);
            }
        };
        visualConfig.add(GoggleSwing.titleBox("Visual Configuration", listener));

        final ItemListener cblBeamerMode = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setInvertGasColor(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        final ItemListener cblInvertedBackground = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setGasInvertedBackgroundColor(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        final ItemListener cblGasColorInfluencedByStars = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setStarInfluencedGasColor(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        final ItemListener cblExaggerateStarColors = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setStarColorsExaggerated(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        final ItemListener cblStereo = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setStereo(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        final ItemListener cblStereoSwitch = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settings.setStereoSwitched(e.getStateChange());
                AmusePanel.timer.redraw();
            }
        };
        visualConfig
                .add(GoggleSwing.checkboxBox("",
                        new GoggleSwing.CheckBoxItem("Beamer mode", settings.getGasInvertedColor(), cblBeamerMode),
                        new GoggleSwing.CheckBoxItem("White background", settings.getGasInvertedBackgroundColor(),
                                cblInvertedBackground),
                        new GoggleSwing.CheckBoxItem("Color gas by stars", settings.getGasStarInfluencedColor(),
                                cblGasColorInfluencedByStars), new GoggleSwing.CheckBoxItem("Exaggerate star colors",
                                settings.getStarColorsExaggerated(), cblExaggerateStarColors),
                        new GoggleSwing.CheckBoxItem("Stereo view", settings.getStereo(), cblStereo),
                        new GoggleSwing.CheckBoxItem("Stereo left/right switch", settings.getStereoSwitched(),
                                cblStereoSwitch)));

        final ChangeListener overallBrightnessSliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setPostprocessingOverallBrightness(source.getValue());
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("Overall Brightness", overallBrightnessSliderListener,
                (int) (settings.getPostprocessingOverallBrightnessMin()),
                (int) (settings.getPostprocessingOverallBrightnessMax()), 1,
                (int) (settings.getPostprocessingOverallBrightness()), new JLabel("")));

        visualConfig.add(GoggleSwing.verticalStrut(5));

        final ChangeListener axesBrightnessSliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setPostprocessingAxesBrightness(source.getValue());
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("Axes Brightness", axesBrightnessSliderListener,
                (int) (settings.getPostprocessingAxesBrightnessMin()),
                (int) (settings.getPostprocessingAxesBrightnessMax()), 1,
                (int) (settings.getPostprocessingAxesBrightness()), new JLabel("")));

        visualConfig.add(GoggleSwing.verticalStrut(5));

        final ChangeListener hudBrightnessSliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setPostprocessingHudBrightness(source.getValue());
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("HUD Brightness", hudBrightnessSliderListener,
                (int) (settings.getPostprocessingHudBrightnessMin()),
                (int) (settings.getPostprocessingHudBrightnessMax()), 1,
                (int) (settings.getPostprocessingHudBrightness()), new JLabel("")));

        visualConfig.add(GoggleSwing.verticalStrut(5));

        final ChangeListener gasOpacityFactorSliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setGasOpacityFactor(source.getValue() / 100f);
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("Gas Opacity Factor", gasOpacityFactorSliderListener,
                (int) (settings.getGasOpacityFactorMin() * 100), (int) (settings.getGasOpacityFactorMax() * 100), 1,
                (int) (settings.getGasOpacityFactor()), new JLabel("")));

        visualConfig.add(GoggleSwing.verticalStrut(5));

        final ChangeListener starHaloBrightnessSliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setPostprocessingStarHaloBrightness(source.getValue());
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("Star Halo Brightness", starHaloBrightnessSliderListener,
                (int) (settings.getPostprocessingStarHaloBrightnessMin()),
                (int) (settings.getPostprocessingStarHaloBrightnessMax()), 1,
                (int) (settings.getPostprocessingStarHaloBrightness()), new JLabel("")));

        visualConfig.add(GoggleSwing.verticalStrut(5));

        final ChangeListener starBrightnessSliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setPostprocessingStarBrightness(source.getValue());
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("Star Brightness", starBrightnessSliderListener,
                (int) (settings.getPostprocessingStarBrightnessMin()),
                (int) (settings.getPostprocessingStarBrightnessMax()), 1,
                (int) (settings.getPostprocessingStarBrightness()), new JLabel("")));

        final ChangeListener stereoOcularDistanceListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final JSlider source = (JSlider) e.getSource();
                if (source.hasFocus()) {
                    settings.setStereoOcularDistance(source.getValue() / 10f);
                }
            }
        };
        visualConfig.add(GoggleSwing.sliderBox("Stereo Ocular Distance", stereoOcularDistanceListener,
                (int) (settings.getStereoOcularDistanceMin() * 10), (int) (settings.getStereoOcularDistanceMax() * 10),
                1, (int) (settings.getStereoOcularDistance() * 10), new JLabel("")));

        visualConfig.add(GoggleSwing.radioBox("Level of Detail", new String[] { "Low", "Medium", "High" },
                new ActionListener[] { new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        settings.setLOD(0);
                        AmusePanel.timer.redraw();
                    }
                }, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        settings.setLOD(1);
                        AmusePanel.timer.redraw();
                    }
                }, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        settings.setLOD(2);
                        AmusePanel.timer.redraw();
                    }
                } }));
    }

    protected void handleFile(File file) {
        if (file != null) {
            final String path = file.getPath().substring(0, file.getPath().length() - file.getName().length());

            final String name = file.getName();
            final String fullPath = path + name;
            final String[] ext = fullPath.split("[.]");
            String prefix = null;
            if (!((ext[1].compareTo("evo") == 0) || (ext[1].compareTo("grav") == 0) || (ext[1].compareTo("add") == 0)
                    || (ext[1].compareTo("gas") == 0) || (ext[1].compareTo("data") == 0))) {
                final JOptionPane pane = new JOptionPane();
                pane.setMessage("Tried to open invalid file type.");
                final JDialog dialog = pane.createDialog("Alert");
                dialog.setVisible(true);
            } else {
                prefix = ext[0].substring(0, ext[0].length() - 6);
                AmusePanel.stopAnimation();
                AmusePanel.timer = new Hdf5TimedPlayer(amuseWindow, timeBar, frameCounter);
                AmusePanel.timer.open(path, prefix);
                AmusePanel.startAnimation();

                settings.setScreenshotPath(path);
            }
        }
    }

    private File openFile() {
        final JFileChooser fileChooser = new JFileChooser();

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        final int result = fileChooser.showOpenDialog(this);

        // user clicked Cancel button on dialog
        if (result == JFileChooser.CANCEL_OPTION) {
            return null;
        } else {
            return fileChooser.getSelectedFile();
        }
    }

    // Callback methods for the various ui actions and listeners
    public void setTweakState(TweakState newState) {
        configPanel.setVisible(false);
        configPanel.remove(visualConfig);
        configPanel.remove(movieConfig);

        currentConfigState = newState;

        if (currentConfigState == TweakState.NONE) {
        } else if (currentConfigState == TweakState.VISUAL) {
            configPanel.setVisible(true);
            configPanel.add(visualConfig, BorderLayout.WEST);
        } else if (currentConfigState == TweakState.MOVIE) {
            configPanel.setVisible(true);
            configPanel.add(movieConfig, BorderLayout.WEST);
        }
    }
}
