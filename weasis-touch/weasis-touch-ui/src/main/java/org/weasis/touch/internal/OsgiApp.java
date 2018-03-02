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
package org.weasis.touch.internal;

import java.util.prefs.Preferences;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.weasis.touch.WeasisPreferences;
import org.weasis.touch.gui.SettingsController;
import org.weasis.touch.gui.WeasisWin;
import org.weasis.touch.gui.WeasisWinListener;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class OsgiApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.hide();
        WeasisWin mainWindow = OsgiApp.showApp(null);
        WeasisWinListener listener = new WeasisWinListener();
        listener.setMainWindow(mainWindow);
    }

    public static Stage getSplashScreen(BundleContext bundleContext) {
        try {
            for (ServiceReference<Application> serviceReference : bundleContext.getServiceReferences(Application.class,
                null)) {
                return (Stage) serviceReference.getProperty("splashScreen");
            }
        } catch (InvalidSyntaxException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    public static WeasisWin showApp(Stage splash) {
        final WeasisWin mainWindow = new WeasisWin();

        if (splash != null) {
            splash.hide();
        }

        try {
            mainWindow.createMainPanel();
        } catch (Exception e) {
            // It is better to exit than to let run a zombie process
            System.err.println("Could not start GUI: " + e); //$NON-NLS-1$
            e.printStackTrace();
            System.exit(-1);
        }
        Stage mainStage = mainWindow.getMainStage();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        mainStage.setX(bounds.getMinX());
        mainStage.setY(bounds.getMinY());
        mainStage.setWidth(bounds.getWidth());
        mainStage.setHeight(bounds.getHeight());
        // mainStage.setMaximized(true);
        Preferences prefs = Preferences.userRoot().node(SettingsController.class.getName());
        if (prefs.getBoolean(WeasisPreferences.FULL_SCREEN.name(),
            (Boolean) WeasisPreferences.FULL_SCREEN.defaultValue())) {
            mainStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            mainStage.setFullScreen(true);
        }
        mainStage.show();
        return mainWindow;
    }

}
