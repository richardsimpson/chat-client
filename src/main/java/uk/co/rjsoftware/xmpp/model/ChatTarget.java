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
package uk.co.rjsoftware.xmpp.model;

import uk.co.rjsoftware.xmpp.client.CustomConnection;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.beans.PropertyChangeListener;

public interface ChatTarget {

    String TITLE_PROPERTY_NAME = "title";
    String NAME_PROPERTY_NAME = "name";
    String LATEST_MESSAGE_TIMESTAMP_PROPERTY_NAME = "latestMessageTimestamp";
    String UNREAD_MESSAGE_COUNT_PROPERTY_NAME = "unreadMessageCount";

    void join(final CustomConnection customConnection);

    void rejoin(final CustomConnection customConnection);

    String getId();

    String getName();

    String getTitle();

    ImageIcon getStatusIcon();

    CustomMessageListModel getCustomMessageListModel();

    StyledDocument getMessagesDocument();

    void sendMessage(final String messageText);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

    UserListModel getOccupantsModel();

    void delete();

    void writeChatHistory();

    long getLatestMessageTimestamp();

    int getUnreadMessageCount();

    void setMessageRead(Integer messageIndex);
}
