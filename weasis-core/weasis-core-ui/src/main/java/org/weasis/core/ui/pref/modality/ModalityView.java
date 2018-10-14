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
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.ui.Messages;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.SynchView;
import org.weasis.core.ui.util.ModelsUtils;
import org.weasis.dicom.codec.display.Modality;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import static org.weasis.core.ui.editor.image.ViewerToolBar.buildLayoutButton;
import static org.weasis.core.ui.editor.image.ViewerToolBar.buildSynchButton;
import static org.weasis.core.ui.editor.image.ViewerToolBar.buildSynchIcon;

@SuppressWarnings("serial")
public class ModalityView extends AbstractItemDialogPage {

    protected final Modality modality;

    private final DropDownButton synchButton;
    private final DropDownButton layoutButton;
    private final JButton jButtonApply;

    private ComboItemListener<?> synchListener;
    private ComboItemListener<?> layoutListener;

    public final List<SynchView> DEFAULT_SYNCH_LIST;
    public final List<GridBagLayoutModel> DEFAULT_LAYOUT_LIST;

    private final Hashtable<String, GridBagLayoutModel> layoutModels = ModelsUtils.createDefaultLayoutModels();
    private final Hashtable<String, SynchView> synchViews = ModelsUtils.createDefaultSynchViews();

    private static final String systemPref = "weasis.modality.%s";
    private static final String systemSynchPref = systemPref.concat(".synch");
    private static final String systemLayoutPref = systemPref.concat(".layout");

    public ModalityView(Modality modality) {
        super(modality.getDescription()); //$NON-NLS-1$
        this.modality = modality;
        setComponentPosition(10);
        setBorder(new EmptyBorder(15, 10, 10, 10));

        DEFAULT_LAYOUT_LIST = new ArrayList<>(layoutModels.values());
        DEFAULT_SYNCH_LIST = new ArrayList<>(synchViews.values());

        this.layoutButton = buildLayoutButton(createLayoutAction());
        this.synchButton = buildSynchButton(createSynchAction(), new SynchGroupMenu());
        this.jButtonApply = new JButton();

        init();
        initButtonsFromPrefs();
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

        jButtonApply.setText(Messages.getString("LabelPrefView.apply")); //$NON-NLS-1$
        jButtonApply.addActionListener(e -> apply());

        panel2.add(jButtonApply);
    }

    public void initButtonsFromPrefs(){
        final String synchPref = String.format(systemSynchPref, modality.getDescription());
        final String layoutPref = String.format(systemLayoutPref, modality.getDescription());

        final String synchPrefValue = BundleTools.SYSTEM_PREFERENCES.getProperty(synchPref);
        final String layoutPrefValue = BundleTools.SYSTEM_PREFERENCES.getProperty(layoutPref);

        if(synchPrefValue != null) {
            SynchView synchView = synchViews.get(synchPrefValue);
            synchListener.setSelectedItem(synchView);
        }

        if(layoutPrefValue != null){
            GridBagLayoutModel model = layoutModels.get(layoutPrefValue);
            layoutListener.setSelectedItem(model);
        }
    }

    private void apply(){
        //BundleTools.SYSTEM_PREFERENCES.putBooleanProperty("weasis.confirm.closing", chckbxConfirmClosing.isSelected()); //$NON-NLS-1$
    }

    private ComboItemListener<?> createSynchAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newSynchAction(DEFAULT_SYNCH_LIST.toArray(
                new SynchView[DEFAULT_SYNCH_LIST.size()]), object -> {

        });
        res.enableAction(true);
        synchListener = res;
        return res;
    }

    private ComboItemListener<?> createLayoutAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newLayoutAction(DEFAULT_LAYOUT_LIST
                .toArray(new GridBagLayoutModel[DEFAULT_LAYOUT_LIST.size()]), object -> {

        });
        res.enableAction(true);
        layoutListener = res;
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
