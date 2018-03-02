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

import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.weasis.core.api.image.util.Unit;
import org.weasis.touch.Messages;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public class LineMesure implements Mesure {

    private Color color;
    private ArrayList<Point2D> point2DList;
    private Integer scroll;
    private String value;
    public static final Integer NB_POINTS = 2;
    private Point2D temporaryPoint;
    private MainCanvas mainCanvas;
    private Integer pointToModify = null;
    private Point2D oldOrigine = null;
    private Double pixelSize;
    private Unit unit;
    public Boolean isEdited = false;

    BooleanProperty editModeProperty = new SimpleBooleanProperty(false);

    public LineMesure(Integer scroll, Color color, Double pixelSize, Unit unit, MainCanvas mainCanvas) {
        point2DList = new ArrayList<>();
        this.scroll = scroll;
        this.color = color;
        this.pixelSize = pixelSize;
        this.unit = unit;
        this.mainCanvas = mainCanvas;
    }

    @Override
    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
        mainCanvas.draw();
    }

    @Override
    public Integer addPoint(Point2D point) {
        point2DList.add(point);
        if (isComplet()) {
            this.computeText();
        }
        return NB_POINTS - point2DList.size();
    }

    @Override
    public Boolean isComplet() {
        return point2DList.size() == NB_POINTS;
    }

    @Override
    public List<Point2D> getPoints() {
        return point2DList;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public int getScroll() {
        return scroll;
    }

    @Override
    public void temporaryPoint(Point2D point) {
        this.temporaryPoint = point;
        if (point2DList.size() + 1 == NB_POINTS) {
            this.computeText();
        }
    }

    private void computeText() {
        DecimalFormat f = new DecimalFormat("##.##");
        value = Messages.getString("WeasisTouchMeasure.length");
        if (isComplet()) {
            value +=
                f.format(point2DList.get(0).distance(point2DList.get(1)) * pixelSize) + " " + unit.getAbbreviation();
        } else if (temporaryPoint != null) {
            value +=
                f.format(point2DList.get(0).distance(mainCanvas.canvasPointToImagePoint(temporaryPoint)) * pixelSize)
                    + " " + unit.getAbbreviation();
        } else {
            value = "";
        }
    }

    @Override
    public void setEditModeProperty(BooleanProperty editModeProperty) {
        this.editModeProperty.bindBidirectional(editModeProperty);
    }

    @Override
    public Boolean isPoint(double x, double y) {
        Point2D p = new Point2D(x, y);
        pointToModify = null;
        for (Point2D point2d : point2DList) {
            Point2D pMeasure = mainCanvas.imagePointToCanvasPoints(point2d);
            if (pMeasure.distance(p) <= Mesure.FINGER_TARGET / 2) {
                pointToModify = point2DList.indexOf(point2d);
                oldOrigine = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean isLine(double x, double y) {
        oldOrigine = null;
        Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
        Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(1));
        if (Line2D.ptSegDistSq(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y) < Math.pow(Mesure.FINGER_TARGET / 2.0, 2)) {
            oldOrigine = new Point2D(x, y);
            pointToModify = null;
            return true;
        }
        return false;
    }

    @Override
    public void moveMeasure(double x, double y) {
        if (pointToModify != null) {
            point2DList.set(pointToModify, mainCanvas.canvasPointToImagePoint(x, y));
            computeText();
        } else if (oldOrigine != null) {
            Point2D delta = new Point2D(x - oldOrigine.getX(), y - oldOrigine.getY());
            oldOrigine = new Point2D(x, y);

            for (int i = 0; i < point2DList.size(); i++) {
                Point2D p = mainCanvas.imagePointToCanvasPoints(point2DList.get(i));
                p = p.add(delta.getX(), delta.getY());
                p = mainCanvas.canvasPointToImagePoint(p);
                point2DList.set(i, p);
            }
        }
    }

    @Override
    public void clearMeasureToModify() {
        pointToModify = null;
        oldOrigine = null;
    }

    @Override
    public void draw() {
        GraphicsContext gc = mainCanvas.getGraphicsContext2D();

        if (editModeProperty.get() && isComplet()) {
            this.drawEditMode();
        }

        gc.setStroke(color);
        gc.setLineWidth(1);
        gc.setLineCap(StrokeLineCap.SQUARE);
        if (isComplet()) {
            Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
            Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(1));
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        } else {
            for (Point2D point2d : point2DList) {
                Point2D p = mainCanvas.imagePointToCanvasPoints(point2d);
                gc.strokeRect(p.getX() - 2.5, p.getY() - 2.5, 5, 5);
            }

            if (point2DList.size() == NB_POINTS - 1 && temporaryPoint != null) {
                Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
                gc.strokeLine(p1.getX(), p1.getY(), temporaryPoint.getX(), temporaryPoint.getY());
            }
        }
        drawText();
    }

    private void drawText() {
        GraphicsContext gc = mainCanvas.getGraphicsContext2D();
        gc.setFill(color);
        if (isComplet()) {
            Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
            Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(1));
            if (p1.getX() > p2.getX()) {
                gc.fillText(value, p1.getX() + 10, p1.getY());
            } else {
                gc.fillText(value, p2.getX() + 10, p2.getY());
            }
        } else if (point2DList.size() == NB_POINTS - 1 && temporaryPoint != null) {
            Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
            // Point2D p2 = mainCanvas.imagePointToCanvasPoints(temporaryPoint);
            if (p1.getX() > temporaryPoint.getX()) {
                gc.fillText(value, p1.getX() + 10, p1.getY());
            } else {
                gc.fillText(value, temporaryPoint.getX() + 10, temporaryPoint.getY());
            }
        }
    }

    private void drawEditMode() {
        if (!isEdited) {
            GraphicsContext gc = mainCanvas.getGraphicsContext2D();
            Color c = Color.rgb((int) (color.getRed() * 255), (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255), 0.3);

            Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
            Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(1));

            gc.setStroke(c.darker().darker().darker());
            gc.setLineWidth(Mesure.FINGER_TARGET);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());

            gc.setFill(c.brighter().brighter().brighter());
            for (Point2D point2d : point2DList) {
                Point2D p = mainCanvas.imagePointToCanvasPoints(point2d);
                gc.fillOval(p.getX() - Mesure.FINGER_TARGET / 2.0, p.getY() - Mesure.FINGER_TARGET / 2.0,
                    Mesure.FINGER_TARGET, Mesure.FINGER_TARGET);
            }
        }
    }
}
