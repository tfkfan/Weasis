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

import java.util.List;

import org.weasis.core.api.gui.util.ActionW;
import org.weasis.dicom.codec.display.PresetWindowLevel;

import javafx.animation.FadeTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class MenuWindowLevelController {

    // Style for menu
    private static final String BORDER = "-fx-border-color:#b34711;-fx-border-width:3;";
    private static final String NO_BORDER = "-fx-border-color:none;";
    private static final String UNSELECTED_BACKGROUND_COLOR = "-fx-background-color:#00000022;";
    private static final String SELECTED_BACKGROUND_COLOR = "-fx-background-color:#b3471144;";
    private static final String NO_BACKGROUND_COLOR = "-fx-background-color:none;";

    private MenuController menuController;
    private MainCanvas canvas;

    final FadeTransition ftContrasteShow;
    final FadeTransition ftContrasteHide;

    private IntegerProperty indexNewWindowLevelKeyboardProperty = new SimpleIntegerProperty();

    public MenuWindowLevelController(MenuController menuController, MainCanvas canvas) {
        this.menuController = menuController;
        this.canvas = canvas;

        menuController.contrasteGrid.setOnTouchMoved(this::handleOnTouchOverContrasteMenu);
        menuController.contrasteGrid.setOnTouchStationary(this::handleOnTouchOverContrasteMenu);
        menuController.contrasteGrid.setOnMouseMoved(this::handleOnMouseMovedContrasteMenu);

        ftContrasteShow = new FadeTransition(Duration.millis(300), menuController.contrasteGrid);
        ftContrasteShow.setFromValue(0);
        ftContrasteShow.setToValue(1);
        ftContrasteShow.setOnFinished(this::handleFadeTransitionWindowLevelShow);

        ftContrasteHide = new FadeTransition(Duration.millis(300), menuController.contrasteGrid);
        ftContrasteHide.setFromValue(1);
        ftContrasteHide.setToValue(0);
        ftContrasteHide.setOnFinished(this::handleFadeTransitionWindowLevelHide);

        this.menuController.contrasteGrid.setVisible(false);
    }

    /*****************************************************************
     * Window / Level *
     *****************************************************************/
    private void handleOnTouchOverContraste(TouchEvent event) {
        if (event.getSource().getClass() == BorderPane.class) {
            for (Node iterable_element : menuController.contrasteGrid.getChildren()) {
                if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                    if (((BorderPane) iterable_element).getId()
                        .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                        iterable_element.setStyle(BORDER + UNSELECTED_BACKGROUND_COLOR);
                    } else {
                        iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR);
                    }
                } else {
                    iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR);
                }
            }
            BorderPane bp = (BorderPane) event.getSource();
            if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                if (bp.getId()
                    .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                    bp.setStyle(BORDER + SELECTED_BACKGROUND_COLOR);
                } else {
                    bp.setStyle(SELECTED_BACKGROUND_COLOR);
                }
            } else {
                bp.setStyle(SELECTED_BACKGROUND_COLOR);
            }
            canvas.setTemporairePresetWindowLevel(bp.getId());
        }
        event.getTouchPoint().ungrab();
        event.consume();
    }

    private void handleOnTouchReleasedContraste(TouchEvent event) {
        if (event.getTouchCount() == 1) {
            flagWindowLevelNewValue = true;
            canvas.setPresetWindowLevel(((BorderPane) event.getSource()).getId());
            menuController.close();
        }
        event.consume();
    }

    private void handleOnTouchOverContrasteMenu(TouchEvent event) {
        for (Node iterable_element : menuController.contrasteGrid.getChildren()) {
            iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR);
            if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                if (((BorderPane) iterable_element).getId()
                    .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                    iterable_element.setStyle(UNSELECTED_BACKGROUND_COLOR + BORDER);
                }
            }
        }
        if (presetWL != null) {
            canvas.setTemporairePresetWindowLevel(presetWL.getName());
        } else {
            canvas.setWindowLevel(windowTemp, levelTemp);
        }
        event.getTouchPoint().ungrab();
        event.consume();
    }

    private void handleOnMouseMovedContrasteMenu(MouseEvent event) {
        if (!event.isSynthesized()) {
            if (presetWL != null) {
                canvas.setTemporairePresetWindowLevel(presetWL.getName());
            } else {
                canvas.setWindowLevel(windowTemp, levelTemp);
            }
        }
        event.consume();
    }

    private void handleOnMouseEnteredContraste(MouseEvent event) {
        if (!event.isSynthesized() && event.getSource().getClass() == BorderPane.class) {
            BorderPane source = (BorderPane) event.getSource();
            if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                if ((source).getId()
                    .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                    source.setStyle(BORDER + UNSELECTED_BACKGROUND_COLOR);
                } else {
                    source.setStyle(UNSELECTED_BACKGROUND_COLOR);
                }
            } else {
                source.setStyle(UNSELECTED_BACKGROUND_COLOR);
            }

            BorderPane bp = (BorderPane) event.getSource();
            if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                if (bp.getId()
                    .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                    bp.setStyle(BORDER + SELECTED_BACKGROUND_COLOR);
                } else {
                    bp.setStyle(SELECTED_BACKGROUND_COLOR);
                }
            } else {
                bp.setStyle(SELECTED_BACKGROUND_COLOR);
            }
            canvas.setTemporairePresetWindowLevel(bp.getId());
        }
        event.consume();
    }

    private void handleOnMouseExitedContraste(MouseEvent event) {
        if (!event.isSynthesized()) {
            BorderPane source = (BorderPane) event.getSource();
            source.setStyle(UNSELECTED_BACKGROUND_COLOR);
            if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null && (source).getId()
                .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                source.setStyle(UNSELECTED_BACKGROUND_COLOR + BORDER);
            }
        }
        event.consume();
    }

    private void handleOnMouseClickedContraste(MouseEvent event) {
        if (!event.isSynthesized()) {
            flagWindowLevelNewValue = true;
            canvas.setPresetWindowLevel(((BorderPane) event.getSource()).getId());
            menuController.contrasteGrid.setDisable(true);
            menuController.close();
        }
        event.consume();
    }

    private Double windowTemp;
    private Double levelTemp;
    private PresetWindowLevel presetWL;
    private Boolean flagWindowLevelNewValue = false;

    public static final int MAX_COLUMN = 5;

    void windowLevel() {
        List<PresetWindowLevel> listWindowLevel =
            (List<PresetWindowLevel>) canvas.getActionValue(MainCanvas.PRESET_LIST_WINDOW_LEVEL);
        windowTemp = (Double) canvas.getActionValue(ActionW.WINDOW.cmd());
        levelTemp = (Double) canvas.getActionValue(ActionW.LEVEL.cmd());
        presetWL = (PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd());
        if (!menuController.contrasteGrid.isVisible() && listWindowLevel != null) {

            // clean constrasteGrid
            menuController.contrasteGrid.getChildren().remove(0, menuController.contrasteGrid.getChildren().size());
            menuController.contrasteGrid.getRowConstraints().remove(0,
                menuController.contrasteGrid.getRowConstraints().size());
            menuController.contrasteGrid.getColumnConstraints().remove(0,
                menuController.contrasteGrid.getColumnConstraints().size());

            // create new contrasteGrid
            int nbColumn = MAX_COLUMN;
            int nbRow = 1;

            if (listWindowLevel.size() / MAX_COLUMN >= 1) {
                nbRow = (int) Math.ceil(listWindowLevel.size() / (double) MAX_COLUMN);
            } else {
                nbColumn = listWindowLevel.size() % MAX_COLUMN;
            }
            for (int r = 0; r < nbRow; r++) {
                menuController.contrasteGrid.getRowConstraints().add(new RowConstraints(40));
            }
            for (int c = 0; c < nbColumn; c++) {
                menuController.contrasteGrid.getColumnConstraints()
                    .add(new ColumnConstraints(menuController.menu.getColumnConstraints().get(0).getPrefWidth()));
            }

            // fill contrasteGrid
            int index = 0;
            for (PresetWindowLevel presetWindowLevel : listWindowLevel) {

                BorderPane bp = new BorderPane();
                Text t = new Text(presetWindowLevel.getName());
                bp.setCenter(t);
                bp.setId(presetWindowLevel.getName());

                bp.setOnTouchMoved(this::handleOnTouchOverContraste);
                bp.setOnTouchStationary(this::handleOnTouchOverContraste);
                bp.setOnTouchReleased(this::handleOnTouchReleasedContraste);

                bp.setOnMouseEntered(this::handleOnMouseEnteredContraste);
                bp.setOnMouseExited(this::handleOnMouseExitedContraste);
                bp.setOnMouseClicked(this::handleOnMouseClickedContraste);
                bp.setOnMouseMoved(e -> e.consume());

                menuController.contrasteGrid.add(bp, index % MAX_COLUMN, index / MAX_COLUMN);
                index++;
            }
            menuController.contrasteGrid.setLayoutY((-50) * nbRow - 15);

            for (Node presetContraste : menuController.contrasteGrid.getChildren()) {
                if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                    if (((BorderPane) presetContraste).getId()
                        .compareTo(((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                        presetContraste.setStyle(BORDER + UNSELECTED_BACKGROUND_COLOR);
                    } else {
                        presetContraste.setStyle(NO_BORDER + UNSELECTED_BACKGROUND_COLOR);
                    }
                } else {
                    presetContraste.setStyle(NO_BORDER + UNSELECTED_BACKGROUND_COLOR);
                }
            }
            menuController.contrasteGrid.setVisible(true);
            ftContrasteShow.playFromStart();
        } else if (listWindowLevel == null) {
            menuController.close();
        }

        this.indexNewWindowLevelKeyboardProperty.addListener((ChangeListener<Object>) (o, oldVal, newVal) -> {
            if ((Integer) newVal >= 0) {
                for (Node presetContraste : menuController.contrasteGrid.getChildren()) {
                    if ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd()) != null) {
                        if (((BorderPane) presetContraste).getId().compareTo(
                            ((PresetWindowLevel) canvas.getActionValue(ActionW.PRESET.cmd())).getName()) == 0) {
                            presetContraste.setStyle(BORDER + UNSELECTED_BACKGROUND_COLOR);
                        } else {
                            presetContraste.setStyle(NO_BORDER + UNSELECTED_BACKGROUND_COLOR);
                        }
                    } else {
                        presetContraste.setStyle(NO_BORDER + UNSELECTED_BACKGROUND_COLOR);
                    }
                }
            }
        });
    }

    void closeWindowLevel() {
        if (menuController.contrasteGrid.isVisible()) {
            if (!flagWindowLevelNewValue) {
                if (presetWL != null) {
                    canvas.setPresetWindowLevel(presetWL.getName());
                } else {
                    canvas.setWindowLevel(windowTemp, levelTemp);
                }
            }
            flagWindowLevelNewValue = false;
            ftContrasteHide.playFromStart();
        }
    }

    private void handleFadeTransitionWindowLevelShow(ActionEvent event) {
        this.indexNewWindowLevelKeyboardProperty.bind(canvas.indexNewWindowLevelKeyboardProperty);
        // System.out.println(indexNewWindowLevelKeyboardProperty.get());
        event.consume();
    }

    private void handleFadeTransitionWindowLevelHide(ActionEvent event) {
        menuController.contrasteGrid.setVisible(false);
        menuController.contrasteGrid.setDisable(false);
        this.indexNewWindowLevelKeyboardProperty.unbind();
        event.consume();
    }
}