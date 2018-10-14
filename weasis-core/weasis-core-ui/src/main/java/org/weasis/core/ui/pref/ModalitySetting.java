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
package org.weasis.core.ui.pref;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.gui.util.*;
import org.weasis.core.ui.Messages;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.MeasureToolBar;
import org.weasis.core.ui.editor.image.ViewCanvas;
import org.weasis.core.ui.editor.image.ViewerPlugin;
import org.weasis.core.ui.model.GraphicModel;
import org.weasis.core.ui.model.utils.bean.Measurement;
import org.weasis.core.ui.pref.modality.ModalityView;
import org.weasis.dicom.codec.display.Modality;

import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class ModalitySetting extends AbstractItemDialogPage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModalitySetting.class);
    public static final String pageName = Messages.getString("ModalitySetting.title"); //$NON-NLS-1$

    public ModalitySetting() {
        super(pageName);
        setComponentPosition(20);
        setBorder(new EmptyBorder(15, 10, 10, 10));
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);

        addSubPage(new ModalityView(Modality.DEFAULT));
        addSubPage(new ModalityView(Modality.CT));
        addSubPage(new ModalityView(Modality.US));
        addSubPage(new ModalityView(Modality.DX));
        addSubPage(new ModalityView(Modality.CR));
    }

    @Override
    public void closeAdditionalWindow() {
        for (PageProps subpage : getSubPages()) {
            subpage.closeAdditionalWindow();
        }
        synchronized (org.weasis.core.ui.docking.UIManager.VIEWER_PLUGINS) {
            for (int i = org.weasis.core.ui.docking.UIManager.VIEWER_PLUGINS.size() - 1; i >= 0; i--) {
                ViewerPlugin<?> p = org.weasis.core.ui.docking.UIManager.VIEWER_PLUGINS.get(i);
                if (p instanceof ImageViewerPlugin) {
                    for (Object v : ((ImageViewerPlugin<?>) p).getImagePanels()) {
                        if (v instanceof ViewCanvas) {
                            ViewCanvas<?> view = (ViewCanvas<?>) v;
                            GraphicModel graphicList = view.getGraphicManager();
                            graphicList.updateLabels(true, view);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void resetoDefaultValues() {
        MeasureToolBar.measureGraphicList.forEach(g -> {
            List<Measurement> list = g.getMeasurementList();
            Optional.ofNullable(list).ifPresent(l -> l.forEach(m -> m.resetToGraphicLabelValue()));
        });
    }

}
