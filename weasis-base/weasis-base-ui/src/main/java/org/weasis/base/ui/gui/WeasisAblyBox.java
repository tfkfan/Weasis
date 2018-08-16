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
package org.weasis.base.ui.gui;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.Message;

import org.weasis.base.ui.Messages;
import org.weasis.core.api.gui.util.AppProperties;
import org.weasis.core.api.gui.util.JMVUtils;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.ResourceUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

public class WeasisAblyBox extends JDialog implements ActionListener {

    private final JPanel jpanelRoot = new JPanel();
    private final JPanel jPanelClose = new JPanel();
    private final JButton jButtonclose = new JButton();
    private final BorderLayout borderLayout1 = new BorderLayout();
    private final JPanel jPanelAbly = new JPanel();
    private final FlowLayout flowLayout1 = new FlowLayout();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();

    private final JPanel jPanel3 = new JPanel();
    private final BorderLayout borderLayout3 = new BorderLayout();
    private final JTextPane jTextPane1 = new JTextPane();


    public WeasisAblyBox(Frame owner) {
        super(owner, "Ably test", true); //$NON-NLS-1$
        init();
        pack();
        try {
            initAbly();
        } catch (AblyException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setModal(true);

        setSize(700, 400);

        jpanelRoot.setLayout(borderLayout1);
        jPanelClose.setLayout(flowLayout1);
        flowLayout1.setAlignment(FlowLayout.RIGHT);
        flowLayout1.setHgap(15);
        flowLayout1.setVgap(10);

        jButtonclose.setText(Messages.getString("WeasisAboutBox.close")); //$NON-NLS-1$

        jButtonclose.addActionListener(this);

        jPanelAbly.setLayout(gridBagLayout1);

        jTextPane1.setEditorKit(JMVUtils.buildHTMLEditorKit(jTextPane1));
        jTextPane1.setContentType("text/html"); //$NON-NLS-1$
        jTextPane1.setEditable(false);

        jPanel3.add(jTextPane1);

        jPanel3.setLayout(borderLayout3);
        jPanelClose.add(jButtonclose, null);

        jpanelRoot.add(jPanelClose, BorderLayout.SOUTH);
        jpanelRoot.add(jPanel3, BorderLayout.CENTER);

        this.getContentPane().add(jpanelRoot, null);
    }

    protected void initAbly() throws AblyException {
        AblyRealtime ably = new AblyRealtime("Rzgycw.3FHJ-Q:75oKq_HwuSSeV_Rn");
        Channel channel = ably.channels.get("test");

        /* Publish a message to the test channel */
        channel.publish("test", "hello");

        Channel.MessageListener listener = message -> {
            jTextPane1.setText(message.data.toString());
        };

        channel.subscribe("greeting2", listener);

    }

    // Overridden so we can exit when window is closed
    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    void cancel() {
        dispose();
    }

    // Close the dialog on a button event
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jButtonclose) {
            cancel();
        }
    }

}
