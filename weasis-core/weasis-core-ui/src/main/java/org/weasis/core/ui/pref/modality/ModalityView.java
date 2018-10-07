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
import org.weasis.core.ui.Messages;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.core.ui.editor.image.SynchView;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static org.weasis.core.ui.editor.image.ViewerToolBar.buildSynchIcon;

@SuppressWarnings("serial")
public class ModalityView extends AbstractItemDialogPage {

    protected final Modality modality;

    private final DropDownButton synchButton;

    public static final List<SynchView> DEFAULT_SYNCH_LIST =
            Arrays.asList(SynchView.NONE, SynchView.DEFAULT_STACK, SynchView.DEFAULT_TILE, SynchView.DEFAULT_TILE_MULTIPLE);

    public ModalityView(Modality modality) {
        super(modality.getTitle()); //$NON-NLS-1$
        this.modality = modality;
        setComponentPosition(10);
        setBorder(new EmptyBorder(15, 10, 10, 10));

        this.synchButton = buildSynchButton();
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, getTitle(), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(panel, BorderLayout.CENTER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(synchButton);

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

    private DropDownButton buildSynchButton() {
        GroupPopup menu = null;
        ComboItemListener synch = createSynchAction();
        SynchView synchView = SynchView.DEFAULT_STACK;

        Object sel = synch.getSelectedItem();
        if (sel instanceof SynchView)
            synchView = (SynchView) sel;

        menu = new SynchGroupMenu();
        synch.registerActionState(menu);

        final DropDownButton button = new DropDownButton(ActionW.SYNCH.cmd(), buildSynchIcon(synchView), menu) {
            @Override
            protected JPopupMenu getPopupMenu() {
                JPopupMenu menu = (getMenuModel() == null) ? new JPopupMenu() : getMenuModel().createJPopupMenu();
                menu.setInvoker(this);
                return menu;
            }

        };
        button.setToolTipText(Messages.getString("ViewerToolBar.synch")); //$NON-NLS-1$
        if (synch != null) {
            synch.enableAction(true);
            synch.registerActionState(button);
        }
        return button;
    }

    private ComboItemListener<?> createSynchAction() {
        return ImageViewerEventManager.newSynchAction(DEFAULT_SYNCH_LIST.toArray(
                new SynchView[DEFAULT_SYNCH_LIST.size()]),
                object ->{});
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
