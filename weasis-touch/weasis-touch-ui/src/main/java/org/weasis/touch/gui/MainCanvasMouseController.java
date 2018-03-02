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

import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.touch.WeasisPreferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class MainCanvasMouseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainCanvasTouchController.class);

    private MainCanvas canvas;

    public BooleanProperty lockedProperty = new SimpleBooleanProperty(false);
    public IntegerProperty measureInProgress = new SimpleIntegerProperty(0);
    private MenuController menu;
    private Preferences prefs;

    private Double windowSensitivity = 1.0;
    private Double levelSensitivity = 1.0;
    private int windowOrientation = 1;
    private int levelOrientation = 1;
    private Double posX, posY;
    private Boolean flagTranslate = false;

    public MainCanvasMouseController(MainCanvas canvas) {
        this.canvas = canvas;

        canvas.setOnMouseClicked(this::handleOnMouseClicked);
        canvas.setOnMouseDragged(this::handleOnMouseDragged);
        canvas.setOnMousePressed(this::handleOnMousePressed);
        canvas.setOnMouseReleased(this::handleOnMouseReleased);
        canvas.setOnMouseMoved(this::handleDrawMeasure);
        canvas.setOnMouseExited(this::handleDrawMeasureMouseExit);

        prefs = Preferences.userRoot().node(SettingsController.class.getName());
    }

    public void setMenu(MenuController menu) {
        this.menu = menu;
    }

    private void handleOnMouseClicked(MouseEvent event) {
        if (!event.isSynthesized()) {
            if (lockedProperty.getValue()) {
                menu.close();
            } else {
                if (event.getButton().compareTo(MouseButton.SECONDARY) == 0) {
                    if (!flagTranslate) {
                        canvas.setInfoMode(((Integer) canvas.getActionValue(MainCanvas.INFO_MODE) + 1) % 3);
                    }
                    flagTranslate = false;
                }
                if (measureInProgress.get() != 0) {
                    if (event.getButton().compareTo(MouseButton.PRIMARY) == 0) {
                        Point2D point = canvas.canvasPointToImagePoint(event.getX(), event.getY());
                        if (canvas.mesures.getLastMeasure().addPoint(point) == 0) {
                            measureInProgress.set(0);
                        }
                        canvas.draw();
                    }
                }
            }
        }
        event.consume();
    }

    private void handleOnMousePressed(MouseEvent event) {
        if (!event.isSynthesized()) {
            if (!lockedProperty.getValue()) {
                if (measureInProgress.get() == 0) {
                    windowSensitivity = 0.02 * prefs.getInt(WeasisPreferences.WINDOW_SENSITIVITY.name(),
                        (int) WeasisPreferences.WINDOW_SENSITIVITY.defaultValue());
                    levelSensitivity = 0.02 * prefs.getInt(WeasisPreferences.LEVEL_SENSITIVITY.name(),
                        (int) WeasisPreferences.LEVEL_SENSITIVITY.defaultValue());
                    windowOrientation = prefs.getBoolean(WeasisPreferences.WINDOW_ORIENTATION.name(),
                        (Boolean) WeasisPreferences.WINDOW_ORIENTATION.defaultValue()) ? -1 : 1;
                    levelOrientation = prefs.getBoolean(WeasisPreferences.LEVEL_ORIENTATION.name(),
                        (Boolean) WeasisPreferences.LEVEL_ORIENTATION.defaultValue()) ? -1 : 1;
                }
                posX = event.getX();
                posY = event.getY();

                if (canvas.editModeProperty.get()) {
                    canvas.mesures.isMeasure(event.getX(), event.getY());
                }
            }
        }
        event.consume();
    }

    private void handleOnMouseReleased(MouseEvent event) {
        if (canvas.editModeProperty.get()) {
            menu.menuToolsControlleur.removeMeasure(event.getPickResult());
            canvas.mesures.resetMeasureToModify();
            canvas.draw();
        }
    }

    private void handleOnMouseDragged(MouseEvent event) {
        if (!event.isSynthesized() && !lockedProperty.getValue()) {
            double multiplicateur = 1;
            if (event.isControlDown()) {
                multiplicateur = 2;
                if (event.isShiftDown()) {
                    multiplicateur = 4;
                }
            }

            if (event.getButton().compareTo(MouseButton.PRIMARY) == 0 && measureInProgress.get() == 0) {
                if (canvas.editModeProperty.get()) {
                    canvas.mesures.editMeasure(event.getX(), event.getY());
                    if (canvas.mesures.editMeasure(event.getX(), event.getY())) {
                        canvas.draw();
                        menu.menuToolsControlleur.trashOver(event.getPickResult());
                    }
                } else {
                    canvas.contraste(multiplicateur * windowOrientation * windowSensitivity * (event.getX() - posX),
                        multiplicateur * -levelOrientation * levelSensitivity * (event.getY() - posY));
                }
            } else if (event.getButton().compareTo(MouseButton.SECONDARY) == 0) {
                canvas.translate(event.getX() - posX, event.getY() - posY);
                flagTranslate = true;
            }
            posX = event.getX();
            posY = event.getY();
        }
        event.consume();
    }

    private void handleDrawMeasure(MouseEvent event) {
        this.canvas.requestFocus();
        if (!event.isSynthesized() && !lockedProperty.getValue() && measureInProgress.get() != 0) {
            canvas.mesures.getLastMeasure().temporaryPoint(new Point2D(event.getX(), event.getY()));
            canvas.draw();
        }
        event.consume();
    }

    private void handleDrawMeasureMouseExit(MouseEvent event) {
        if (!event.isSynthesized() && !lockedProperty.getValue() && measureInProgress.get() != 0) {
            canvas.mesures.getLastMeasure().temporaryPoint(null);
            canvas.draw();
        }
        event.consume();
    }
}