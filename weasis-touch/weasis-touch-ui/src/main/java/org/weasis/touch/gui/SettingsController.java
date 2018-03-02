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
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.touch.Messages;
import org.weasis.touch.WeasisPreferences;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SettingsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsController.class);
    
    public static final String ABOUT = "about";
    public static final String USER_MANUAL = "userManual";
    public static final String VIEWER_SETTINGS = "viewerSettings";
    public static final String GENERAL_SETTINGS = "generalSettings";

    @FXML
    StackPane mainPane;
    @FXML
    private VBox categorie;
    @FXML
    private StackPane detail;
    @FXML
    private AnchorPane background;
    @FXML
    private Node userPreference;
    @FXML
    private JFXButton close;
    @FXML
    private JFXButton restoreDefault;

    private MenuController menuController;
    private Preferences prefs;

    @FXML
    private void initialize() {
        for (Node node : categorie.getChildren()) {
            JFXButton button = (JFXButton) node;
            button.setOnAction(this::handleOnAction);
            if (button.getId().compareTo(GENERAL_SETTINGS) == 0) {
                button.setStyle("-fx-background-color:#b3471122");
            }
        }

        background.setOnTouchReleased(this::handleCloseSettings);
        background.setOnMouseClicked(this::handleOnMouseClicked);
        close.setOnAction(this::handleCloseSettings);
        restoreDefault.setOnAction(this::handleRestoreDefault);

        prefs = Preferences.userRoot().node(SettingsController.class.getName());
    }

    public void setParam(MenuController menuController) {
        this.menuController = menuController;
        Scene scene = this.menuController.menuGroup.getScene();
        this.mainPane.maxWidthProperty().bind(this.mainPane.prefWidthProperty());
        this.mainPane.minWidthProperty().bind(this.mainPane.prefWidthProperty());
        this.mainPane.prefWidthProperty().bind(scene.widthProperty());

        this.mainPane.maxHeightProperty().bind(this.mainPane.prefHeightProperty());
        this.mainPane.minHeightProperty().bind(this.mainPane.prefHeightProperty());
        this.mainPane.prefHeightProperty().bind(scene.heightProperty());
    }

    private void handleRestoreDefault(ActionEvent event) {
        String currentSettingPage = detail.getChildren().get(0).getId();
        switch (currentSettingPage) {
            case GENERAL_SETTINGS:
                prefs.putBoolean(WeasisPreferences.FULL_SCREEN.name(),
                    (Boolean) WeasisPreferences.FULL_SCREEN.defaultValue());
                prefs.putBoolean(WeasisPreferences.SHOW_TUTO.name(),
                    (Boolean) WeasisPreferences.SHOW_TUTO.defaultValue());
                prefs.putBoolean(WeasisPreferences.ASK_EXIT.name(),
                    (Boolean) WeasisPreferences.ASK_EXIT.defaultValue());
                prefs.putBoolean(WeasisPreferences.ASK_RESET.name(),
                    (Boolean) WeasisPreferences.ASK_RESET.defaultValue());
                prefs.putBoolean(WeasisPreferences.SHOW_NOTIFICATIONS.name(),
                    (Boolean) WeasisPreferences.SHOW_NOTIFICATIONS.defaultValue());
                break;
            case VIEWER_SETTINGS:
                prefs.putInt(WeasisPreferences.WINDOW_SENSITIVITY.name(),
                    (int) WeasisPreferences.WINDOW_SENSITIVITY.defaultValue());
                prefs.putInt(WeasisPreferences.LEVEL_SENSITIVITY.name(),
                    (int) WeasisPreferences.LEVEL_SENSITIVITY.defaultValue());
                prefs.putBoolean(WeasisPreferences.WINDOW_ORIENTATION.name(),
                    (Boolean) WeasisPreferences.WINDOW_ORIENTATION.defaultValue());
                prefs.putBoolean(WeasisPreferences.LEVEL_ORIENTATION.name(),
                    (Boolean) WeasisPreferences.LEVEL_ORIENTATION.defaultValue());
                prefs.putBoolean(WeasisPreferences.HIDE_SHOW_SCROLL.name(),
                    (Boolean) WeasisPreferences.HIDE_SHOW_SCROLL.defaultValue());
                prefs.putBoolean(WeasisPreferences.HIDE_THUMBNAIL.name(),
                    (Boolean) WeasisPreferences.HIDE_THUMBNAIL.defaultValue());
                break;
        }
        this.loadPage(currentSettingPage);
        event.consume();
    }

    private void handleCloseSettings(Event event) {
        menuController.hideSettings();
        event.consume();
    }

    private void handleOnMouseClicked(MouseEvent event) {
        if (!event.isSynthesized()) {
            menuController.hideSettings();
        }
        event.consume();
    }

    private void handleOnAction(ActionEvent event) {
        String buttonId = ((JFXButton) event.getSource()).getId();
        loadPage(buttonId);
    }

    public void loadFirstPage() {
        loadPage(GENERAL_SETTINGS);
    }

    private void loadPage(String pageName) {
        for (Node node : categorie.getChildren()) {
            JFXButton button = (JFXButton) node;
            if (button.getId().compareTo(pageName) == 0) {
                button.setStyle("-fx-background-color:#b3471122");
            } else {
                button.setStyle("-fx-background-color:none");
            }
        }

        detail.getChildren().clear();
        String ressourceName = null;
        switch (pageName) {
            case GENERAL_SETTINGS:
                restoreDefault.setVisible(true);
                ressourceName = "GeneralSettingsView.fxml";
                break;
            case VIEWER_SETTINGS:
                restoreDefault.setVisible(true);
                ressourceName = "ViewerSettingsView.fxml";
                break;
            case USER_MANUAL:
                restoreDefault.setVisible(false);
                ressourceName = "UserManualView.fxml";
                break;
            case ABOUT:
                restoreDefault.setVisible(false);
                ressourceName = "AboutView.fxml";
                break;
        }
        if (ressourceName != null) {
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setResources(Messages.RESOURCE_BUNDLE);
                loader.setLocation(this.getClass().getResource(ressourceName));
                loader.setClassLoader(this.getClass().getClassLoader());
                detail.getChildren().add(loader.load());
            } catch (IOException e) {
                LOGGER.error("Load settings", e);
            }
        }
    }
}
