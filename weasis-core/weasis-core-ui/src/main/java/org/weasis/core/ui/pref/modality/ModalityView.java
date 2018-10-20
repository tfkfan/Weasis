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

import static org.weasis.core.ui.editor.image.ViewerToolBar.*;

@SuppressWarnings("serial")
public class ModalityView extends AbstractItemDialogPage {

    protected final Modality modality;

    private final DropDownButton synchButton;
    private final DropDownButton layoutButton;
    private final DropDownButton scrollButton;

    private final JButton jButtonApply;

    private ComboItemListener<?> synchListener;
    private ComboItemListener<?> layoutListener;
    private ComboItemListener<?> scrollSetListener;

    public final List<SynchView> DEFAULT_SYNCH_LIST;
    public final List<GridBagLayoutModel> DEFAULT_LAYOUT_LIST;
    public final List<Integer> DEFAULT_SCROLLSET_LIST;

    private final Hashtable<String, GridBagLayoutModel> layoutModels = ModelsUtils.createDefaultLayoutModels();
    private final Hashtable<String, SynchView> synchViews = ModelsUtils.createDefaultSynchViews();

    public static final String systemPref = "weasis.modality.%s";
    public static final String systemSynchPref = systemPref.concat(".synch");
    public static final String systemLayoutPref = systemPref.concat(".layout");
    public static final String systemScrollSetPref = systemPref.concat(".scrollset");

    public ModalityView(Modality modality) {
        super(modality.name()); //$NON-NLS-1$
        this.modality = modality;
        setComponentPosition(10);
        setBorder(new EmptyBorder(15, 10, 10, 10));

        DEFAULT_LAYOUT_LIST = new ArrayList<>(layoutModels.values());
        DEFAULT_SYNCH_LIST = new ArrayList<>(synchViews.values());
        DEFAULT_SCROLLSET_LIST = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        this.layoutButton = buildLayoutButton(createLayoutAction());
        this.synchButton = buildSynchButton(createSynchAction(), new SynchGroupMenu());
        this.scrollButton = buildScrollSetButton(createScrollSetAction(), new GroupRadioMenu<Integer>());
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

        JPanel scrollPanel = new JPanel();
        scrollPanel.setBorder(new TitledBorder(null, Messages.getString("ModalitySettings.synch"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        scrollPanel.setLayout(new BoxLayout(scrollPanel, BoxLayout.X_AXIS));
        scrollPanel.add(scrollButton);

        panel.add(scrollPanel);

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

    public void initButtonsFromPrefs() {
        final String synchPref = String.format(systemSynchPref, modality.name());
        final String layoutPref = String.format(systemLayoutPref, modality.name());
        final String scrollPref = String.format(systemScrollSetPref, modality.name());

        final String synchPrefValue = BundleTools.SYSTEM_PREFERENCES.getProperty(synchPref);
        final String layoutPrefValue = BundleTools.SYSTEM_PREFERENCES.getProperty(layoutPref);
        final String scrollPrefValue = BundleTools.SYSTEM_PREFERENCES.getProperty(scrollPref);

        if (synchPrefValue != null) {
            SynchView synchView = synchViews.get(synchPrefValue);
            synchListener.setSelectedItem(synchView);
        }

        if (layoutPrefValue != null) {
            GridBagLayoutModel model = layoutModels.get(layoutPrefValue);
            layoutListener.setSelectedItem(model);
        }

        if (scrollPrefValue != null) {
            scrollSetListener.setSelectedItem(Integer.valueOf(scrollPrefValue));
        }
    }

    private void apply() {
        final String synchPref = String.format(systemSynchPref, modality.name());
        final String layoutPref = String.format(systemLayoutPref, modality.name());
        final String scrollSetPref = String.format(systemScrollSetPref, modality.name());

        BundleTools.SYSTEM_PREFERENCES.put(synchPref, ((SynchView) synchListener.getSelectedItem()).getName()); //$NON-NLS-1$
        BundleTools.SYSTEM_PREFERENCES.put(layoutPref, ((GridBagLayoutModel) layoutListener.getSelectedItem()).getUIName()); //$NON-NLS-1$
        BundleTools.SYSTEM_PREFERENCES.put(scrollSetPref, ((Integer) scrollSetListener.getSelectedItem()).toString());
    }

    private ComboItemListener<?> createSynchAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newSynchAction(DEFAULT_SYNCH_LIST.toArray(
                new SynchView[DEFAULT_SYNCH_LIST.size()]), object -> onSynchChange((SynchView) object));
        res.enableAction(true);
        synchListener = res;
        return res;
    }

    private ComboItemListener<?> createLayoutAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newLayoutAction(DEFAULT_LAYOUT_LIST
                .toArray(new GridBagLayoutModel[DEFAULT_LAYOUT_LIST.size()]), object -> onLayoutChange((GridBagLayoutModel) object));
        res.enableAction(true);
        layoutListener = res;
        return res;
    }

    private ComboItemListener<?> createScrollSetAction() {
        ComboItemListener<?> res = ImageViewerEventManager.newScrollSetAction(new Integer[]{1, 2, 3, 4, 5, 6}, object -> {
        });
        res.enableAction(true);
        scrollSetListener = res;
        return res;
    }

    private void onSynchChange(SynchView selectedItem) {
    }

    private void onLayoutChange(GridBagLayoutModel selectedItem) {
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
