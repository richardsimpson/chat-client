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
package uk.co.rjsoftware.xmpp;

import com.jgoodies.binding.list.SelectionInList;
import uk.co.rjsoftware.xmpp.model.User;
import uk.co.rjsoftware.xmpp.model.UserListModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UserListCellRenderer implements ListCellRenderer<User> {

    private static final int STATUS_COLUMN_WIDTH = 75;

    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JPanel usernamePanel;
    private JLabel usernameLabel;

    private UserListModel userListModel;
    private String ownerId;

    private final Font boldFont;
    private final Font normalFont;

    // rightBorder is a hack to provide a gap between columns when this renderer is used to display the list
    // of users within a room.
    public UserListCellRenderer(final int rightBorder) {
        this.mainPanel = new JPanel();
        this.mainPanel.setLayout(new BorderLayout());

        // status
        this.statusPanel = new JPanel(new BorderLayout());
        this.statusPanel.setOpaque(false);
        this.statusPanel.setBorder(new EmptyBorder(4, 2, 2, 4));
        this.statusLabel = new JLabel();
        //this.statusLabel.setPreferredSize(new Dimension(STATUS_COLUMN_WIDTH, 10));
        //this.senderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.statusPanel.add(this.statusLabel, BorderLayout.PAGE_START);
        this.mainPanel.add(this.statusPanel, BorderLayout.LINE_START);

        // username
        this.usernamePanel = new JPanel(new BorderLayout());
        this.usernamePanel.setOpaque(false);
        this.usernamePanel.setBorder(new EmptyBorder(0, 0, 0, rightBorder));
        this.usernameLabel = new JLabel();
        this.usernamePanel.add(this.usernameLabel, BorderLayout.CENTER);
        this.mainPanel.add(this.usernamePanel, BorderLayout.CENTER);

        // fonts
        this.normalFont = this.usernameLabel.getFont();
        this.boldFont = new Font(this.normalFont.getName(), Font.BOLD, this.normalFont.getSize());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends User> list, User user, int index, boolean isSelected, boolean cellHasFocus) {
        this.userListModel = null;
        this.ownerId = null;

        if (list.getModel() instanceof SelectionInList) {
            final SelectionInList selectionInList = (SelectionInList)list.getModel();
            if (selectionInList.getListHolder().getValue() instanceof UserListModel) {
                this.userListModel = (UserListModel)selectionInList.getListHolder().getValue();
            }
        }

        if (null != this.userListModel) {
            this.ownerId = this.userListModel.getOwnerId();
        }

        if ((this.ownerId != null) && (this.ownerId.equals(user.getUserId()))) {
            this.usernameLabel.setFont(boldFont);
        }
        else {
            this.usernameLabel.setFont(normalFont);
        }

        this.usernameLabel.setText(user.getName());

        this.statusLabel.setIcon(user.getHighestStatus().getImageIcon());

        if (isSelected) {
            this.mainPanel.setBackground(list.getSelectionBackground());
            this.usernameLabel.setForeground(list.getSelectionForeground());
        }
        else {
            this.mainPanel.setBackground(list.getBackground());
            this.usernameLabel.setForeground(list.getForeground());
        }

        return this.mainPanel;
    }
}
