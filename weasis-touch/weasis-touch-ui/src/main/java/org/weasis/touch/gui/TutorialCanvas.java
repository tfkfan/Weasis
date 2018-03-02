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

import java.io.IOException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.touch.Messages;
import org.weasis.touch.WeasisPreferences;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;

public class TutorialCanvas extends AnchorPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(TutorialCanvas.class);
    private Preferences prefs;
    private Canvas canvas;
    private GridPane gestureTutorial;

    public TutorialCanvas() {
        prefs = Preferences.userRoot().node(SettingsController.class.getName());

        canvas = new Canvas();

        this.getChildren().add(canvas);

        this.setMinHeight(0);
        this.setMinWidth(0);

        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        AnchorPane.setTopAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);

        canvas.widthProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
            if (prefs.getBoolean(WeasisPreferences.SHOW_TUTO.name(),
                (Boolean) WeasisPreferences.SHOW_TUTO.defaultValue())) {
                if ((Double) newValue >= 488) {
                    drawTutorial();
                } else {
                    clear();
                }
            }
        });

        canvas.heightProperty().addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
            if (prefs.getBoolean(WeasisPreferences.SHOW_TUTO.name(),
                (Boolean) WeasisPreferences.SHOW_TUTO.defaultValue())) {
                if ((Double) newValue >= 422) {
                    drawTutorial();
                } else {
                    clear();
                }
            }
        });

        try {
            FXMLLoader loaderGestureTutorial = new FXMLLoader();
            loaderGestureTutorial.setResources(Messages.RESOURCE_BUNDLE);
            loaderGestureTutorial.setLocation(this.getClass().getResource("GestureTutorial.fxml"));
            loaderGestureTutorial.setClassLoader(this.getClass().getClassLoader());
            gestureTutorial = loaderGestureTutorial.load();

            this.getChildren().add(gestureTutorial);

            AnchorPane.setTopAnchor(gestureTutorial, 40.0);
            AnchorPane.setLeftAnchor(gestureTutorial, 400.0);
        } catch (IOException e) {
            LOGGER.error("Load tutorial", e);
        }

        drawTutorial();
    }

    public void clear() {
        double cW = canvas.getWidth();
        double cH = canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cW, cH);

        AnchorPane.setTopAnchor(gestureTutorial, cH / 2 - gestureTutorial.getHeight() / 2);
        AnchorPane.setLeftAnchor(gestureTutorial, cW / 2 - gestureTutorial.getWidth() / 2);
    }

    public void drawTutorial() {
        double cW = canvas.getWidth();
        double cH = canvas.getHeight();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cW, cH);
        gc.setStroke(Color.WHITE);
        gc.setFill(Color.WHITE);

        Image arrow = new Image(getClass().getResourceAsStream("arrow.png"));

        drawRotatedImage(gc, arrow, 180, -60, 40);
        drawRotatedImage(gc, arrow, 10, cW - 360, cH - 200);
        drawRotatedImage(gc, arrow, 20, -100, cH / 2);
        drawRotatedImage(gc, arrow, 20, cW - 340, cH - 400);
        gc.strokeRect(cW - 100, 100, 100, cH - 260);

        gc.setFont(new Font(24));
        gc.fillText(Messages.getString("WeasisTouchTutorial.menu"), cW - 400, cH - 70);
        gc.fillText(Messages.getString("WeasisTouchTutorial.hideShowThumbnail"), 70, 50);
        gc.fillText(Messages.getString("WeasisTouchTutorial.dragAndDrop"), 70, cH / 2);
        gc.fillText(Messages.getString("WeasisTouchTutorial.scrollBar"), cW - 350, cH - 300);

        AnchorPane.setTopAnchor(gestureTutorial, 40.0);
        AnchorPane.setLeftAnchor(gestureTutorial, 400.0);
    }

    private void rotate(GraphicsContext gc, double angle, double px, double py) {
        Rotate r = new Rotate(angle, px, py);
        gc.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());
    }

    private void drawRotatedImage(GraphicsContext gc, Image image, double angle, double tlpx, double tlpy) {
        gc.save(); // saves the current state on stack, including the current transform
        rotate(gc, angle, tlpx + image.getWidth() / 2, tlpy + image.getHeight() / 2);
        gc.drawImage(image, tlpx, tlpy);
        gc.restore(); // back to original state (before rotation)
    }

    public void setParam(MenuController menu) {
        this.setOnTouchReleased(e -> menu.close());
        this.setOnMouseClicked(e -> menu.close());

    }
}