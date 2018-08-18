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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

public class WeasisAblyBox extends JDialog implements ActionListener, Channel.MessageListener {

    private final JPanel jpanelRoot = new JPanel();
    private final JButton jButtonclose = new JButton();
    private final JTextArea jTextArea = new JTextArea();
    private final JTextField jTextInput = new JTextField();
    private final JTextField userTextInput = new JTextField();
    private final JButton jButtonsend = new JButton();
    private final JButton jButtonusername = new JButton();
    private final JPanel jPanelBtns = new JPanel();
    private final JPanel jPanelText = new JPanel();
    private final FlowLayout flowLayout1 = new FlowLayout();
    private final BorderLayout borderLayout1 = new BorderLayout();
    private final JPanel userInputPanel = new JPanel();
    private final JPanel textInputPanel = new JPanel();
    private final JPanel jPanelInputs = new JPanel();

    private final JLabel jLabel = new JLabel();
    private final JLabel jLabel2 = new JLabel();

    private final static String CHANNEL_NAME = "test";
    private final static String API_KEY = "Rzgycw.3FHJ-Q:75oKq_HwuSSeV_Rn";

    private String userName = null;
    private AblyRealtime ablyRealtime;
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

        jPanelInputs.setLayout(new BoxLayout(jPanelInputs, BoxLayout.Y_AXIS));

        jButtonclose.setText(Messages.getString("WeasisAblyBox.close")); //$NON-NLS-1$
        jButtonclose.addActionListener(this);

        jTextInput.setPreferredSize(new Dimension(200, 28));
        userTextInput.setPreferredSize(new Dimension(200, 28));
        jTextArea.setPreferredSize(new Dimension(200, 200));
        jTextArea.setEnabled(false);

        jButtonsend.setText(Messages.getString("WeasisAblyBox.send"));
        jButtonsend.addActionListener(this);

        jButtonusername.setText(Messages.getString("WeasisAblyBox.login"));
        jButtonusername.addActionListener(this);

        jPanelBtns.add(jButtonsend, null);
        jPanelBtns.add(jButtonclose, null);

        jLabel.setText(Messages.getString("WeasisAblyBox.username"));
        jLabel2.setText("-");

        userInputPanel.add(jLabel);
        userInputPanel.add(userTextInput);
        userInputPanel.add(jButtonusername);

        textInputPanel.add(jTextInput);

        jPanelInputs.add(jLabel2);
        jPanelInputs.add(userInputPanel);

        jPanelText.setBorder(new EmptyBorder(50, 50, 50, 50));
        jPanelText.setLayout(new BorderLayout());
        jPanelText.add(jTextArea, BorderLayout.NORTH);
        jPanelText.add(jPanelInputs, BorderLayout.CENTER);
        jPanelText.add(textInputPanel, BorderLayout.SOUTH);

        jpanelRoot.add(jPanelBtns, BorderLayout.SOUTH);
        jpanelRoot.add(jPanelText, BorderLayout.CENTER);

        this.getContentPane().setPreferredSize(new Dimension(600, 500));
        this.getContentPane().add(jpanelRoot, null);
    }

    protected void initAbly() throws AblyException {
        ablyRealtime = new AblyRealtime(API_KEY);

        channel = ablyRealtime.channels.get(CHANNEL_NAME);
        channel.subscribe(this);
    }

    @Override
    public void onMessage(Message message) {
        jTextArea.append(String.format("\n %s : %s", message.name, message.data.toString()));
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
                if (userName != null) {
                    channel.publish(userName, jTextInput.getText());
                    jTextInput.setText("");
                }
            } catch (AblyException ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == jButtonusername) {
            userName = userTextInput.getText();
            userTextInput.setText("");

            jLabel2.setText("You're signed in as: " + userName);
        }
    }
}
