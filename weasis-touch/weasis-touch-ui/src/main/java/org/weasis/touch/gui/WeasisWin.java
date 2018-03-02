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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.model.TreeModel;
import org.weasis.core.api.explorer.model.TreeModelNode;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.touch.Messages;

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WeasisWin {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeasisWin.class);

    MainViewController mainViewConroller;
    MenuController menuController;
    ScrollController scrollController;
    SettingsController settingsController;

    private volatile boolean busy = false;

    private final List<Runnable> runOnClose = new ArrayList<>();

    private final AnchorPane rootPaneContainer = new AnchorPane();
    private final Stage mainStage;

    public WeasisWin() {
        this.mainStage = new Stage(StageStyle.DECORATED);
        mainStage.setTitle(AppProperties.WEASIS_NAME + " v" + AppProperties.WEASIS_VERSION); //$NON-NLS-1$
        Image icon = ResourceUtil.getFxIconLogo64();
        if (icon != null) {
            mainStage.getIcons().add(icon);
        }
        mainStage.setOnCloseRequest(event -> {
            if (!closeWindow()) {
                event.consume();
            }
        });
    }

    public Stage getMainStage() {
        return mainStage;
    }

    public AnchorPane getRootPaneContainer() {
        return rootPaneContainer;
    }

    public boolean closeWindow() {
        if (busy) {
            // TODO add a message, Please wait or kill
            return false;
        }
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty("weasis.confirm.closing", false)) { //$NON-NLS-1$
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setContentText(Messages.getString("WeasisWin.exit_mes"));//$NON-NLS-1$

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                closeAllRunnable();
                System.exit(0);
                return true;
            }
        } else {
            closeAllRunnable();
            System.exit(0);
            return true;
        }
        return false;
    }

    private void closeAllRunnable() {
        for (Runnable onClose : runOnClose) {
            onClose.run();
        }
    }

    public MainCanvas getCanvas() {
        return new MainCanvas(null);
    }

    public void runOnClose(Runnable run) {
        runOnClose.add(run);
    }

    public void createMainPanel() throws Exception {
        Scene scene = new Scene(rootPaneContainer, Color.WHITE);
        mainStage.setMinWidth(400);
        mainStage.setMinHeight(400);

        FXMLLoader loaderVC = new FXMLLoader();
        loaderVC.setLocation(this.getClass().getResource("MainView.fxml"));
        loaderVC.setClassLoader(this.getClass().getClassLoader());
        StackPane mainView = loaderVC.load();
        mainViewConroller = loaderVC.getController();

        FXMLLoader loaderMenu = new FXMLLoader();
        loaderMenu.setResources(Messages.RESOURCE_BUNDLE);
        loaderMenu.setLocation(this.getClass().getResource("MenuView.fxml"));
        loaderMenu.setClassLoader(this.getClass().getClassLoader());
        Group menu = loaderMenu.load();
        menuController = loaderMenu.getController();

        mainViewConroller.lockedProperty.bindBidirectional(menuController.lockedProperty);
        mainViewConroller.blurProperty.bindBidirectional(menuController.blurProperty);

        rootPaneContainer.getChildren().add(mainView);
        rootPaneContainer.getChildren().add(menu);

        mainStage.setScene(scene);

        mainViewConroller.setParam(menuController);
        menuController.setParam(mainViewConroller, mainStage);

        // Too to show UI layer
        // ScenicView.show(scene);
    }

    public ThumbnailViewerController getThumbnailViewerController() {
        return mainViewConroller.thumbnailViewerController;
    }

    HashMap<MediaSeriesGroup, List<MediaSeries<?>>> getSeriesByEntry(TreeModel treeModel,
        List<? extends MediaSeries<?>> series, TreeModelNode entry) {
        HashMap<MediaSeriesGroup, List<MediaSeries<?>>> map = new HashMap<>();
        if (series != null && treeModel != null && entry != null) {
            for (MediaSeries<?> s : series) {
                MediaSeriesGroup entry1 = treeModel.getParent(s, entry);
                List<MediaSeries<?>> seriesList = Optional.ofNullable(map.get(entry1)).orElseGet(ArrayList::new);
                seriesList.add(s);
                map.put(entry1, seriesList);
            }
        }
        return map;
    }
}
