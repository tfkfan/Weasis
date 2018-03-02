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

import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ViewerSettingsController {

    @FXML
    private JFXSlider windowSensitivity;
    @FXML
    private JFXSlider levelSensitivity;
    @FXML
    private JFXToggleButton windowOrientation;
    @FXML
    private JFXToggleButton levelOrientation;
    @FXML
    private JFXToggleButton hideShowScroll;
    @FXML
    private JFXToggleButton hideThumbnail;

    private Preferences prefs;

    @FXML
    private void initialize() {
        windowSensitivity.valueProperty().addListener(this::handleWindowSensitivity);
        levelSensitivity.valueProperty().addListener(this::handleLevelSensitivity);

        windowOrientation.setOnAction(this::handleWindowOrientation);
        levelOrientation.setOnAction(this::handleLevelOrientation);
        hideShowScroll.setOnAction(this::handleHideShowScroll);
        hideThumbnail.setOnAction(this::handleHideThumbnail);

        prefs = Preferences.userRoot().node(SettingsController.class.getName());

        windowSensitivity.setValue(prefs.getInt(WeasisPreferences.WINDOW_SENSITIVITY.name(),
            (int) WeasisPreferences.WINDOW_SENSITIVITY.defaultValue()));
        levelSensitivity.setValue(prefs.getInt(WeasisPreferences.LEVEL_SENSITIVITY.name(),
            (int) WeasisPreferences.LEVEL_SENSITIVITY.defaultValue()));
        windowOrientation.setSelected(prefs.getBoolean(WeasisPreferences.WINDOW_ORIENTATION.name(),
            (Boolean) WeasisPreferences.WINDOW_ORIENTATION.defaultValue()));
        levelOrientation.setSelected(prefs.getBoolean(WeasisPreferences.LEVEL_ORIENTATION.name(),
            (Boolean) WeasisPreferences.LEVEL_ORIENTATION.defaultValue()));
        hideShowScroll.setSelected(prefs.getBoolean(WeasisPreferences.HIDE_SHOW_SCROLL.name(),
            (Boolean) WeasisPreferences.HIDE_SHOW_SCROLL.defaultValue()));
        hideThumbnail.setSelected(prefs.getBoolean(WeasisPreferences.HIDE_THUMBNAIL.name(),
            (Boolean) WeasisPreferences.HIDE_THUMBNAIL.defaultValue()));
    }

    private void handleWindowSensitivity(ObservableValue<? extends Number> observableValue, Number oldVal,
        Number newVal) {
        prefs.putInt(WeasisPreferences.WINDOW_SENSITIVITY.name(), newVal.intValue());
    }

    private void handleLevelSensitivity(ObservableValue<? extends Number> observableValue, Number oldVal,
        Number newVal) {
        prefs.putInt(WeasisPreferences.LEVEL_SENSITIVITY.name(), newVal.intValue());
    }

    private void handleWindowOrientation(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.WINDOW_ORIENTATION.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleLevelOrientation(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.LEVEL_ORIENTATION.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleHideShowScroll(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.HIDE_SHOW_SCROLL.name(), toggleButton.isSelected());
        event.consume();
    }

    private void handleHideThumbnail(ActionEvent event) {
        JFXToggleButton toggleButton = (JFXToggleButton) event.getSource();
        prefs.putBoolean(WeasisPreferences.HIDE_THUMBNAIL.name(), toggleButton.isSelected());
        event.consume();
    }
}
