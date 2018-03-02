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

import org.weasis.core.api.gui.util.ActionW;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Mesures {

    private ArrayList<Mesure> mesureList;
    private MainCanvas canvas;
    BooleanProperty editModeProperty = new SimpleBooleanProperty(false);
    private Mesure measureToModifyPoint = null;
    private Mesure measureToModifyLine = null;

    public Mesures(MainCanvas canvas) {
        mesureList = new ArrayList<>();
        this.canvas = canvas;
    }

    public void addMesure(Mesure mesure) {
        mesureList.add(mesure);
        mesure.setEditModeProperty(editModeProperty);
    }

    public Mesure getLastMeasure() {
        if (!mesureList.isEmpty()) {
            return mesureList.get(mesureList.size() - 1);
        } else {
            return null;
        }
    }

    public int size() {
        return mesureList.size();
    }

    public void draw() {
        for (Mesure mesure : mesureList) {
            if (mesure.getScroll() == (int) canvas.getActionValue(ActionW.SCROLL_SERIES.cmd())) {
                mesure.draw();
            }
        }
    }

    public int nbMeasureInScroll() {
        int res = 0;
        for (Mesure mesure : mesureList) {
            if (mesure.getScroll() == (int) canvas.getActionValue(ActionW.SCROLL_SERIES.cmd())) {
                res++;
            }
        }
        return res;
    }

    public Boolean isMeasure(double x, double y) {
        measureToModifyPoint = null;
        measureToModifyLine = null;
        for (Mesure mesure : mesureList) {
            if (mesure.getScroll() == (int) canvas.getActionValue(ActionW.SCROLL_SERIES.cmd())) {
                if (measureToModifyPoint == null) {
                    if (mesure.isPoint(x, y)) {
                        measureToModifyPoint = mesure;
                        measureToModifyPoint.setIsEdited(true);
                    }
                } else {
                    mesure.clearMeasureToModify();
                    measureToModifyPoint.setIsEdited(true);
                }
            }
        }
        if (measureToModifyPoint == null) {
            for (Mesure mesure : mesureList) {
                if (mesure.getScroll() == (int) canvas.getActionValue(ActionW.SCROLL_SERIES.cmd())) {
                    if (measureToModifyLine == null) {
                        if (mesure.isLine(x, y)) {
                            measureToModifyLine = mesure;
                            measureToModifyLine.setIsEdited(true);
                        }
                    }
                }
            }
        }
        return (measureToModifyPoint == null) || (measureToModifyLine == null);
    }

    public void resetMeasureToModify() {
        if (measureToModifyPoint != null) {
            measureToModifyPoint.setIsEdited(false);
            measureToModifyPoint = null;
        } else if (measureToModifyLine != null) {
            measureToModifyLine.setIsEdited(false);
            measureToModifyLine = null;
        }
    }

    public void removeActual() {
        if (measureToModifyPoint != null) {
            mesureList.remove(measureToModifyPoint);
        } else if (measureToModifyLine != null) {
            mesureList.remove(measureToModifyLine);
        }
        resetMeasureToModify();
    }

    public void cancelMeasurInProgress() {
        Mesure last = this.getLastMeasure();
        if (!last.isComplet()) {
            mesureList.remove(last);
            canvas.draw();
        }
    }

    public Boolean editMeasure(double x, double y) {
        if (measureToModifyPoint != null) {
            if (measureToModifyPoint.getScroll() == (int) canvas.getActionValue(ActionW.SCROLL_SERIES.cmd())) {
                measureToModifyPoint.moveMeasure(x, y);
            }

        } else if (measureToModifyLine != null) {
            if (measureToModifyLine.getScroll() == (int) canvas.getActionValue(ActionW.SCROLL_SERIES.cmd())) {
                measureToModifyLine.moveMeasure(x, y);
            }
        }
        return measureToModifyLine != null || measureToModifyPoint != null;
    }
}
