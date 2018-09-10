/*******************************************************************************
 * Copyright (c) 2009-2018 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.base.ui.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.base.ui.gui.WeasisWin;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.service.BundlePreferences;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.pref.GeneralSetting;

public class Activator implements BundleActivator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        // Starts core bundles for initialization before calling UI components
        Bundle bundle = FrameworkUtil.getBundle(BundleTools.class);
        if (bundle != null) {
            bundle.start();
        }
        bundle = FrameworkUtil.getBundle(UIManager.class);
        if (bundle != null) {
            bundle.start();
        }
        String className = BundleTools.SYSTEM_PREFERENCES.getProperty("weasis.look"); //$NON-NLS-1$
        if (StringUtil.hasText(className)) {
            LookAndFeel lf = javax.swing.UIManager.getLookAndFeel();
            if (lf == null || !className.equals(lf.getClass().getName())) {
                GeneralSetting.setLookAndFeel(className);
            }
        }

        // WeasisWin must be instantiate in the EDT but required to end before the bundle startup
        GuiExecutor.instance().invokeAndWait(() -> {
            final WeasisWin mainWindow = new WeasisWin();
            // Register "weasis" command
            Dictionary<String, Object> dict = new Hashtable<>();
            dict.put(CommandProcessor.COMMAND_SCOPE, "weasis"); //$NON-NLS-1$
            dict.put(CommandProcessor.COMMAND_FUNCTION, WeasisWin.functions);
            bundleContext.registerService(WeasisWin.class.getName(), mainWindow, dict);
            try {
                mainWindow.createMainPanel();
                mainWindow.showWindow();
                mainWindow.init();
            } catch (Exception ex) {
                // It is better to exit than to let run a zombie process
                LOGGER.error("Cannot start GUI", ex);//$NON-NLS-1$
                System.exit(-1);
            }
            MainWindowListener listener = BundlePreferences.getService(bundleContext, MainWindowListener.class);
            if (listener != null) {
                listener.setMainWindow(mainWindow);
            }
        });
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        // Let osgi services doing their job
    }

}
