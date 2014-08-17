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
package uk.co.rjsoftware.xmpp.dialogs.inviteusers;

import uk.co.rjsoftware.xmpp.client.CustomConnection;
import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.User;
import uk.co.rjsoftware.xmpp.model.UserListModel;
import uk.co.rjsoftware.xmpp.view.UserListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class InviteUsersForm extends JDialog {

    private final JLabel existingUsersLabel;
    private final JLabel selectedUsersLabel;
    private final JList<User> existingUsersList;
    private final JList<User> selectedUsersList;
    private final JButton addButton;
    private final JButton removeButton;
    private final JButton okButton;
    private final JButton cancelButton;

    private final UserListModel selectedUserListModel;
    private final UserListModel existingUserListModel;

    private final CustomConnection connection;

    public InviteUsersForm(final CustomConnection connection) {
        super(null, "Invite Users", ModalityType.APPLICATION_MODAL);

        this.connection = connection;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        this.existingUsersLabel = new JLabel("Users:");
        constraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(10, 10, 2, 2), 0, 0);
        pane.add(this.existingUsersLabel, constraints);

        this.existingUserListModel = new UserListModel(connection.getUserListModel());
        this.existingUsersList = new JList<User>(existingUserListModel);
        this.existingUsersList.setCellRenderer(new UserListCellRenderer());
        this.existingUsersList.setVisibleRowCount(20);
        constraints = new GridBagConstraints(0, 1, 1, 5, 0.5, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 10, 2, 2), 0, 0);
        pane.add(new JScrollPane(this.existingUsersList), constraints);

        this.addButton = new JButton("Add >");
        constraints = new GridBagConstraints(1, 2, 1, 2, 0, 0.25, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);
        pane.add(this.addButton, constraints);

        this.addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelectedItems();
            }
        });

        this.removeButton = new JButton("< Remove");
        constraints = new GridBagConstraints(1, 4, 1, 2, 0, 0.25, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);
        pane.add(this.removeButton, constraints);

        this.removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedItems();
            }
        });

        this.selectedUsersLabel = new JLabel("Selected Users:");
        constraints = new GridBagConstraints(2, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(10, 2, 2, 10), 0, 0);
        pane.add(this.selectedUsersLabel, constraints);

        this.selectedUserListModel = new UserListModel();
        this.selectedUsersList = new JList(selectedUserListModel);
        this.selectedUsersList.setCellRenderer(new UserListCellRenderer());
        this.selectedUsersList.setVisibleRowCount(20);
        constraints = new GridBagConstraints(2, 1, 3, 5, 0.5, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 10), 0, 0);
        pane.add(new JScrollPane(this.selectedUsersList), constraints);

        final JPanel buttonPanel =  new JPanel(new FlowLayout(FlowLayout.RIGHT));
        constraints = new GridBagConstraints(0, 6, 5, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        pane.add(buttonPanel, constraints);

        this.okButton = new JButton("Invite");
        buttonPanel.add(this.okButton);

        this.okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendInvites();
            }
        });

        this.cancelButton = new JButton("Cancel");
        buttonPanel.add(this.cancelButton);

        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelInvites();
            }
        });

        setPreferredSize(new Dimension(661, 436));
        setResizable(false);

        getRootPane().setDefaultButton(this.okButton);

        pack();

        DialogUtils.centerDialog(this);
    }

    private void addSelectedItems() {
        final List<User> selectedUsers = this.existingUsersList.getSelectedValuesList();
        this.selectedUserListModel.addAll(selectedUsers);
        this.existingUserListModel.removeAll(selectedUsers);
        this.existingUsersList.getSelectionModel().clearSelection();
    }

    private void removeSelectedItems() {
        final List<User> selectedUsers = this.selectedUsersList.getSelectedValuesList();
        this.existingUserListModel.addAll(selectedUsers);
        this.selectedUserListModel.removeAll(selectedUsers);
        this.selectedUsersList.getSelectionModel().clearSelection();
    }

    private void sendInvites() {
        for (int index = 0 ; index < this.selectedUserListModel.getSize() ; index++) {
            final User user = this.selectedUserListModel.getElementAt(index);
            ((Room)this.connection.getCurrentChatTarget()).invite(user.getUserId(), "");
        }

        dispose();
    }

    private void cancelInvites() {
        dispose();
    }

}
