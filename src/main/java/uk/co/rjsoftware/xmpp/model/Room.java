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
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import uk.co.rjsoftware.xmpp.client.CustomConnection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DelayInfo;
import uk.co.rjsoftware.xmpp.view.MessageListHTMLDocument;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.StyledDocument;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Room extends Model implements Comparable<Room>, ChatTarget {

    public static final String SUBJECT_PROPERTY_NAME = "subject";
    public static final String PRIVACY_PROPERTY_NAME = "privacy";
    public static final String OWNER_ID_PROPERTY_NAME = "ownerId";

    private CustomConnection customConnection;

    private final String roomId;
    private String name;
    private RoomPrivacy privacy;
    private String ownerId;

    private final UserListModel occupantsModel = new UserListModel();
    // the participantMap maps participantJid's (room@chat.hipchat.com/nick) to users
    // so that the occupants can be removed from the occupantsModel when they leave.
    private final Map<String, User> participantMap = new HashMap<String, User>();
    private String subject = "";
    private MultiUserChat chat;
    private CustomMessageListModel customMessageListModel = new CustomMessageListModel();
    private final MessageListHTMLDocument messagesDocument;
    private Thread messageReceivingThread;

    private ChatPersistor chatPersistor;

    public Room(final String roomId, final String name) {
        this.roomId = roomId;
        this.name = name;
        this.messagesDocument = new MessageListHTMLDocument();
        this.customMessageListModel.addListDataListener(new ChatListDataListener(this.customMessageListModel, this.messagesDocument));
    }

    // TODO: Remove getRoomId
    public String getRoomId() {
        return this.roomId;
    }

    @Override
    public String getId() {
        return this.roomId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        if (this.name != name) {
            final String oldNameValue = this.name;
            this.name = name;
            firePropertyChange(ChatTarget.NAME_PROPERTY_NAME, oldNameValue, name);
        }
    }

    public String getTitle() {
        return this.subject;
    }

    private void doSetSubject(final String subject) {
        if (this.subject != subject) {
            final String oldSubjectValue = this.subject;
            final String oldTitleValue = oldSubjectValue;

            this.subject = subject;

            firePropertyChange(SUBJECT_PROPERTY_NAME, oldSubjectValue, subject);
            firePropertyChange(ChatTarget.TITLE_PROPERTY_NAME, oldTitleValue, subject);
        }
    }

    public void setSubject(final String subject) {
        try {
            this.chat.changeSubject(subject);
            doSetSubject(subject);
        } catch (XMPPException exception) {
            throw new RuntimeException(exception);
        }

    }

    public String getSubject() {
        return this.subject;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(final Room room) {
        return this.name.toUpperCase(Locale.getDefault()).compareTo(room.name.toUpperCase(Locale.getDefault()));
    }

    @Override
    public void join(final CustomConnection customConnection) {
        this.customConnection = customConnection;

        if (this.chat == null) {
            this.chatPersistor = new ChatPersistor(customConnection.getCurrentUser().getUserId(), this.roomId, this.customMessageListModel);
            this.chatPersistor.readChatHistory();
            this.chat = customConnection.joinRoom(this);

            // add all the chat listeners
            this.chat.addSubjectUpdatedListener(new SubjectUpdatedListenerImpl(this));
            this.chat.addParticipantStatusListener(new ParticipantStatusListenerImpl(this));

            try {
                final String password = "";
                DiscussionHistory history = new DiscussionHistory();

                // request history starting from just after the last message retrieved from the local history, if any
                final long lastMessageTimestamp;
                if (this.customMessageListModel.isEmpty()) {
                    lastMessageTimestamp = 0;
                }
                else {
                    final CustomMessage latestMessage = this.customMessageListModel.get(this.customMessageListModel.size()-1);
                    lastMessageTimestamp = latestMessage.getTimestamp();
                }
                history.setSince(new Date(lastMessageTimestamp + 1));

                // note that the above doesn't seem to work in hipchat - it will reply with lots of messages before the
                // specified date - about 75, it seems, so need to rely on the MessageReceiver to filter out old
                // messages.

                chat.join(customConnection.getCurrentUser().getName(), password, history, SmackConfiguration.getPacketReplyTimeout());

                // create a separate thread that will fetch the chat history and all future messages for this room
                this.messageReceivingThread = new Thread(new MessageReceiver(this.chat, this.customMessageListModel, this));
                this.messageReceivingThread.start();

                // Note: List of occupants is fine for public rooms, but for private rooms, would like to
                // display all of the users who are allowed access, but who are not currently online.
                final Iterator<String> occupants = this.chat.getOccupants();
                while (occupants.hasNext()) {
                    final String participantJID = occupants.next();
                    System.out.println("Participant: " + participantJID);
                    final Occupant occupant = this.chat.getOccupant(participantJID);
                    System.out.println("User JID: " + occupant.getJid());

                    addOccupant(participantJID, occupant);
                }

            } catch (XMPPException exception) {
                this.chat = null;
                // Remove the messages read from the local history
                this.customMessageListModel.clear();
                this.messagesDocument.clear();
                throw new RuntimeException(exception);
            }
        }
    }

    private void addOccupant(final String participantJID, final Occupant occupant) {
        final User user = customConnection.getUserListModel().get(occupant.getJid());
        if (null != user) {
            synchronized (this.occupantsModel) {
                if (!this.occupantsModel.contains(user.getUserId())) {
                    this.occupantsModel.add(user);
                    this.participantMap.put(participantJID, user);
                }
            }
        }
    }

    private static class MessageReceiver extends SwingWorker<List<MessagePayload>, MessagePayload> {

        private final MultiUserChat chat;
        private final CustomMessageListModel customMessageListModel;
        private final CustomMessage mostRecentMessageFromLocalHistory;
        private final Room room;

        public MessageReceiver(final MultiUserChat chat, final CustomMessageListModel customMessageListModel,
                               final Room room) {
            this.chat = chat;
            this.customMessageListModel = customMessageListModel;
            if (this.customMessageListModel.isEmpty()) {
                this.mostRecentMessageFromLocalHistory = null;
            }
            else {
                this.mostRecentMessageFromLocalHistory = this.customMessageListModel.get(this.customMessageListModel.size()-1);
            }
            this.room = room;
        }

        private long extractTimestamp(final Message message) {
            for (PacketExtension extension : message.getExtensions()) {
                if (extension instanceof DelayInfo) {
                    return ((DelayInfo)extension).getStamp().getTime();
                }
            }

            // no delay information (not an old message), so assume the message just came through
            return new Date().getTime();
        }

        @Override
        protected List<MessagePayload> doInBackground() throws Exception {
            boolean interrupted = false;
            while (!interrupted) {
                try {
                    final Message message = this.chat.nextMessage();

                    // TODO: Implement behaviour for Message.Type.headline
                    switch (message.getType()) {
                        case normal:
                        case chat:
                        case groupchat:
                            if (message.getBody() != null) {
                                final CustomMessage customMessage = new CustomMessage(extractTimestamp(message), message.getFrom(), message.getBody());
                                final MessagePayload messagePayload = new MessagePayload(customMessage);
                                publish(messagePayload);
                            }
                            else if (message.getSubject() != null) {
                                final MessagePayload messagePayload = new MessagePayload(message.getSubject());
                                publish(messagePayload);
                            }
                        default: // do nothing
                    }
                } catch (RuntimeException exception) {
                    System.out.println(exception);
                    // forced to use a local variable to stop the thread, since the InterruptedException is swallowed
                    // inside nextMessage
                    interrupted = true;
                }
            }

            return null;
        }

        private boolean isNewMessage(final CustomMessage newMessage) {
            if (null == this.mostRecentMessageFromLocalHistory) {
                return true;

            }
            if (this.mostRecentMessageFromLocalHistory.getTimestamp() > newMessage.getTimestamp()) {
                return false;
            }

            if (this.mostRecentMessageFromLocalHistory.getTimestamp() < newMessage.getTimestamp()) {
                return true;
            }

            // timestamps are the same.  hipchat messages are rounded (or truncated?) to the nearest second,
            // so it's quite possible to have multiple messages with the same timestamp.  Therefore
            // we need to check the sender and body to see if it's a new message.
            if (!this.mostRecentMessageFromLocalHistory.getSender().equals(newMessage.getSender())) {
                return true;
            }

            if (!this.mostRecentMessageFromLocalHistory.getBody().equals(newMessage.getBody())) {
                return true;
            }

            // all parts of the new message are the same as the most recent one, so must assume that it is the same message
            return false;
        }

        @Override
        protected void process(List<MessagePayload> chunks) {
            for (MessagePayload messagePayload : chunks) {
                if (messagePayload.getCustomMessage() != null) {
                    if (isNewMessage(messagePayload.getCustomMessage())) {
                        this.customMessageListModel.add(messagePayload.getCustomMessage());
                    }
                }
                else if (messagePayload.getSubject() != null) {
                    this.room.doSetSubject(messagePayload.getSubject());
                }
            }
        }
    }

    private static class MessagePayload {
        private CustomMessage customMessage;
        private String subject;

        public MessagePayload(final String subject) {
            this.subject = subject;
        }

        public MessagePayload(final CustomMessage customMessage) {
            this.customMessage = customMessage;
        }

        public CustomMessage getCustomMessage() {
            return customMessage;
        }

        public String getSubject() {
            return subject;
        }
    }

    private static final class ChatListDataListener implements ListDataListener {

        private final CustomMessageListModel customMessageListModel;
        private final MessageListHTMLDocument messagesDocument;

        private ChatListDataListener(final CustomMessageListModel customMessageListModel, final MessageListHTMLDocument messagesDocument) {
            this.customMessageListModel = customMessageListModel;
            this.messagesDocument = messagesDocument;
        }

        @Override
        public void intervalAdded(ListDataEvent event) {
            final int index = event.getIndex0();
            final CustomMessage message = this.customMessageListModel.get(index);
            this.messagesDocument.insertMessage(message, index);
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

    private static class SubjectUpdatedListenerImpl implements SubjectUpdatedListener {

        private final Room room;

        public SubjectUpdatedListenerImpl(final Room room) {
            this.room = room;
        }

        @Override
        public void subjectUpdated(String subject, String from) {
            room.doSetSubject(subject);
            System.out.println("Room.SubjectUpdatedListener: subject: " + subject + ", from: " + from);
        }
    }

    private static class ParticipantStatusListenerImpl extends DefaultParticipantStatusListener {

        private final Room room;

        public ParticipantStatusListenerImpl(final Room room) {
            this.room = room;
        }

        @Override
        public void joined(String participant) {
            final Occupant occupant = this.room.chat.getOccupant(participant);

            System.out.println("participant name: " + StringUtils.parseResource(participant) +  ", jid: " + occupant.getJid());

            this.room.addOccupant(participant, occupant);
        }

        @Override
        public void left(String participant) {
            System.out.println("participant left: " + participant);

            synchronized (this.room.occupantsModel) {
                final User user = this.room.participantMap.get(participant);
                if (null != user) {
                    this.room.occupantsModel.remove(user);
                    this.room.participantMap.remove(participant);
                }
            }
        }
    }

    public void cleanUp() {
        if (this.messageReceivingThread != null) {
            this.messageReceivingThread.interrupt();
        }
    }

    @Override
    public void sendMessage(final String messageText) {
        if (this.chat != null) {
            try {
                this.chat.sendMessage(messageText);
            } catch (XMPPException exception) {
                throw new RuntimeException(exception);
            }
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

    @Override
    public void delete() {
        try {
            this.chat.destroy(null, null);
        } catch (XMPPException exception) {
            throw new RuntimeException(exception);
        }
    }

    public RoomPrivacy getPrivacy() {
        return this.privacy;
    }

    public void setPrivacy(RoomPrivacy privacy) {
        if (this.privacy != privacy) {
            final RoomPrivacy oldValue = this.privacy;
            this.privacy = privacy;
            firePropertyChange(PRIVACY_PROPERTY_NAME, oldValue, privacy);
        }
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        if (this.ownerId != ownerId) {
            final String oldValue = this.ownerId;
            this.ownerId = ownerId;
            this.occupantsModel.setOwnerId(ownerId);
            firePropertyChange(OWNER_ID_PROPERTY_NAME, oldValue, ownerId);
        }
    }

    public void invite(final String user, final String reason) {
        this.chat.invite(user, reason);
    }

    @Override
    public void writeChatHistory() {
        this.chatPersistor.writeChatHistory();
    }
}
