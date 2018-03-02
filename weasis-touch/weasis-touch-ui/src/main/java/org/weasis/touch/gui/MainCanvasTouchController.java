/*******************************************************************************
 * Copyright (c) 2009-2018 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Gérôme Pasquier - initial API and implementation
 *     Nicolas Roduit - implementation
 *******************************************************************************/
package org.weasis.touch.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.dicom.codec.display.PresetWindowLevel;
import org.weasis.touch.WeasisPreferences;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.util.Duration;

public class MainCanvasTouchController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainCanvasTouchController.class);

    private MainCanvas canvas;

    public BooleanProperty lockedProperty = new SimpleBooleanProperty(false);
    public IntegerProperty measureInProgress = new SimpleIntegerProperty(0);
    private MenuController menu;
    private Preferences prefs;

    public MainCanvasTouchController(MainCanvas canvas) {
        this.canvas = canvas;

        this.canvas.setOnScroll(this::handleOnScroll);
        this.canvas.setOnScrollStarted(this::handleOnScrollStart);
        this.canvas.setOnScrollFinished(this::handleOnScrollEnd);

        this.canvas.setOnZoom(this::handleOnZoom);
        this.canvas.setOnZoomStarted(this::handleOnZoomStarted);
        this.canvas.setOnZoomFinished(this::handleOnZoomFinished);

        this.canvas.setOnTouchPressed(this::handleTouchPress);
        this.canvas.setOnTouchReleased(this::handleTouchRelease);

        this.canvas.setOnSwipeRight(this::handleSwipeRight);

        pause.setOnFinished(this::pause);

        prefs = Preferences.userRoot().node(SettingsController.class.getName());
    }

    public void setMenu(MenuController menu) {
        this.menu = menu;
    }

    /*****************************************************************
     * SCROLL *
     *****************************************************************/
    private Integer mode = 0;
    private Integer lastOperationMode = 0;

    private Double windowTemp, levelTemp;
    private PresetWindowLevel presetWL;

    private void handleOnScroll(ScrollEvent event) {
        if (!lockedProperty.getValue()) {
            if (event.isDirect()) {
                switch (event.getTouchCount()) {
                    case 2:
                        mode = 2;
                        lastOperationMode = 2;
                        canvas.translate(event.getDeltaX(), event.getDeltaY());
                        if (measureInProgress.get() != 0) {
                            canvas.mesures.getLastMeasure().temporaryPoint(null);
                        }
                        break;
                    case 1:

                        if (mode == 0 || mode == 1) {
                            mode = 1;
                            if (measureInProgress.get() == 0) {
                                if (canvas.editModeProperty.get()) {
                                    if (canvas.mesures.editMeasure(event.getX(), event.getY() - 100)) {
                                        canvas.draw();
                                        menu.menuToolsControlleur.trashOver(event.getPickResult());
                                    }
                                } else {
                                    lastOperationMode = 1;
                                    canvas.contraste(event.getDeltaX() * windowSensitivity * windowOrientation,
                                        -event.getDeltaY() * levelSensitivity * levelOrientation);
                                }
                            } else {
                                canvas.mesures.getLastMeasure()
                                    .temporaryPoint(new Point2D(event.getX(), event.getY() - 100));
                                canvas.drawCursor(event.getX(), event.getY() - 100);
                            }
                        }
                        break;
                }
            } else {
                // zoom with mouse scroll
                double multiplicateur = 0;
                if (event.isControlDown()) {
                    multiplicateur += 0.3;
                }

                if (event.getDeltaY() < 0) {
                    canvas.zoom(0.9 - multiplicateur, event.getX(), event.getY());
                } else {
                    canvas.zoom(1.1 + multiplicateur, event.getX(), event.getY());
                }
                canvas.zoomEnd();
            }
        }
        event.consume();
    }

    private Double windowSensitivity = 1.0;
    private Double levelSensitivity = 1.0;
    private int windowOrientation = 1;
    private int levelOrientation = 1;

    private void handleOnScrollStart(ScrollEvent event) {

        windowSensitivity = 0.02 * prefs.getInt(WeasisPreferences.WINDOW_SENSITIVITY.name(),
            (int) WeasisPreferences.WINDOW_SENSITIVITY.defaultValue());
        levelSensitivity = 0.02 * prefs.getInt(WeasisPreferences.LEVEL_SENSITIVITY.name(),
            (int) WeasisPreferences.LEVEL_SENSITIVITY.defaultValue());
        windowOrientation = prefs.getBoolean(WeasisPreferences.WINDOW_ORIENTATION.name(),
            (Boolean) WeasisPreferences.WINDOW_ORIENTATION.defaultValue()) ? -1 : 1;
        levelOrientation = prefs.getBoolean(WeasisPreferences.LEVEL_ORIENTATION.name(),
            (Boolean) WeasisPreferences.LEVEL_ORIENTATION.defaultValue()) ? -1 : 1;

        windowTemp = (Double) canvas.getActionValue(ActionW.WINDOW.cmd());
        levelTemp = (Double) canvas.getActionValue(ActionW.LEVEL.cmd());
        presetWL = (PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd());
        event.consume();
    }

    private void handleOnScrollEnd(ScrollEvent event) {
        if (flagSwipe) {
            flagSwipe = false;
            if (lastOperationMode == 1) {
                if (presetWL != null) {
                    canvas.setPresetWindowLevel(presetWL.getName());
                } else {
                    canvas.setWindowLevel(windowTemp, levelTemp);
                }
            }
        }
        event.consume();
    }

    /*****************************************************************
     * ZOOM *
     *****************************************************************/
    private void handleOnZoom(ZoomEvent event) {
        if (!lockedProperty.getValue()) {
            if (mode == 2) {
                canvas.zoom(event.getZoomFactor(), event.getX(), event.getY());
            }
        }
        event.consume();
    }

    private void handleOnZoomStarted(ZoomEvent event) {
        event.consume();
    }

    private void handleOnZoomFinished(ZoomEvent event) {
        if (!lockedProperty.getValue()) {
            canvas.zoomEnd();
        }
        event.consume();
    }

    /*****************************************************************
     * SWIPE *
     *****************************************************************/
    private Boolean flagSwipe = false;
    final LongProperty dragExitTime = new SimpleLongProperty(0);

    public void handleSwipeRight(SwipeEvent event) {
        if (!lockedProperty.getValue() && measureInProgress.get() == 0) {
            if (event.getTouchCount() == 1) {
                flagSwipe = true;
                if (dragExitTime.get() == 0 || System.currentTimeMillis() - dragExitTime.get() > 500) {
                    canvas.setInfoMode(((Integer) canvas.getActionValue(MainCanvas.INFO_MODE) + 1) % 3);
                }
                dragExitTime.set(0);
            }
        }
        event.consume();
    }

    /*****************************************************************
     * TOUCH *
     *****************************************************************/
    private static ArrayList<String> doubleTab2Fingers =
        new ArrayList<>(Arrays.asList("press", "press", "release", "release", "press", "press", "release", "release"));
    private static ArrayList<String> doubleTab3Fingers = new ArrayList<>(Arrays.asList("press", "press", "press",
        "release", "release", "release", "press", "press", "press", "release", "release", "release"));
    private static ArrayList<String> doubleTab1Finger =
        new ArrayList<>(Arrays.asList("press", "release", "press", "release"));

    private ArrayList<String> touchevent = new ArrayList<>();
    private double x = 0, y = 0;

    private PauseTransition pause = new PauseTransition(Duration.millis(130));

    private void handleTouchPress(TouchEvent event) {

        if (!lockedProperty.getValue()) {

            if (event.getTouchCount() > 2) {
                mode = 3;
            }

            pause.stop();
            touchevent.add("press");
            pause.playFromStart();

            if (canvas.editModeProperty.get()) {
                canvas.mesures.isMeasure(event.getTouchPoint().getX(), event.getTouchPoint().getY());
            }
        }
        event.consume();
    }

    private void handleTouchRelease(TouchEvent event) {
        if (!lockedProperty.getValue()) {

            if ((mode == 1 || mode == 0) && measureInProgress.get() != 0) {
                Point2D point =
                    canvas.canvasPointToImagePoint(event.getTouchPoint().getX(), event.getTouchPoint().getY() - 100);

                if (!menu.menuToolsControlleur.cancelMeasure(event.getTouchPoint().getPickResult())) {
                    if (canvas.mesures.getLastMeasure().addPoint(point) == 0) {
                        measureInProgress.set(0);
                    }
                    canvas.draw();
                }
            }

            if (canvas.editModeProperty.get()) {
                menu.menuToolsControlleur.removeMeasure(event.getTouchPoint().getPickResult());
                canvas.mesures.resetMeasureToModify();
                canvas.draw();
            }

            if (event.getTouchCount() == 1) {
                mode = 0;
            }

            x += event.getTouchPoint().getX();
            y += event.getTouchPoint().getY();

            pause.stop();
            touchevent.add("release");
            pause.playFromStart();
        } else {
            menu.close();
        }
        event.consume();
    }

    private void pause(ActionEvent e) {
        x = x / (touchevent.size() / 2.0);
        y = y / (touchevent.size() / 2.0);
        if (touchevent.equals(doubleTab2Fingers)) {
            canvas.zoom(0.5, x, y);
            canvas.zoomEnd();
        } else if (touchevent.equals(doubleTab1Finger)) {
            canvas.zoom(2, x, y);
            canvas.zoomEnd();
        } else if (touchevent.equals(doubleTab3Fingers)) {
            canvas.reset();
        }

        x = y = 0;
        touchevent.clear();
    }
}
