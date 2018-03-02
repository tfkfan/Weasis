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

import org.weasis.touch.WeasisPreferences;

import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class GeneralSettingsController {

    @FXML
    private JFXToggleButton fullScreen;
    @FXML
    private JFXToggleButton showTuto;
    @FXML
    private JFXToggleButton askExit;
    @FXML
    private JFXToggleButton askReset;
    @FXML
    private JFXToggleButton notifications;
    @FXML
    private ChoiceBox<String> language;

    private Preferences prefs;

    @FXML
    private void initialize() {
        fullScreen.setOnAction(this::handleFullScreen);
        showTuto.setOnAction(this::handleShowTuto);
        askExit.setOnAction(this::handleAskExit);
        askReset.setOnAction(this::handleAskReset);
        notifications.setOnAction(this::handleNotifications);

        prefs = Preferences.userRoot().node(SettingsController.class.getName());

        language.setItems(FXCollections.observableArrayList(WeasisPreferences.lstLanguage).sorted());
        language
            .setValue(prefs.get(WeasisPreferences.LANGUAGE.name(), (String) WeasisPreferences.LANGUAGE.defaultValue()));
        language.valueProperty().addListener(this::listenerOnChangeLanguage);
        fullScreen.setSelected(prefs.getBoolean(WeasisPreferences.FULL_SCREEN.name(),
            (Boolean) WeasisPreferences.FULL_SCREEN.defaultValue()));
        showTuto.setSelected(
            prefs.getBoolean(WeasisPreferences.SHOW_TUTO.name(), (Boolean) WeasisPreferences.SHOW_TUTO.defaultValue()));
        askExit.setSelected(
            prefs.getBoolean(WeasisPreferences.ASK_EXIT.name(), (Boolean) WeasisPreferences.ASK_EXIT.defaultValue()));
        askReset.setSelected(
            prefs.getBoolean(WeasisPreferences.ASK_RESET.name(), (Boolean) WeasisPreferences.ASK_RESET.defaultValue()));
        notifications.setSelected(prefs.getBoolean(WeasisPreferences.SHOW_NOTIFICATIONS.name(),
            (Boolean) WeasisPreferences.SHOW_NOTIFICATIONS.defaultValue()));
    }

    private void listenerOnChangeLanguage(ObservableValue<? extends String> observableValue, String oldVal,
        String newVal) {
        prefs.put(WeasisPreferences.LANGUAGE.name(), newVal);
    }

    private void handleFullScreen(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.FULL_SCREEN.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleShowTuto(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.SHOW_TUTO.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleAskExit(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.ASK_EXIT.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleAskReset(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.ASK_RESET.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleNotifications(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.SHOW_NOTIFICATIONS.name(), toggleButton.isSelected());
        event.consume();
    }
}
