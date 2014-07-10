/*
 * Copyright (c) 2014, Richard Simpson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.co.rjsoftware.xmpp.dialogs.login;

import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class LoginForm extends JDialog {

    private final JTextArea loginMessageLabel;
    private final JLabel usernameLabel;
    private final JLabel passwordLabel;
    private final JTextField usernameField;
    private final JTextField passwordField;
    private final JButton usernameHelpButton;
    private final JButton loginButton;

    private final List<LoginListener> listeners = new ArrayList<LoginListener>();

    public LoginForm() {
        super(null, "Login", ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 25, 2, 2);

        this.usernameLabel = new JLabel("Username:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        pane.add(this.usernameLabel, constraints);

        this.passwordLabel = new JLabel("Password:");
        constraints.gridx = 0;
        constraints.gridy = 1;
        pane.add(this.passwordLabel, constraints);

        this.usernameField = new JTextField();
        this.usernameField.setPreferredSize(new Dimension(120, 20));
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        pane.add(this.usernameField, constraints);

        this.usernameHelpButton = new JButton("Get");
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        pane.add(this.usernameHelpButton, constraints);

        this.usernameHelpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogUtils.launchUrl("https://www.hipchat.com/account/xmpp");
            }
        });

        this.passwordField = new JPasswordField();
        this.passwordField.setPreferredSize(new Dimension(120, 20));
        constraints.gridx = 1;
        constraints.gridy = 1;
        pane.add(this.passwordField, constraints);

        this.loginButton = new JButton("Login");
        constraints.gridx = 1;
        constraints.gridy = 2;
        pane.add(this.loginButton, constraints);

        this.loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireLoginAttempt();
            }
        });

        this.loginMessageLabel = new JTextArea();
        this.loginMessageLabel.setEditable(false);
        this.loginMessageLabel.setLineWrap(true);
        this.loginMessageLabel.setWrapStyleWord(true);
        this.loginMessageLabel.setFont(this.loginButton.getFont());
        this.loginMessageLabel.setOpaque(false);
        constraints.insets = new Insets(2, 10, 2, 10);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        pane.add(this.loginMessageLabel, constraints);

        setPreferredSize(new Dimension(291, 200));
        setResizable(false);

        getRootPane().setDefaultButton(this.loginButton);

        pack();

        DialogUtils.centerDialog(this);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            this.passwordField.setText("");
        }
        super.setVisible(b);
    }

    private void fireLoginAttempt() {
        for (LoginListener listener : this.listeners) {
            listener.loginAttempt(this.usernameField.getText(), this.passwordField.getText());
        }
    }

    public void addLoginListener(final LoginListener listener) {
        this.listeners.add(listener);
    }

    public void setLoginMessage(String loginMessage) {
        this.loginMessageLabel.setText(loginMessage);
    }
}
