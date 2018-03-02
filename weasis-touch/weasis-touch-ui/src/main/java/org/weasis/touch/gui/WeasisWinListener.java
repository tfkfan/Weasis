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

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.concurrent.Executors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.ui.editor.FileModel;
import org.weasis.core.ui.editor.SeriesViewer;
import org.weasis.dicom.codec.TagD;
import org.weasis.dicom.codec.TagD.Level;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.ExplorerTask;
import org.weasis.dicom.explorer.LoadLocalDicom;
import org.weasis.touch.internal.MainWindowListener;
import org.weasis.touch.internal.OsgiApp;

import javafx.application.Application;
import javafx.stage.Stage;

@org.osgi.service.component.annotations.Component(service = MainWindowListener.class, immediate = true)
public class WeasisWinListener implements MainWindowListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeasisWinListener.class);
    public static final FileModel DefaultDataModel = new FileModel();
    
    @org.osgi.service.component.annotations.Reference
    private DicomModel model;

    private volatile WeasisWin mainWindow;

    @Override
    public void setMainWindow(WeasisWin mainWindow) {
        this.mainWindow = mainWindow;
        if (model == null) {
            this.model = new DicomModel();
            model.addPropertyChangeListener(this);
            // TMP for debug
            loadLocalSample();
        }
    }

    @Override
    public DicomModel getModel() {
        return model;
    }

    @Override
    public WeasisWin getMainWindow() {
        return mainWindow;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (mainWindow == null) {
            return;
        }
        // Get only event from the dicom model and execute in FX thread
        if (evt instanceof ObservableEvent) {
            GuiExecutor.executeFX(() -> propertyChange((ObservableEvent) evt));
        }
    }

    private void propertyChange(ObservableEvent event) {
        ObservableEvent.BasicAction action = event.getActionCommand();
        Object newVal = event.getNewValue();
        ThumbnailViewerController thController = mainWindow.getThumbnailViewerController();
        if (model.equals(event.getSource())) {
            if (ObservableEvent.BasicAction.SELECT.equals(action)) {
                if (newVal instanceof Series) {
                    Series dcm = (Series) newVal;
                    MediaSeriesGroup patient = model.getParent(dcm, DicomModel.patient);
                    if (!thController.isSelectedPatient(patient)) {
                        thController.setSelectedPatient(patient);
                    }

                }
            } else if (ObservableEvent.BasicAction.ADD.equals(action)) {
                if (newVal instanceof Series) {
                    thController.addDicomSeries((Series) newVal, model);
                }
            } else if (ObservableEvent.BasicAction.REMOVE.equals(action)) {
                if (newVal instanceof MediaSeriesGroup) {
                    MediaSeriesGroup group = (MediaSeriesGroup) newVal;
                    // Patient Group
                    if (TagD.getUID(Level.PATIENT).equals(group.getTagID())) {
                        thController.removePatientPane(group);
                    }
                    // Study Group
                    else if (TagD.getUID(Level.STUDY).equals(group.getTagID())) {
                        thController.removeStudy(group, model);

                    }
                    // Series Group
                    else if (TagD.getUID(Level.SERIES).equals(group.getTagID())) {
                        thController.removeSeries(group, model);

                    }
                }
            }
            // update
            else if (ObservableEvent.BasicAction.UPDATE.equals(action)) {
                if (newVal instanceof Series) {
                    Series series = (Series) newVal;
                    Integer splitNb = (Integer) series.getTagValue(TagW.SplitSeriesNumber);
                    if (splitNb != null) {
                        thController.updateSplitSeries(series, model);
                    }
                }
                // else if (newVal instanceof KOSpecialElement) {
                // Object item = modelPatient.getSelectedItem();
                // if (item instanceof MediaSeriesGroupNode) {
                // koOpen.setVisible(
                // DicomModel.hasSpecialElements((MediaSeriesGroup) item, KOSpecialElement.class));
                // }
                // }
            } else if (ObservableEvent.BasicAction.LOADING_START.equals(action)) {
                if (newVal instanceof ExplorerTask) {
                    // addTaskToGlobalProgression((ExplorerTask<?, ?>) newVal);
                }
            } else if (ObservableEvent.BasicAction.LOADING_STOP.equals(action)
                || ObservableEvent.BasicAction.LOADING_CANCEL.equals(action)) {
                if (newVal instanceof ExplorerTask) {
                    // removeTaskToGlobalProgression((ExplorerTask<?, ?>) newVal);
                }
                // Object item = modelPatient.getSelectedItem();
                // if (item instanceof MediaSeriesGroupNode) {
                // koOpen.setVisible(
                // DicomModel.hasSpecialElements((MediaSeriesGroup) item, KOSpecialElement.class));
                // }
            }
        } else if (event.getSource() instanceof SeriesViewer) {
            if (ObservableEvent.BasicAction.SELECT.equals(action)) {
                if (newVal instanceof MediaSeriesGroup) {
                    MediaSeriesGroup patient = (MediaSeriesGroup) newVal;
                    if (!thController.isSelectedPatient(patient)) {
                        thController.setSelectedPatient(patient);
                        // focus get back to viewer
                        // if (evt.getSource() instanceof ViewerPlugin) {
                        // ((ViewerPlugin) evt.getSource()).requestFocusInWindow();
                        // }
                    }
                }
            }
        }
    }

    private void loadLocalSample() {
        String demoDir = System.getProperty("dicom.demo.dir");
        if (StringUtil.hasText(demoDir)) {
            LoadLocalDicom dicom = new LoadLocalDicom(new File[] { new File(demoDir) }, true, model);
            DicomModel.LOADING_EXECUTOR.execute(dicom);
        }
    }

    // ================================================================================
    // OSGI service implementation
    // ================================================================================

    @Activate
    protected void activate(ComponentContext context) {
        LOGGER.info("Activate the main window PropertyChangeListener"); //$NON-NLS-1$
        // Register default model
        DefaultDataModel.addPropertyChangeListener(this);

        final Stage splash = OsgiApp.getSplashScreen(context.getBundleContext());
        if (splash == null) {
            Executors.defaultThreadFactory().newThread(() -> {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                Application.launch(OsgiApp.class);
            }).start();
            try {
                // Let ui to start before executing the commands
                Thread.sleep(1000);
            } catch (InterruptedException et) {
                // DO nothing
            }
        } else {
            GuiExecutor.runFxAndWait(() -> {
                mainWindow = OsgiApp.showApp(splash);
                if (mainWindow != null) {
                    model.addPropertyChangeListener(this);
                    // TMP for debug
                    loadLocalSample();
                }
            });
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // UnRegister default model
        DefaultDataModel.removePropertyChangeListener(this);
        LOGGER.info("Deactivate the main window PropertyChangeListener"); //$NON-NLS-1$
    }
}
