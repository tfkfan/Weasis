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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.image.op.ByteLut;

import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class LUTCanvas extends Canvas {
    private static final Logger LOGGER = LoggerFactory.getLogger(LUTCanvas.class);

    private ByteLut byteLut;
    private double height;
    private double width;
    private MainCanvas canvas;
    private MenuController menuController;
    private Boolean selected = false;
    private Boolean currentLUT = false;

    public LUTCanvas(ByteLut byteLut, MainCanvas canvas, MenuController menuController, double width, double height) {
        this.byteLut = byteLut;
        this.height = height;
        this.width = width;
        this.canvas = canvas;
        this.menuController = menuController;
        this.setId(byteLut.getName());

        this.setOnTouchMoved(this::handleOnTouchOver);
        this.setOnTouchStationary(this::handleOnTouchOver);
        this.setOnTouchReleased(this::handleOnTouchReleased);

        this.setOnMouseEntered(this::handleOnMouseEntred);
        this.setOnMouseExited(this::handleOnMouseExited);
        this.setOnMouseClicked(this::handleOnMouseClicked);

        draw();
    }

    public void draw() {
        this.setWidth(width);
        this.setHeight(height);
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        try {
            Image lut = new Image(getClass().getResourceAsStream("lut/" + byteLut.getName() + ".jpg"));
            gc.drawImage(lut, 0, 0, width, height);
        } catch (Exception e) {
            for (int i = 0; i < byteLut.getLutTable()[0].length; i = i + 2) {
                int b = byteLut.getLutTable()[0][i] & 0xFF;
                int g = byteLut.getLutTable()[1][i] & 0xFF;
                int r = byteLut.getLutTable()[2][i] & 0xFF;

                gc.setStroke(Color.rgb(r, g, b));
                gc.strokeLine(i / 2.0, 0, i / 2.0, height);
            }
        }

        gc.setStroke(Color.BLACK);
        gc.setFill(Color.WHITE);

        gc.setFont(new Font(14));
        gc.setTextBaseline(VPos.CENTER);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.strokeText(byteLut.getName(), width / 2, height / 2);
        gc.fillText(byteLut.getName(), width / 2, height / 2);

        if (currentLUT) {
            gc.setStroke(Color.rgb(0xb3, 0x47, 0x11));
            gc.setLineWidth(5);
            gc.strokeRect(0, 0, width, height);
            gc.setLineWidth(1.0);
        }
    }

    private void handleOnTouchOver(TouchEvent event) {
        Node parent = this.getParent();
        GridPane menu = (GridPane) parent;

        for (Node iterable_element : menu.getChildren()) {
            if (iterable_element.getClass() == LUTCanvas.class) {
                LUTCanvas lc = (LUTCanvas) iterable_element;
                if (lc.selected) {
                    lc.unselect();
                }
            }
        }
        this.setOpacity(0.1);
        selected = true;
        canvas.setTempLut(this.byteLut);

        event.getTouchPoint().ungrab();
        event.consume();
    }

    private void handleOnTouchReleased(TouchEvent event) {
        if (event.getTouchCount() == 1) {
            LOGGER.trace("{}", event.getSource());
            canvas.setLut(this.byteLut);
            menuController.close();
        }
        Node parent = this.getParent();
        GridPane menu = (GridPane) parent;
        for (Node iterable_element : menu.getChildren()) {
            if (iterable_element.getClass() == LUTCanvas.class) {
                LUTCanvas lc = (LUTCanvas) iterable_element;
                if (lc.selected) {
                    lc.unselect();
                }
            }
        }
        event.consume();
    }

    private void handleOnMouseEntred(MouseEvent event) {
        if (!event.isSynthesized()) {
            this.setOpacity(0.1);
            selected = true;
            canvas.setTempLut(this.byteLut);

            event.consume();
        }
    }

    private void handleOnMouseExited(MouseEvent event) {
        if (!event.isSynthesized()) {
            this.unselect();
            ByteLut lut = (ByteLut) canvas.getActionValue(ActionW.LUT.cmd());
            if (lut != null) {
                canvas.setLut(lut);
            }
        }
        event.consume();
    }

    private void handleOnMouseClicked(MouseEvent event) {
        if (!event.isSynthesized()) {
            LOGGER.trace("{}", event.getSource());
            canvas.setLut(this.byteLut);
            menuController.close();
        }
        Node parent = this.getParent();
        GridPane menu = (GridPane) parent;
        for (Node element : menu.getChildren()) {
            if (element.getClass() == LUTCanvas.class) {
                LUTCanvas lc = (LUTCanvas) element;
                if (lc.selected) {
                    lc.unselect();
                }
            }
        }
        event.consume();
    }

    public void unselect() {
        if (selected) {
            this.setOpacity(1);
            selected = false;
        }
    }

    public void setCurentLUT(Boolean currentLUT) {
        if (currentLUT != this.currentLUT) {
            this.currentLUT = currentLUT;
            draw();
        }
    }

    public String getName() {
        return byteLut.getName();
    }
}
