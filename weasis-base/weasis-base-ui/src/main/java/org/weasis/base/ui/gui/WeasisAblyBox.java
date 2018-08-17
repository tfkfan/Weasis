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

import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;

import org.weasis.base.ui.Messages;
import org.weasis.core.api.networking.AblyService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

public class WeasisAblyBox extends JDialog implements ActionListener {

    private final JPanel jpanelRoot = new JPanel();
    private final JButton jButtonclose = new JButton();
    private final JTextArea jTextArea = new JTextArea();
    private final JTextField jTextInput = new JTextField();
    private final JButton jButtonsend = new JButton();
    private final JPanel jPanelBtns = new JPanel();
    private final JPanel jPanelText = new JPanel();
    private final FlowLayout flowLayout1 = new FlowLayout();
    private final BorderLayout borderLayout1 = new BorderLayout();

    private final AblyService ablyService = new AblyService();
    private Channel channel;

    public WeasisAblyBox(Frame owner) {
        super(owner, Messages.getString("WeasisAblyBox.title"), true); //$NON-NLS-1$
        init();
        pack();
        try {
            initAbly();
        } catch (AblyException e) {
            e.printStackTrace();
        } catch (Exception e) {
            jTextArea.setText(e.getMessage());
        }
    }

    private void init() {
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setModal(true);

        jpanelRoot.setLayout(borderLayout1);
        jPanelBtns.setLayout(flowLayout1);
        flowLayout1.setAlignment(FlowLayout.RIGHT);
        flowLayout1.setHgap(15);
        flowLayout1.setVgap(10);

        jButtonclose.setText(Messages.getString("WeasisAblyBox.close")); //$NON-NLS-1$
        jButtonclose.addActionListener(this);

        jTextInput.setPreferredSize(new Dimension(200, 28));
        jTextArea.setPreferredSize(new Dimension(200, 200));

        jButtonsend.setText(Messages.getString("WeasisAblyBox.send"));
        jButtonsend.addActionListener(this);

        jPanelBtns.add(jButtonsend, null);
        jPanelBtns.add(jButtonclose, null);

        jPanelText.setBorder(new EmptyBorder(10, 10, 10, 10));
        jPanelText.setLayout(new BorderLayout());
        jPanelText.add(jTextArea, BorderLayout.NORTH);
        jPanelText.add(jTextInput, BorderLayout.SOUTH);

        jpanelRoot.add(jPanelBtns, BorderLayout.SOUTH);
        jpanelRoot.add(jPanelText, BorderLayout.CENTER);

        this.getContentPane().setPreferredSize(new Dimension(600, 600));
        this.getContentPane().add(jpanelRoot, null);
    }

    protected void initAbly() throws AblyException {
        ablyService.init();
        ablyService.setChannelName("test");

        /*channel = ablyService.getAblyChannel();
        channel.subscribe( message -> {
            jTextArea.append(message.data.toString());
        });*/
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jButtonclose) {
            cancel();
        } else if (e.getSource() == jButtonsend) {
            try {
                ablyService.publish("test", jTextInput.getText());
                jTextInput.setText("");
            } catch (AblyException ex) {
                ex.printStackTrace();
            }
        }
    }

}
