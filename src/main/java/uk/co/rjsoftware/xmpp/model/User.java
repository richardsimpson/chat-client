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

import com.jgoodies.binding.beans.Model;
import org.jivesoftware.smack.util.StringUtils;
import uk.co.rjsoftware.xmpp.client.CustomConnection;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import uk.co.rjsoftware.xmpp.view.MessageListHTMLDocument;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.StyledDocument;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class User extends Model implements Comparable<User>, ChatTarget {

    public static final String HIGHEST_STATUS_PROPERTY_NAME = "highestStatus";

    private final String userId;
    private final String name;
    private final UserListModel occupantsModel = new UserListModel();

    private Map<String, UserStatus> statuses = new HashMap<String, UserStatus>();

    private Chat chat;
    private CustomMessageListModel customMessageListModel = new CustomMessageListModel();
    private final MessageListHTMLDocument messagesDocument;
    private CustomConnection customConnection;
    private long latestMessageTimestamp;
    private int unreadMessageCount;

    private ChatPersistor chatPersistor;

    public User(final String userId, final String name) {
        this.userId = userId;
        this.name = name;
        this.occupantsModel.add(this);
        this.messagesDocument = new MessageListHTMLDocument();
        this.customMessageListModel.addListDataListener(new ChatListDataListener(this));
    }

    // TODO: Remove getUserId
    public String getUserId() {
        return this.userId;
    }

    @Override
    public String getId() {
        return this.userId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getTitle() {
        return this.name;
    }

    public void setStatus(final String resource, final UserStatus status) {
        final UserStatus oldValue = this.statuses.get(resource);
        if (oldValue != status) {
            final UserStatus oldHighestStatus = getHighestStatus();
            this.statuses.put(resource, status);
            final UserStatus newHighestStatus = getHighestStatus();
            if (oldHighestStatus != newHighestStatus) {
                firePropertyChange(HIGHEST_STATUS_PROPERTY_NAME, oldValue, status);
            }
        }
    }

    public UserStatus getHighestStatus() {
        UserStatus highestStatus = null;

        for (UserStatus status : this.statuses.values()) {
            if (null==highestStatus) {
                highestStatus = status;
            }
            else if (status.getPriority() < highestStatus.getPriority()) {
                highestStatus = status;
            }
        }

        if (null == highestStatus) {
            return UserStatus.UNAVAILABLE;
        }

        return highestStatus;
    }

    public Map<String, UserStatus> getStatuses() {
        return this.statuses;
    }

//    @Override
//    public String toString() {
//        String result = this.name + " (";
//        final Iterator<Map.Entry<String, UserStatus>> iterator = this.statuses.entrySet().iterator();
//
//        while (iterator.hasNext()) {
//            final Map.Entry<String, UserStatus> entry = iterator.next();
//            final String resource;
//            if (entry.getKey().equals("")) {
//                resource = "default";
//            }
//            else {
//                resource = entry.getKey();
//            }
//            result = result + resource + ":" + entry.getValue().getDescription();
//            if (iterator.hasNext()) {
//                result = result + ", ";
//            }
//        }
//
//        result = result + ")";
//        return result;
//    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public ImageIcon getStatusIcon() {
        return getHighestStatus().getImageIcon();
    }

    @Override
    public int compareTo(final User user) {
        return this.name.toUpperCase(Locale.getDefault()).compareTo(user.name.toUpperCase(Locale.getDefault()));
    }

    @Override
    public void join(final CustomConnection customConnection) {
        if (this.chat == null) {
            this.chatPersistor = new ChatPersistor(customConnection.getCurrentUser().getUserId(), this.userId, this.customMessageListModel);
            this.chatPersistor.readChatHistory();
            this.customConnection = customConnection;
            this.chat = customConnection.createChat(this);
            this.chat.addMessageListener(new UserMessageListener(this.userId, this.name, customConnection, this.customMessageListModel));
        }
    }

    public void joinExistingChat(final CustomConnection customConnection, final Chat chat) {
        if (this.chat == null) {
            this.chatPersistor = new ChatPersistor(customConnection.getCurrentUser().getUserId(), this.userId, this.customMessageListModel);
            this.chatPersistor.readChatHistory();
            this.customConnection = customConnection;
            this.chat = chat;
            this.chat.addMessageListener(new UserMessageListener(this.userId, this.name, customConnection, this.customMessageListModel));
        }
    }

    private static class UserMessageListener implements MessageListener {

        private final String otherUserId;
        private final String otherUsername;
        private final CustomConnection customConnection;
        private final CustomMessageListModel customMessageListModel;

        public UserMessageListener(final String otherUserId, final String otherUsername, final CustomConnection customConnection,
                                   final CustomMessageListModel customMessageListModel) {
            this.otherUserId = otherUserId;
            this.otherUsername = otherUsername;
            this.customConnection = customConnection;
            this.customMessageListModel = customMessageListModel;
        }

        @Override
        public void processMessage(Chat chat, Message message) {
//            System.out.println("Incomming Message: Type: " + message.getType() + ", Body: " + message.getBody() +
//                    ", Subject: " + message.getSubject());
//            for (PacketExtension extension : message.getExtensions()) {
//                if (extension instanceof ChatStateExtension) {
//                    System.out.println("Extension: State: " + ((ChatStateExtension)extension).getElementName() +
//                            ", Type: " + extension.getClass().getName() +
//                            ", Namespace: " + extension.getNamespace());
//                }
//                else {
//                    System.out.println("Extension: Type: " + extension.getClass().getName() +
//                            ", elementName: " + extension.getElementName() + ", Namespace: " + extension.getNamespace());
//                }
//            }

            // TODO: If body is empty, check for a packet extension of type ChatStateExtension, with a state of 'composing' - could display '[blaa] is typing'.
            // Hipchat doesn't send anything to 'turn this off'.  It re-sends it if the user clears their text input and
            // starts again.  In our own client we could send a Message with no body and an extension using a ChatState of
            // paused.
            switch (message.getType()) {
                case normal:
                case chat:
                case groupchat:
                    if (message.getBody() != null) {
                        // chec the sender (message.from) - e.g. 380.._nnnnnn@chat.hipchat.com..., and look it
                        // up.  It's not always going to be the other person - it could be the current user
                        // typing in another chat client.
                        String username;
                        if (StringUtils.parseBareAddress(message.getFrom()).equals(this.otherUserId)) {
                            username = this.otherUsername;
                        }
                        else {
                            // this is a one-2-one chat, so if the sender wasn't the other user, it must be the
                            // current user.
                            username = customConnection.getCurrentUser().getName();
                        }
                        final CustomMessage customMessage = new CustomMessage(extractTimestamp(message), username, message.getBody());
                        this.customMessageListModel.add(customMessage);
                    }
                default: // do nothing
            }
        }

        private long extractTimestamp(final Message message) {
            // one to one chat messages don't have valid timestamps, so always use the current time
            return new Date().getTime();
        }
    }

    @Override
    public void sendMessage(final String messageText) {
        if (this.chat != null) {
            try {
                this.chat.sendMessage(messageText);
                final CustomMessage customMessage = new CustomMessage(System.currentTimeMillis(),
                        this.customConnection.getCurrentUser().getName(), messageText);
                this.customMessageListModel.add(customMessage);

            } catch (XMPPException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static final class ChatListDataListener implements ListDataListener {

        private final User user;
        private final CustomMessageListModel customMessageListModel;
        private final MessageListHTMLDocument messagesDocument;

        private ChatListDataListener(final User user) {
            this.user = user;
            this.customMessageListModel = user.customMessageListModel;
            this.messagesDocument = user.messagesDocument;
        }

        @Override
        public void intervalAdded(ListDataEvent event) {
            final int index = event.getIndex0();
            final CustomMessage message = this.customMessageListModel.get(index);
            this.messagesDocument.insertMessage(message, index);

            // update 'latestMessageTimestamp in the ChatTarget (room)
            if (message.getTimestamp() > this.user.latestMessageTimestamp) {
                this.user.setLatestMessageTimestamp(message.getTimestamp());
            }

            if (!message.isRead()) {
                this.user.setUnreadMessageCount(this.user.getUnreadMessageCount() + 1);
            }
        }

        @Override
        public void intervalRemoved(ListDataEvent event) {
            // should never happen
        }

        @Override
        public void contentsChanged(ListDataEvent event) {
            // should never happen
        }

    }

    @Override
    public CustomMessageListModel getCustomMessageListModel() {
        return this.customMessageListModel;
    }

    @Override
    public StyledDocument getMessagesDocument() {
        return this.messagesDocument;
    }

    @Override
    public UserListModel getOccupantsModel() {
        return this.occupantsModel;
    }

    private void setLatestMessageTimestamp(final long latestMessageTimestamp) {
        if (this.latestMessageTimestamp != latestMessageTimestamp) {
            final long oldTimestamp = this.latestMessageTimestamp;
            this.latestMessageTimestamp = latestMessageTimestamp;

            firePropertyChange(LATEST_MESSAGE_TIMESTAMP_PROPERTY_NAME, oldTimestamp, latestMessageTimestamp);
        }
    }

    @Override
    public long getLatestMessageTimestamp() {
        return this.latestMessageTimestamp;
    }

    private void setUnreadMessageCount(final int unreadMessageCount) {
//        why is this being called twice??  Is the MessageStateChanger being called
//        twice when a message is added?

        if (this.unreadMessageCount != unreadMessageCount) {
            final long oldUnreadMessageCount = this.unreadMessageCount;
            this.unreadMessageCount = unreadMessageCount;

            firePropertyChange(UNREAD_MESSAGE_COUNT_PROPERTY_NAME, oldUnreadMessageCount, unreadMessageCount);
        }
    }

    @Override
    public int getUnreadMessageCount() {
        return this.unreadMessageCount;
    }

    @Override
    public void setMessageRead(final Integer messageIndex) {
        CustomMessage message = this.customMessageListModel.get(messageIndex);

        if (!message.isRead()) {
            message.setRead(true);
            setUnreadMessageCount(this.unreadMessageCount-1);
        }
    }

    @Override
    public void delete() {
        throw new RuntimeException("Cannot delete a single user chat");
    }

    @Override
    public void writeChatHistory() {
        this.chatPersistor.writeChatHistory();
    }
}
