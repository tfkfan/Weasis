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

import javafx.animation.FadeTransition;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public class MenuToolsControlleur {

    // Style for menu
    private static final String BORDER = "-fx-border-color:#b34711;-fx-border-width:3;";
    private static final String NO_BORDER = "-fx-border-color:none;";
    private static final String UNSELECTED_BACKGROUND_COLOR = "-fx-background-color:#00000022;";
    private static final String SELECTED_BACKGROUND_COLOR = "-fx-background-color:#b3471144;";
    private static final String NO_BACKGROUND_COLOR = "-fx-background-color:none;";

    private MenuController menuController;
    private MainCanvas canvas;
    private FadeTransition ftToolsShow;
    final FadeTransition ftToolsHide;

    public MenuToolsControlleur(MenuController menuController, MainCanvas canvas) {
        this.menuController = menuController;
        this.canvas = canvas;

        menuController.toolsGrid.setOnTouchMoved(this::handleOnTouchOverToolsMenu);
        menuController.toolsGrid.setOnTouchStationary(this::handleOnTouchOverToolsMenu);
        menuController.toolsGrid.setOnMouseMoved(this::handleOnMouseMovedToolsMenu);

        for (Node iterable_element : menuController.toolsGrid.getChildren()) {
            if (iterable_element.getClass() == BorderPane.class) {
                BorderPane bp = (BorderPane) iterable_element;

                bp.setOnTouchMoved(this::handleOnTouchOverTools);
                bp.setOnTouchStationary(this::handleOnTouchOverTools);
                bp.setOnTouchReleased(this::handleOnTouchReleasedTools);

                bp.setOnMouseEntered(this::handleOnMouseEnteredTools);
                bp.setOnMouseExited(this::handleOnMouseExitedTools);
                bp.setOnMouseClicked(this::handleOnMouseClickedTools);
                bp.setOnMouseMoved(MouseEvent::consume);
            }
        }

        ftToolsShow = new FadeTransition(Duration.millis(300), menuController.toolsGrid);
        ftToolsShow.setFromValue(0);
        ftToolsShow.setToValue(1);

        ftToolsHide = new FadeTransition(Duration.millis(300), menuController.toolsGrid);
        ftToolsHide.setFromValue(1);
        ftToolsHide.setToValue(0);
        ftToolsHide.setOnFinished(this::handleFadeTransitionToolsEnd);

        menuController.showHideMeasure.selectedProperty().bindBidirectional(this.menuController.showMeasureProperty);
        menuController.showHideMeasure.selectedProperty().addListener(this::handleSelectedPropertyShowHideMeasure);

        menuController.editMode.selectedProperty().bindBidirectional(this.menuController.editModeProperty);
        menuController.editMode.selectedProperty().addListener(this::handleSelectedPropertyEditMode);
        menuController.colorPicker.setValue(Mesure.DEFAULT_COLOR);

        menuController.trashMeasure.visibleProperty().bind(this.menuController.editModeProperty);
        menuController.exitMeasure.visibleProperty()
            .bind(this.menuController.measureInProgress.greaterThan(0).or(this.menuController.editModeProperty));
        menuController.buttonPlus.visibleProperty().bind(this.menuController.exitMeasure.visibleProperty().not());

        menuController.exitMeasure.setOnMouseClicked(this::handleExitMeasure);

        canvas.scrollController.scrollbarGroup.visibleProperty()
            .bind(this.menuController.editModeProperty.not().and(this.menuController.measureInProgress.isEqualTo(0)));

        menuController.editMode.selectedProperty().addListener(this::handleEditModeSelected);

        menuController.exitMeasure.setOnMouseEntered(e -> {
            if (!e.isSynthesized()) {
                menuController.exitMeasure.setScaleX(menuController.exitMeasure.getScaleX() + 0.1);
                menuController.exitMeasure.setScaleY(menuController.exitMeasure.getScaleY() + 0.1);
            }
            e.consume();
        });
        menuController.exitMeasure.setOnMouseExited(e -> {
            if (!e.isSynthesized()) {
                menuController.exitMeasure.setScaleX(menuController.exitMeasure.getScaleX() - 0.1);
                menuController.exitMeasure.setScaleY(menuController.exitMeasure.getScaleY() - 0.1);
            }
            e.consume();
        });
    }

    private void handleEditModeSelected(ObservableValue<? extends Boolean> observableValue, Boolean oldVal,
        Boolean newVal) {
        if (newVal) {
            menuController.close();
        }
    }

    private void handleExitMeasure(MouseEvent event) {
        canvas.mesures.cancelMeasurInProgress();
        menuController.editModeProperty.set(false);
        menuController.measureInProgress.set(0);
    }

    private void handleSelectedPropertyEditMode(ObservableValue<? extends Boolean> observableValue, Boolean oldValue,
        Boolean newValue) {
        if (newValue) {
            menuController.showMeasureProperty.set(true);
            this.menuController.trashMeasureSVG.setScaleX(0.12);
            this.menuController.trashMeasureSVG.setScaleY(0.12);
        }
        canvas.draw();
    }

    private void handleSelectedPropertyShowHideMeasure(ObservableValue<? extends Boolean> observableValue,
        Boolean oldValue, Boolean newValue) {
        if (!newValue) {
            menuController.editModeProperty.set(false);
        }
    }

    private void handleOnTouchOverTools(TouchEvent event) {
        if (event.getSource().getClass() == BorderPane.class) {
            for (Node iterable_element : menuController.toolsGrid.getChildren()) {
                if (iterable_element.getClass() == BorderPane.class) {
                    iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR);
                }
            }
            ((BorderPane) event.getSource()).setStyle(SELECTED_BACKGROUND_COLOR);
        }
        event.getTouchPoint().ungrab();
        event.consume();
    }

    private void handleOnTouchReleasedTools(TouchEvent event) {
        if (event.getTouchCount() == 1) {
            this.buttonSelected((BorderPane) event.getSource());
        }
        event.consume();
    }

    private void handleOnTouchOverToolsMenu(TouchEvent event) {
        for (Node iterable_element : menuController.toolsGrid.getChildren()) {
            if (iterable_element.getClass() == BorderPane.class) {
                iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR);
            }
        }
        event.getTouchPoint().ungrab();
        event.consume();
    }

    private void handleOnMouseMovedToolsMenu(MouseEvent event) {
        if (!event.isSynthesized()) {
        }
        event.consume();
    }

    private void handleOnMouseEnteredTools(MouseEvent event) {
        if (!event.isSynthesized()) {
            if (event.getSource().getClass() == BorderPane.class) {
                ((BorderPane) event.getSource()).setStyle(SELECTED_BACKGROUND_COLOR);
            }
        }
        event.consume();
    }

    private void handleOnMouseExitedTools(MouseEvent event) {
        if (!event.isSynthesized()) {
            BorderPane source = (BorderPane) event.getSource();
            source.setStyle(UNSELECTED_BACKGROUND_COLOR);
        }
        event.consume();
    }

    private void handleOnMouseClickedTools(MouseEvent event) {
        if (!event.isSynthesized()) {
            this.buttonSelected((BorderPane) event.getSource());
        }
        event.consume();
    }

    private void buttonSelected(BorderPane button) {
        menuController.close();
        switch (button.getId()) {
            case "line":
                menuController.measureInProgress.set(Mesure.LINE);
                canvas.addMeasure(Mesure.LINE, menuController.colorPicker.getValue());
                break;
            case "rectangle":
                canvas.addMeasure(Mesure.RECTANGLE, menuController.colorPicker.getValue());
                break;
        }
        menuController.showMeasureProperty.set(true);
        menuController.editModeProperty.set(false);
    }

    public void tools() {
        if (!canvas.isImageNull()) {
            if (!menuController.toolsGrid.isVisible()) {
                if (canvas.mesures.nbMeasureInScroll() == 0) {
                    this.menuController.editMode.setDisable(true);
                    this.menuController.showHideMeasure.setDisable(true);
                } else {
                    this.menuController.editMode.setDisable(false);
                    this.menuController.showHideMeasure.setDisable(false);
                }
                menuController.toolsGrid.setVisible(true);
                ftToolsShow.playFromStart();
            }
        } else {
            menuController.close();
        }
    }

    public void trashOver(PickResult pickResult) {
        try {
            String id = pickResult.getIntersectedNode().getId();
            if (id.compareTo("trashMeasure") == 0 || id.compareTo("trashMeasureSVG") == 0) {
                this.menuController.trashMeasureSVG.setScaleX(0.2);
                this.menuController.trashMeasureSVG.setScaleY(0.2);
            }
        } catch (Exception e) {
            this.menuController.trashMeasureSVG.setScaleX(0.12);
            this.menuController.trashMeasureSVG.setScaleY(0.12);
        }
    }

    public void removeMeasure(PickResult pickResult) {
        try {
            String id = pickResult.getIntersectedNode().getId();
            if (id.compareTo("trashMeasure") == 0 || id.compareTo("trashMeasureSVG") == 0) {
                canvas.mesures.removeActual();
                canvas.draw();
                if (canvas.mesures.nbMeasureInScroll() == 0) {
                    menuController.editModeProperty.set(false);
                } else {
                    this.menuController.trashMeasureSVG.setScaleX(0.12);
                    this.menuController.trashMeasureSVG.setScaleY(0.12);
                }
            }
        } catch (Exception e) {
        }
    }

    public Boolean cancelMeasure(PickResult pickResult) {
        try {
            String id = pickResult.getIntersectedNode().getId();

            if (id.compareTo("measureIndicator") == 0 || id.compareTo("measureIndicatorSVG") == 0) {
                canvas.mesures.cancelMeasurInProgress();
                menuController.measureInProgress.set(0);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private void handleFadeTransitionToolsEnd(ActionEvent event) {
        menuController.toolsGrid.setVisible(false);
        event.consume();
    }

    public void closeTools() {
        if (menuController.toolsGrid.isVisible()) {
            for (Node iterable_element : menuController.toolsGrid.getChildren()) {
                if (iterable_element.getClass() == BorderPane.class) {
                    iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR);
                }
            }
            if (ftToolsHide.getCurrentRate() == 0.0d) {
                ftToolsHide.playFromStart();
            }
        }
    }
}
