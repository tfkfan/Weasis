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

import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.layout.AnchorPane;

public class DICOMViewer extends AnchorPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(DICOMViewer.class);

    MainCanvas mainCanvas;
    private MainViewController mainViewController;
    private MenuController menuController;
    private ScrollController scrollController;

    public DICOMViewer(MainViewController mainViewController, MenuController menuController) {
        this.mainViewController = mainViewController;
        this.menuController = menuController;

        mainCanvas = new MainCanvas(mainViewController);

        mainCanvas.canvasTouchController.setMenu(this.menuController);
        mainCanvas.canvasMouseController.setMenu(this.menuController);
        this.getChildren().add(mainCanvas);

        try {
            FXMLLoader loaderScroll = new FXMLLoader();
            loaderScroll.setLocation(this.getClass().getResource("ScrollView.fxml"));
            loaderScroll.setClassLoader(this.getClass().getClassLoader());
            Group scroll = loaderScroll.load();
            scrollController = loaderScroll.getController();
            this.getChildren().add(scroll);
        } catch (IOException e) {
            LOGGER.error("build scroll view", e);
        }
        mainCanvas.setScrollBar(scrollController);

        this.setMinHeight(0);
        this.setMinWidth(0);

        this.mainCanvas.heightProperty().bind(this.heightProperty());
        this.mainCanvas.widthProperty().bind(this.widthProperty());

        scrollController.blurProperty.bindBidirectional(this.mainViewController.blurProperty);
        scrollController.lockedProperty.bindBidirectional(this.mainViewController.lockedProperty);
        scrollController.measureInProgress.bindBidirectional(mainCanvas.measureInProgress);

        mainCanvas.canvasTouchController.lockedProperty.bindBidirectional(this.mainViewController.lockedProperty);
        mainCanvas.canvasMouseController.lockedProperty.bindBidirectional(this.mainViewController.lockedProperty);
        mainCanvas.showMeasureProperty.bindBidirectional(this.menuController.showMeasureProperty);
        mainCanvas.measureInProgress.bindBidirectional(this.menuController.measureInProgress);
        mainCanvas.editModeProperty.bindBidirectional(this.menuController.editModeProperty);

        AnchorPane.setTopAnchor(this, 0.0);
        AnchorPane.setLeftAnchor(this, 0.0);
        AnchorPane.setBottomAnchor(this, 0.0);
        AnchorPane.setRightAnchor(this, 0.0);

        this.scrollController.setParam(mainCanvas);
    }
}
