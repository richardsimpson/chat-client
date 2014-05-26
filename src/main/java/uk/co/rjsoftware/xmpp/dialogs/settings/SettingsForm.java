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
package uk.co.rjsoftware.xmpp.dialogs.settings;

import uk.co.rjsoftware.xmpp.client.YaccProperties;
import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsForm extends JDialog {

    private final JLabel endpointLabel;
    private final JLabel authTokenLabel;
    private final JTextField endpointField;
    private final JTextField authTokenField;
    private final JButton saveButton;
    private final JButton cancelButton;

    private final YaccProperties yaccProperties;

    public SettingsForm(final YaccProperties yaccProperties) {
        super(null, "Settings", ModalityType.APPLICATION_MODAL);

        this.yaccProperties = yaccProperties;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 25, 2, 2);

        this.endpointLabel = new JLabel("HipChat API Endpoint:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        pane.add(this.endpointLabel, constraints);

        this.authTokenLabel = new JLabel("HipChat Auth Token:");
        constraints.gridx = 0;
        constraints.gridy = 1;
        pane.add(this.authTokenLabel, constraints);

        this.endpointField = new JTextField();
        this.endpointField.setPreferredSize(new Dimension(300, 20));
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        pane.add(this.endpointField, constraints);

        this.authTokenField = new JTextField();
        this.authTokenField.setPreferredSize(new Dimension(300, 20));
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 1;
        constraints.gridy = 1;
        pane.add(this.authTokenField, constraints);

        this.endpointField.setText(this.yaccProperties.getProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_ENDPOINT));
        this.authTokenField.setText(this.yaccProperties.getProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_AUTH_TOKEN));

        this.saveButton = new JButton("Save");
        constraints.gridx = 0;
        constraints.gridy = 2;
        pane.add(this.saveButton, constraints);

        this.saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveButtonPressed();
            }
        });

        this.cancelButton = new JButton("Cancel");
        constraints.gridx = 1;
        constraints.gridy = 2;
        pane.add(this.cancelButton, constraints);

        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButtonPressed();
            }
        });

        setPreferredSize(new Dimension(250, 200));
        setResizable(false);

        getRootPane().setDefaultButton(this.saveButton);

        pack();

        DialogUtils.centerDialog(this);
    }

    private void saveButtonPressed() {
        this.yaccProperties.setProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_ENDPOINT, this.endpointField.getText());
        this.yaccProperties.setProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_AUTH_TOKEN, this.authTokenField.getText());
        this.yaccProperties.store();
        setVisible(false);
    }

    private void cancelButtonPressed() {
        setVisible(false);
    }
}
