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

import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.User;

import javax.swing.*;
import java.awt.*;

public class RecentChatListCellRenderer implements ListCellRenderer<ChatTarget> {

    private UserListCellRenderer userListCellRenderer = new UserListCellRenderer();
    private RoomListCellRenderer roomListCellRenderer = new RoomListCellRenderer();

    @Override
    public Component getListCellRendererComponent(JList<? extends ChatTarget> list, ChatTarget chatTarget, int index, boolean isSelected, boolean cellHasFocus) {
        if (chatTarget instanceof User) {
            return userListCellRenderer.getListCellRendererComponent((JList<? extends User>)list, (User)chatTarget, index, isSelected, cellHasFocus);
        }
        else if (chatTarget instanceof Room) {
            return roomListCellRenderer.getListCellRendererComponent((JList<? extends Room>)list, (Room)chatTarget, index, isSelected, cellHasFocus);
        }
        else {
            throw new RuntimeException("unexpected ChatTarget class: " + chatTarget.getClass().getCanonicalName());
        }
    }
}
