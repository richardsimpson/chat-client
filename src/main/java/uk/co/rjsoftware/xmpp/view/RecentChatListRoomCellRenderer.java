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
package uk.co.rjsoftware.xmpp.view;

import uk.co.rjsoftware.xmpp.model.Room;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RecentChatListRoomCellRenderer extends RoomListCellRenderer {

    private JPanel unreadMessageCountPanel;
    private JLabel unreadMessageCountLabel;
    private boolean componentInitialised;

    public RecentChatListRoomCellRenderer() {
        this.unreadMessageCountPanel = new CirclePanel(new BorderLayout());
        this.unreadMessageCountPanel.setOpaque(false);
        this.unreadMessageCountPanel.setBorder(new EmptyBorder(0, 0, 0, 10));
        this.unreadMessageCountLabel = new JLabel();

        final Font font = this.unreadMessageCountLabel.getFont();
        final Font newFont = new Font(font.getName(), Font.BOLD, font.getSize());
        this.unreadMessageCountLabel.setFont(newFont);

        this.unreadMessageCountPanel.add(this.unreadMessageCountLabel, BorderLayout.CENTER);
    }

    @Override
    protected void setupMainPanel(JPanel mainPanel, JList<? extends Room> list, Room object, int index, boolean isSelected, boolean cellHasFocus) {
        super.setupMainPanel(mainPanel, list, object, index, isSelected, cellHasFocus);

        // add the unread message count panel
        if (!this.componentInitialised) {
            mainPanel.add(this.unreadMessageCountPanel, BorderLayout.LINE_END);
            this.componentInitialised = true;
        }

        if (isSelected) {
            this.unreadMessageCountLabel.setForeground(list.getSelectionForeground());
        }
        else {
            this.unreadMessageCountLabel.setForeground(CirclePanel.DARK_ORANGE);
        }

        if (object.getUnreadMessageCount() == 0) {
            mainPanel.remove(this.unreadMessageCountPanel);
            this.componentInitialised = false;
        }
        else {
            this.unreadMessageCountLabel.setText(" " + Integer.toString(object.getUnreadMessageCount()) + " ");
        }
    }

}
