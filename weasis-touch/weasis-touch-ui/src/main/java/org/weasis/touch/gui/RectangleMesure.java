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
import javafx.scene.shape.StrokeLineJoin;

public class RectangleMesure implements Mesure {

    public static final Integer LINE = 1;
    public static final Integer RECTANGLE = 2;

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

    public RectangleMesure(Integer scroll, Color color, Double pixelSize, Unit unit, MainCanvas mainCanvas) {
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
        this.draw();
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
        computeText();
    }

    private void computeText() {
        DecimalFormat f = new DecimalFormat("##.##");
        Double h = null;
        Double w = null;
        if (isComplet()) {
            w = Math.abs(point2DList.get(0).getX() - point2DList.get(1).getX()) * pixelSize;
            h = Math.abs(point2DList.get(0).getY() - point2DList.get(1).getY()) * pixelSize;
        } else if (point2DList.size() == NB_POINTS - 1 && temporaryPoint != null) {
            Point2D temp = mainCanvas.canvasPointToImagePoint(temporaryPoint);
            w = Math.abs(point2DList.get(0).getX() - temp.getX()) * pixelSize;
            h = Math.abs(point2DList.get(0).getY() - temp.getY()) * pixelSize;
        }

        if (h != null) {
            value = Messages.getString("WeasisTouchMeasure.height") + f.format(h) + " " + unit.getAbbreviation() + "\n";
            value += Messages.getString("WeasisTouchMeasure.width") + f.format(w) + " " + unit.getAbbreviation() + "\n";
            value += Messages.getString("WeasisTouchMeasure.area") + f.format(h * w) + " " + unit.getAbbreviation()
                + Character.toString((char) 178);
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
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean isLine(double x, double y) {
        oldOrigine = null;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                int k = j == 0 ? 1 : 0;
                Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(i));
                Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(j).getX(), point2DList.get(k).getY());
                if (Line2D.ptSegDistSq(p1.getX(), p1.getY(), p2.getX(), p2.getY(), x, y) < Math.pow(Mesure.FINGER_TARGET / 2, 2)) {
                    oldOrigine = new Point2D(x, y);
                    return true;
                }
            }
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
    }

    @Override
    public void draw() {
        GraphicsContext gc = mainCanvas.getGraphicsContext2D();

        if (editModeProperty.get() && isComplet()) {
            this.drawEditMode();
        }

        gc.setStroke(color);
        gc.setLineWidth(1);
        gc.setLineJoin(StrokeLineJoin.MITER);
        if (isComplet()) {
            Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
            Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(1));

            gc.strokeRect(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()),
                Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));

        } else {
            for (Point2D point2d : point2DList) {
                Point2D p = mainCanvas.imagePointToCanvasPoints(point2d);
                gc.strokeRect(p.getX() - 2.5, p.getY() - 2.5, 5, 5);
            }
            if (point2DList.size() == NB_POINTS - 1 && temporaryPoint != null) {
                Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
                gc.strokeRect(Math.min(p1.getX(), temporaryPoint.getX()), Math.min(p1.getY(), temporaryPoint.getY()),
                    Math.abs(p1.getX() - temporaryPoint.getX()), Math.abs(p1.getY() - temporaryPoint.getY()));
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
            Point2D p2 = mainCanvas.imagePointToCanvasPoints(temporaryPoint);
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

            gc.setStroke(c.darker().darker().darker());
            Point2D p1 = mainCanvas.imagePointToCanvasPoints(point2DList.get(0));
            Point2D p2 = mainCanvas.imagePointToCanvasPoints(point2DList.get(1));
            gc.setLineWidth(Mesure.FINGER_TARGET);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.strokeRect(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()),
                Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));

            gc.setFill(c.brighter().brighter().brighter());
            for (Point2D point2d : point2DList) {
                Point2D p = mainCanvas.imagePointToCanvasPoints(point2d);
                gc.fillOval(p.getX() - Mesure.FINGER_TARGET / 2.0, p.getY() - Mesure.FINGER_TARGET / 2.0,
                    Mesure.FINGER_TARGET, Mesure.FINGER_TARGET);
            }
        }
    }
}
