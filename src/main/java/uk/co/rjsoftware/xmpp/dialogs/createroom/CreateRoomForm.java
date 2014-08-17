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
package uk.co.rjsoftware.xmpp.dialogs.createroom;

import uk.co.rjsoftware.xmpp.client.YaccProperties;
import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.RoomPrivacy;
import uk.co.rjsoftware.xmpp.model.hipchat.room.HipChatRoom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class CreateRoomForm extends JDialog {

    private final JLabel nameLabel;
    private final JLabel privacyLabel;
    private final JTextField nameField;
    private final JRadioButton publicRadioButton;
    private final JRadioButton privateRadioButton;
    private final ButtonGroup privacyButtonGroup;
    private final JButton createButton;
    private final JButton cancelButton;

    private final YaccProperties yaccProperties;

    private final java.util.List<NewRoomListener> listeners = new ArrayList<NewRoomListener>();

    public CreateRoomForm(final YaccProperties yaccProperties) {
        super(null, "Create Room", ModalityType.APPLICATION_MODAL);

        this.yaccProperties = yaccProperties;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());

        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 25, 2, 2);

        this.nameLabel = new JLabel("Room name:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        pane.add(this.nameLabel, constraints);

        this.privacyLabel = new JLabel("Privacy:");
        constraints.gridx = 0;
        constraints.gridy = 1;
        pane.add(this.privacyLabel, constraints);

        this.nameField = new JTextField();
        this.nameField.setPreferredSize(new Dimension(120, 20));
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        pane.add(this.nameField, constraints);

        this.publicRadioButton = new JRadioButton("public");
        constraints.gridx = 1;
        constraints.gridy = 1;
        pane.add(this.publicRadioButton, constraints);

        this.privateRadioButton = new JRadioButton("private");
        this.privateRadioButton.setSelected(true);
        constraints.gridx = 1;
        constraints.gridy = 2;
        pane.add(this.privateRadioButton, constraints);

        this.privacyButtonGroup = new ButtonGroup();
        this.privacyButtonGroup.add(this.publicRadioButton);
        this.privacyButtonGroup.add(this.privateRadioButton);

        this.createButton = new JButton("Create");
        constraints.gridx = 0;
        constraints.gridy = 3;
        pane.add(this.createButton, constraints);

        this.createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createRoom();
            }
        });

        this.cancelButton = new JButton("Cancel");
        constraints.gridx = 1;
        constraints.gridy = 3;
        pane.add(this.cancelButton, constraints);

        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelRoomCreation();
            }
        });

        setPreferredSize(new Dimension(250, 200));
        setResizable(false);

        getRootPane().setDefaultButton(this.createButton);

        pack();

        DialogUtils.centerDialog(this);
    }

    private void createRoom() {
        // get the RoomPrivacy to use
        RoomPrivacy privacy;
        if (this.publicRadioButton.isSelected()) {
            privacy = RoomPrivacy.PUBLIC;
        }
        else if (this.privateRadioButton.isSelected()) {
            privacy = RoomPrivacy.PRIVATE;
        }
        else {
            throw new RuntimeException("No privacy option selected");
        }

        HipChatRoom hipChatRoom = new HipChatRoom(this.yaccProperties);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            final Room room = hipChatRoom.createRoom(this.nameField.getText(), privacy);

            for (NewRoomListener listener : this.listeners) {
                listener.onNewRoom(room);
            }

            dispose();
        } finally {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void cancelRoomCreation() {
        dispose();
    }

    public void addNewRoomListener(final NewRoomListener listener) {
        this.listeners.add(listener);
    }

}
