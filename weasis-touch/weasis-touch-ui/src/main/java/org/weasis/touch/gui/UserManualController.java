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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.touch.Messages;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class UserManualController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserManualController.class);

    @FXML
    private TitledPane general;
    @FXML
    private TitledPane gesture;
    @FXML
    private TitledPane menu;
    @FXML
    private TitledPane mouseKeyboard;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private Accordion accordion;

    private VBox vb;

    @FXML
    private void initialize() {
        addExpandedPropertyListener(general, "UserManualGeneralView");
        addExpandedPropertyListener(gesture, "UserManualGestureView");
        addExpandedPropertyListener(menu, "UserManualMenuView");
        addExpandedPropertyListener(mouseKeyboard, "UserManualMouseKeyboardView");
    }

    private void addExpandedPropertyListener(TitledPane titledPane, String fxmlFileName) {
        titledPane.expandedProperty().addListener((ChangeListener<Boolean>) (o, oldVal, newVal) -> {
            if (newVal) {
                try {
                    FXMLLoader loaderMouseKeyboard = new FXMLLoader();
                    loaderMouseKeyboard.setResources(Messages.RESOURCE_BUNDLE);
                    loaderMouseKeyboard.setLocation(this.getClass().getResource(fxmlFileName + ".fxml"));
                    loaderMouseKeyboard.setClassLoader(this.getClass().getClassLoader());
                    vb = loaderMouseKeyboard.load();

                    titledPane.setContent(vb);

                    // TODO put the titlePane to the top of the scrollPane
                    scrollPane.setVvalue(0);
                } catch (IOException e) {
                    LOGGER.error("Load manual", e);
                }
            } else {
                titledPane.setContent(null);
            }
        });
    }
}