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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.dicom.codec.display.PresetWindowLevel;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MainCanvasKeyBoardController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainCanvasKeyBoardController.class);
    final IntegerProperty indexNewWindowLevelKeyboardProperty = new SimpleIntegerProperty(-1);

    private MainCanvas canvas;

    public MainCanvasKeyBoardController(MainCanvas canvas) {
        this.canvas = canvas;

        this.canvas.setOnKeyReleased(this::handleOnKeyReleased);
    }

    private void handleOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            canvas.reset();
        } else if (event.getCode() == KeyCode.SPACE) {
            canvas.setInfoMode(((int) this.canvas.getActionValue(MainCanvas.INFO_MODE) + 1) % 3);
        } else if (event.getCode().isDigitKey()) {
            if (!canvas.isImageNull()) {
                List<PresetWindowLevel> listWindowLevel =
                    (List<PresetWindowLevel>) canvas.getActionValue(MainCanvas.PRESET_LIST_WINDOW_LEVEL);
                Integer index = Integer.parseInt(event.getText());
                if (index < listWindowLevel.size()) {
                    canvas.setPresetWindowLevel(listWindowLevel.get(index).getName());
                    indexNewWindowLevelKeyboardProperty.set(index);
                }
            }
        }
        event.consume();
    }
}