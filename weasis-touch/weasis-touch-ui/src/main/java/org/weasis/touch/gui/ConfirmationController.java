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

import java.util.prefs.Preferences;

import org.weasis.touch.Messages;
import org.weasis.touch.WeasisPreferences;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXToggleButton;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class ConfirmationController {

    private Preferences prefs;
    public static final int TYPE_EXIT = 1;
    public static final int TYPE_RESET = 2;
    private int type;

    @FXML
    StackPane mainPane;
    @FXML
    private JFXButton buttonYes;
    @FXML
    private JFXButton buttonNo;
    @FXML
    private AnchorPane background;
    @FXML
    private JFXToggleButton remember;
    @FXML
    private Text title;
    @FXML
    private Text text;

    private MenuController menuController;

    @FXML
    private void initialize() {
        prefs = Preferences.userRoot().node(SettingsController.class.getName());

        background.setOnTouchReleased(this::handleNo);
        background.setOnMouseClicked(this::handleOnMouseClicked);

        buttonNo.setOnAction(this::handleNo);
        buttonYes.setOnAction(this::handleButtonYes);
    }

    public void setType(Integer type, MenuController menuController) {
        this.menuController = menuController;
        this.type = type;
        Scene scene = menuController.menuGroup.getScene();

        switch (type) {
            case TYPE_EXIT:
                this.title.setText(Messages.getString("WeasisTouchConfirmation.exit"));
                this.text.setText(Messages.getString("WeasisTouchConfirmation.exitText"));
                break;
            case TYPE_RESET:
                this.title.setText(Messages.getString("WeasisTouchConfirmation.reset"));
                this.text.setText(Messages.getString("WeasisTouchConfirmation.resetText"));
                break;
        }

        this.mainPane.maxWidthProperty().bind(this.mainPane.prefWidthProperty());
        this.mainPane.minWidthProperty().bind(this.mainPane.prefWidthProperty());
        this.mainPane.prefWidthProperty().bind(scene.widthProperty());

        this.mainPane.maxHeightProperty().bind(this.mainPane.prefHeightProperty());
        this.mainPane.minHeightProperty().bind(this.mainPane.prefHeightProperty());
        this.mainPane.prefHeightProperty().bind(scene.heightProperty());
    }

    private void handleNo(Event event) {
        switch (type) {
            case TYPE_EXIT:
                menuController.exit(false);
                break;
            case TYPE_RESET:
                menuController.reset(false);
                break;
        }
    }

    private void handleOnMouseClicked(MouseEvent event) {
        if (!event.isSynthesized()) {
            switch (type) {
                case TYPE_EXIT:
                    menuController.exit(false);
                    break;
                case TYPE_RESET:
                    menuController.reset(false);
                    break;
            }
        }
    }

    private void handleButtonYes(ActionEvent event) {
        switch (type) {
            case TYPE_EXIT:
                if (remember.isSelected()) {
                    prefs.putBoolean(WeasisPreferences.ASK_EXIT.name(), false);
                }
                menuController.exit(true);
                break;
            case TYPE_RESET:
                if (remember.isSelected()) {
                    prefs.putBoolean(WeasisPreferences.ASK_RESET.name(), false);
                }
                menuController.reset(true);
                break;
        }
    }
}
