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
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.FxUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

public class AboutController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AboutController.class);

    @FXML
    private Hyperlink weasisLink;
    @FXML
    private Hyperlink hessoLink;

    @FXML
    private void initialize() {
        weasisLink.setOnAction(this::handleOnActionLink);
        hessoLink.setOnAction(this::handleOnActionLink);
    }

    private void handleOnActionLink(ActionEvent event) {
        Hyperlink hyperlink = (Hyperlink) event.getSource();
        String uri = "";
        switch (hyperlink.getId()) {
            case "weasisLink":
                uri = "https://nroduit.github.io/en/";
                break;
            case "hessoLink":
                uri = "https://www.hes-so.ch/";
                break;
        }
        try {
            FxUtil.openInDefaultBrowser(new URL(uri));
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Cannot open URL to the system browser", e); //$NON-NLS-1$
        }
    }
}
