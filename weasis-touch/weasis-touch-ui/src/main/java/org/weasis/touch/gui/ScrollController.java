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

import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.touch.WeasisPreferences;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ScrollController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollController.class);
    private Preferences prefs;
    private PreferenceChangeListener changeListener;

    @FXML
    Group scrollbarGroup;
    @FXML
    private ScrollBar scrollBar;
    @FXML
    private Rectangle zoneScrollVirtual;

    private FadeTransition ftHide;
    private PauseTransition pause = new PauseTransition(Duration.seconds(1));

    public final BooleanProperty lockedProperty = new SimpleBooleanProperty(false);
    public final BooleanProperty blurProperty = new SimpleBooleanProperty(false);
    public final IntegerProperty measureInProgress = new SimpleIntegerProperty(0);

    private MainCanvasScrollController mainCanvasScrollController;
    private MainCanvas canvas;

    @FXML
    private void initialize() {
        scrollBar.prefHeightProperty().bind(zoneScrollVirtual.heightProperty());
        scrollBar.layoutYProperty().bind(zoneScrollVirtual.layoutYProperty());

        zoneScrollVirtual.setOnScroll(this::scroll);
        zoneScrollVirtual.setOnScrollStarted(this::scrollStart);
        zoneScrollVirtual.setOnScrollFinished(this::scrollFinish);
        zoneScrollVirtual.setOnTouchPressed(this::touchPress);
        zoneScrollVirtual.setOnTouchReleased(this::touchRelease);

        zoneScrollVirtual.setOnMouseDragged(this::scrollMouseMoved);

        scrollBar.valueProperty().addListener((ChangeListener<Number>) (ov, oldVal, newVal) -> {
            if (oldVal.intValue() != newVal.intValue()) {
                mainCanvasScrollController.setScroll(newVal.intValue());
            }
            ftHide.stop();
        });

        ftHide = new FadeTransition(Duration.millis(1000), scrollBar);
        ftHide.setFromValue(0.6);
        ftHide.setToValue(0);

        pause.setOnFinished(e -> ftHide.playFromStart());

        ftHide.playFrom(Duration.millis(1000));

        prefs = Preferences.userRoot().node(SettingsController.class.getName());

        changeListener = evt -> {
            if (evt.getKey().equals(WeasisPreferences.HIDE_SHOW_SCROLL.name())) {
                Boolean newVal = Boolean.valueOf(evt.getNewValue());
                if (newVal) {
                    ftHide.setToValue(0.2);
                    scrollBar.setOpacity(0.2);
                } else {
                    ftHide.setToValue(0);
                    scrollBar.setOpacity(0);
                }
            }
        };
        prefs.addPreferenceChangeListener(changeListener);
        blurProperty.addListener((ChangeListener<Boolean>) (o, oldVal, newVal) -> {
            if (newVal) {
                scrollbarGroup.setEffect(new GaussianBlur());
            } else {
                scrollbarGroup.setEffect(null);
            }
        });
    }

    public void setParam(MainCanvas mainCanvas) {

        this.canvas = mainCanvas;
        this.mainCanvasScrollController = mainCanvas.canvasScrollController;

        scrollBar.layoutXProperty().bind(mainCanvas.widthProperty().subtract(52));
        zoneScrollVirtual.setLayoutY(100);
        zoneScrollVirtual.heightProperty()
            .bind(mainCanvas.heightProperty().subtract(160 + zoneScrollVirtual.getLayoutY()));

        zoneScrollVirtual.layoutXProperty().bind(mainCanvas.widthProperty().subtract(zoneScrollVirtual.getWidth()));
    }

    public void setValue(int val) {
        if (prefs.getBoolean(WeasisPreferences.HIDE_SHOW_SCROLL.name(),
            (Boolean) WeasisPreferences.HIDE_SHOW_SCROLL.defaultValue())) {
            ftHide.setToValue(0.2);
            scrollBar.setOpacity(0.2);
        } else {
            ftHide.setToValue(0);
            scrollBar.setOpacity(0);
        }
        scrollBar.setValue(val);
    }

    public void setMaxValue(int val) {
        scrollBar.setMax(val);
    }

    /*****************************************************************
     * SCROLL *
     *****************************************************************/
    void scrollMouseMoved(MouseEvent event) {
        if (!lockedProperty.getValue() && !canvas.isImageNull() && measureInProgress.get() == 0) {
            if (!event.isSynthesized()) {
                pause.stop();
                scrollBar.setOpacity(0.6);
                Double val = event.getY() * scrollBar.getMax() / scrollBar.getHeight();

                if (val > scrollBar.getMax()) {
                    val = scrollBar.getMax();
                } else if (val < scrollBar.getMin()) {
                    val = scrollBar.getMin();
                }

                scrollBar.setValue(val);
                pause.playFromStart();
            }
        }
        event.consume();
    }

    private void scrollStart(ScrollEvent event) {
        event.consume();
    }

    private void scroll(ScrollEvent event) {
        if (!lockedProperty.getValue() && !canvas.isImageNull() && measureInProgress.get() == 0) {
            if (!event.isInertia()) {

                Double val;
                scrollBar.setOpacity(0.6);

                if (event.isDirect()) {
                    val = scrollBar.getValue() + ((event.getDeltaY() * scrollBar.getMax()) / scrollBar.getHeight());
                } else {
                    val = scrollBar.getValue() + (event.isControlDown() ? 10 : 1) * (event.getDeltaY() < 0 ? 1 : -1);
                }
                val = val > scrollBar.getMax() ? scrollBar.getMax()
                    : (val < scrollBar.getMin() ? scrollBar.getMin() : val);

                scrollBar.setValue(val);
                pause.playFromStart();
            }
        }
        event.consume();
    }

    private void scrollFinish(ScrollEvent event) {
        if (!lockedProperty.getValue() && !canvas.isImageNull() && measureInProgress.get() == 0) {
            pause.playFromStart();
        }
        event.consume();
    }

    /*****************************************************************
     * TOUCH *
     *****************************************************************/
    private double startTime;
    private double timeFirstTouch;
    private Integer touchID = 0;

    private void touchPress(TouchEvent event) {
        if (!lockedProperty.getValue() && event.getTouchCount() == 1 && !canvas.isImageNull()
            && measureInProgress.get() == 0) {
            scrollBar.setOpacity(0.6);
            touchID = event.getTouchPoint().getId();
            startTime = System.currentTimeMillis();
        }
        event.consume();
    }

    private void touchRelease(TouchEvent event) {
        if (!lockedProperty.getValue() && touchID == event.getTouchPoint().getId() && measureInProgress.get() == 0) {
            pause.playFromStart();
            if (System.currentTimeMillis() - startTime < 300) {
                if (System.currentTimeMillis() - timeFirstTouch < 300) {

                    scrollBar
                        .setValue((event.getTouchPoint().getY() * scrollBar.getMax()) / zoneScrollVirtual.getHeight());
                } else {
                    Double val;
                    timeFirstTouch = System.currentTimeMillis();

                    if (event.getTouchPoint().getY() < zoneScrollVirtual.getHeight() / 2) {
                        val = scrollBar.getValue() - 1;
                    } else {
                        val = scrollBar.getValue() + 1;
                    }
                    val = val > scrollBar.getMax() ? scrollBar.getMax()
                        : (val < scrollBar.getMin() ? scrollBar.getMin() : val);

                    scrollBar.setValue(val);
                }
            }
        }
        event.consume();
    }
}
