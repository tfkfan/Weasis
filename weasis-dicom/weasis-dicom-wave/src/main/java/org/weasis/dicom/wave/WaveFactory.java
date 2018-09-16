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
package org.weasis.dicom.wave;

import java.util.Map;

import javax.swing.Icon;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.weasis.core.api.explorer.DataExplorerView;
import org.weasis.core.api.explorer.model.DataExplorerModel;
import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.api.media.MimeInspector;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewer;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.dicom.explorer.DicomExplorer;
import org.weasis.dicom.explorer.DicomModel;

@org.osgi.service.component.annotations.Component(service = SeriesViewerFactory.class, immediate = false)
public class WaveFactory implements SeriesViewerFactory {

    public static final String NAME = "DICOM Waveform"; //$NON-NLS-1$

    public WaveFactory() {
        super();
    }

    @Override
    public Icon getIcon() {
        return MimeInspector.ecgIcon;
    }

    @Override
    public String getUIName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return NAME;
    }

    @Override
    public SeriesViewer<? extends MediaElement> createSeriesViewer(Map<String, Object> properties) {
        GridBagLayoutModel model = WaveContainer.VIEWS_1x1;
        String uid = null;
        if (properties != null) {
            Object obj = properties.get(org.weasis.core.api.image.GridBagLayoutModel.class.getName());
            if (obj instanceof GridBagLayoutModel) {
                model = (GridBagLayoutModel) obj;
            }
            // Set UID
            Object val = properties.get(ViewerPluginBuilder.UID);
            if (val instanceof String) {
                uid = (String) val;
            }
        }

        WaveContainer instance = new WaveContainer(model, uid);
        if (properties != null) {
            Object obj = properties.get(DataExplorerModel.class.getName());
            if (obj instanceof DicomModel) {
                // Register the PropertyChangeListener
                DicomModel m = (DicomModel) obj;
                m.addPropertyChangeListener(instance);
            }
        }
        return instance;
    }

    public static void closeSeriesViewer(WaveContainer container) {
        // Unregister the PropertyChangeListener
        DataExplorerView dicomView = UIManager.getExplorerplugin(DicomExplorer.NAME);
        if (dicomView != null) {
            dicomView.getDataExplorerModel().removePropertyChangeListener(container);
        }
    }

    @Override
    public boolean canReadMimeType(String mimeType) {
        return WaveElementFactory.SERIES_WAVEFORM_MIMETYPE.equals(mimeType);
    }

    @Override
    public boolean isViewerCreatedByThisFactory(SeriesViewer<? extends MediaElement> viewer) {
        return viewer instanceof WaveContainer;
    }

    @Override
    public int getLevel() {
        return 35;
    }

    @Override
    public boolean canAddSeries() {
        return false;
    }

    @Override
    public boolean canExternalizeSeries() {
        return true;
    }

    // ================================================================================
    // OSGI service implementation
    // ================================================================================
    @Deactivate
    protected void deactivate(ComponentContext context) {
        UIManager.closeSeriesViewerType(WaveContainer.class);
    }
}
