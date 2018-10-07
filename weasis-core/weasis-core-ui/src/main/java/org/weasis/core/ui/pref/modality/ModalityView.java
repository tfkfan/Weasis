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
package org.weasis.core.ui.pref.modality;

import org.weasis.core.api.gui.util.*;
import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.ui.Messages;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.SynchView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.weasis.core.ui.editor.image.ViewerToolBar.buildLayoutButton;
import static org.weasis.core.ui.editor.image.ViewerToolBar.buildSynchButton;
import static org.weasis.core.ui.editor.image.ViewerToolBar.buildSynchIcon;

@SuppressWarnings("serial")
public class ModalityView extends AbstractItemDialogPage {

    protected final Modality modality;

    private final DropDownButton synchButton;
    private final DropDownButton layoutButton;

    public static final List<SynchView> DEFAULT_SYNCH_LIST =
            Arrays.asList(SynchView.NONE, SynchView.DEFAULT_STACK, SynchView.DEFAULT_TILE, SynchView.DEFAULT_TILE_MULTIPLE);

    public static final List<GridBagLayoutModel> DEFAULT_LAYOUT_LIST =
            Arrays.asList(ImageViewerPlugin.VIEWS_1x1, ImageViewerPlugin.VIEWS_1x2, ImageViewerPlugin.VIEWS_2x1,
                    ImageViewerPlugin.VIEWS_2x2_f2, ImageViewerPlugin.VIEWS_2_f1x2, ImageViewerPlugin.VIEWS_2x1_r1xc2_dump,
                    ImageViewerPlugin.VIEWS_2x2,
                    ImageViewerPlugin.buildGridBagLayoutModel(1, 3, ImageViewerPlugin.view2dClass.getName()),
                    ImageViewerPlugin.buildGridBagLayoutModel(1, 4, ImageViewerPlugin.view2dClass.getName()),
                    ImageViewerPlugin.buildGridBagLayoutModel(2, 4, ImageViewerPlugin.view2dClass.getName()),
                    ImageViewerPlugin.buildGridBagLayoutModel(2, 6, ImageViewerPlugin.view2dClass.getName()),
                    ImageViewerPlugin.buildGridBagLayoutModel(2, 8, ImageViewerPlugin.view2dClass.getName()));

    public ModalityView(Modality modality) {
        super(modality.getTitle()); //$NON-NLS-1$
        this.modality = modality;
        setComponentPosition(10);
        setBorder(new EmptyBorder(15, 10, 10, 10));

        this.layoutButton = buildLayoutButton(createLayoutAction());
        this.synchButton = buildSynchButton(createSynchAction(), new SynchGroupMenu());

        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, getTitle(), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JPanel layoutPanel = new JPanel();
        layoutPanel.setBorder(new TitledBorder(null, Messages.getString("ModalitySettings.layout"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        layoutPanel.setLayout(new BoxLayout(layoutPanel, BoxLayout.X_AXIS));
        layoutPanel.add(layoutButton);

        panel.add(layoutPanel);

        JPanel synchPanel = new JPanel();
        synchPanel.setBorder(new TitledBorder(null, Messages.getString("ModalitySettings.synch"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        synchPanel.setLayout(new BoxLayout(synchPanel, BoxLayout.X_AXIS));
        synchPanel.add(synchButton);

        panel.add(synchPanel);

        JPanel panel2 = new JPanel();
        FlowLayout flowLayout1 = (FlowLayout) panel2.getLayout();
        flowLayout1.setHgap(10);
        flowLayout1.setAlignment(FlowLayout.RIGHT);
        flowLayout1.setVgap(7);
        add(panel2, BorderLayout.SOUTH);

        JButton btnNewButton = new JButton(Messages.getString("restore.values")); //$NON-NLS-1$
        panel2.add(btnNewButton);
        btnNewButton.addActionListener(e -> resetoDefaultValues());
    }

    private ComboItemListener<?> createSynchAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newSynchAction(DEFAULT_SYNCH_LIST.toArray(
                new SynchView[DEFAULT_SYNCH_LIST.size()]), object -> {

        });
        res.enableAction(true);
        return res;
    }

    private ComboItemListener<?> createLayoutAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newLayoutAction(DEFAULT_LAYOUT_LIST
                .toArray(new GridBagLayoutModel[DEFAULT_LAYOUT_LIST.size()]), object -> {

        });
        res.enableAction(true);
        return res;
    }

    @Override
    public void closeAdditionalWindow() {
        // Do nothing
    }

    @Override
    public void resetoDefaultValues() {
        //Arrays.stream(ImageStatistics.ALL_MEASUREMENTS).forEach(m -> m.resetToGraphicLabelValue());
    }

    class SynchGroupMenu extends GroupRadioMenu<SynchView> {

        @Override
        public void contentsChanged(ListDataEvent e) {
            super.contentsChanged(e);
            changeButtonState();
        }

        public void changeButtonState() {
            Object sel = dataModel.getSelectedItem();
            if (sel instanceof SynchView && synchButton != null) {
                Icon icon = buildSynchIcon((SynchView) sel);
                synchButton.setIcon(icon);
                synchButton.setActionCommand(sel.toString());
            }
        }
    }
}
