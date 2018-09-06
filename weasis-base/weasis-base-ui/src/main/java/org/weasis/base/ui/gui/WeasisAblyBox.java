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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.Message;
import org.weasis.base.ui.Messages;
import org.weasis.core.api.telnet.CustomTelnetExecutor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Properties;

public class WeasisAblyBox extends JDialog implements ActionListener, Channel.MessageListener, KeyListener {
    private final static String configs = "/config.properties";

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

    private Properties properties = new Properties();
    private String userName = null;
    private AblyRealtime ablyRealtime;
    private Channel channel;
    private CustomTelnetExecutor telnet;
    private JsonParser jsonParser;

    public WeasisAblyBox(Frame owner) {
        super(owner, Messages.getString("WeasisAblyBox.title"), true); //$NON-NLS-1$
        try {
            init();
            pack();
            initFeatures();
        } catch (AblyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        properties.load(WeasisAblyBox.class.getResourceAsStream(configs));
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

        jTextInput.addKeyListener(this);
        jTextInput.setPreferredSize(new Dimension(200, 28));
        userTextInput.setPreferredSize(new Dimension(200, 28));

        jTextArea.setEnabled(false);
        jTextArea.setVisible(true);
        jTextArea.setColumns(20);
        jTextArea.setRows(10);

        DefaultCaret caret = (DefaultCaret) jTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

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


        jPanelText.add(new JScrollPane(jTextArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.NORTH);
        jPanelText.add(jPanelInputs, BorderLayout.CENTER);
        jPanelText.add(textInputPanel, BorderLayout.SOUTH);

        jpanelRoot.add(jPanelBtns, BorderLayout.SOUTH);
        jpanelRoot.add(jPanelText, BorderLayout.CENTER);

        this.getContentPane().setPreferredSize(new Dimension(600, 500));
        this.getContentPane().add(jpanelRoot, null);
    }

    protected void initFeatures() throws AblyException, IOException {
        ablyRealtime = new AblyRealtime(properties.getProperty("ably_api_key"));
        channel = ablyRealtime.channels.get(properties.getProperty("ably_channel"));
        channel.subscribe(this);

        telnet = new CustomTelnetExecutor(properties.getProperty("telnet_host"), Integer.valueOf(properties.getProperty("telnet_port")));
        jsonParser = new JsonParser();
    }

    @Override
    public void onMessage(Message message) {
        final String msgData = message.data.toString();
        try {
            final Object data = jsonParser.parse(msgData);
            if (data instanceof JsonObject) {
                final JsonObject json = (JsonObject) data;
                final String path = json.get("path").getAsString();
                final String command = String.format("dicom:get -l %s", path);
                
                jTextArea.append(String.format("\n %s : sending command from json: %s", message.name, command));

                telnet.connect();
                telnet.sendCommand(command);
                telnet.disconnect();
            } else if (data instanceof JsonPrimitive) {
                JsonPrimitive primitive = (JsonPrimitive) data;
                jTextArea.append(String.format("\n %s : %s", message.name, primitive.getAsString()));
            } else
                jTextArea.append(String.format("\n %s : %s", message.name, msgData));
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(null, e1.getMessage());
        }
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

    public void sendMessage() throws AblyException {
        if (userName != null) {
            channel.publish(userName, jTextInput.getText());
            jTextInput.setText("");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jButtonclose) {
            cancel();
        } else if (e.getSource() == jButtonsend) {
            try {
                sendMessage();
            } catch (AblyException ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == jButtonusername) {
            userName = userTextInput.getText();
            userTextInput.setText("");

            jLabel2.setText("You're signed in as: " + userName);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getSource() == jTextInput) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                try {
                    sendMessage();
                } catch (AblyException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
