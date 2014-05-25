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

import javax.swing.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Room extends Model implements Comparable<Room>, ChatTarget {

    private CustomConnection customConnection;

    private final String roomId;
    private final String name;
    private final UserListModel occupantsModel = new UserListModel();
    // the participantMap maps occupantJid's (room@chat.hipchat.com/nick) to users
    // so that the occupants can be removed from the occupantsModel when they leave.
    private final Map<String, User> participantMap = new HashMap<String, User>();
    private String subject = "";
    private MultiUserChat chat;
    private CustomMessageListModel customMessageListModel = new CustomMessageListModel();
    private Thread messageReceivingThread;

    public Room(final String roomId, final String name) {
        this.roomId = roomId;
        this.name = name;
    }

    public String getRoomId() {
        return this.roomId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getTitle() {
        if ((null == this.subject) || (this.subject.equals(""))) {
            return this.name;
        }
        else {
            return this.name + " (" + this.subject+  ")";
        }
    }

    private void setSubject(final String subject) {
        final String oldValue = this.getTitle();
        this.subject = subject;
        final String newValue = this.getTitle();
        if (!oldValue.equals(newValue)) {
            firePropertyChange(ChatTarget.TITLE_PROPERTY_NAME, oldValue, newValue);
        }
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
    // TODO: Display list of people in the room, together with connection status
    public void join(final CustomConnection customConnection) {
        this.customConnection = customConnection;

        if (this.chat == null) {
            this.chat = customConnection.joinRoom(this);
            try {
                final String password = "";
                DiscussionHistory history = new DiscussionHistory();
                // TODO: Get more of the history, and provide ability to get even more.
                history.setMaxStanzas(20);
                chat.join(customConnection.getCurrentUser().getName(), password, history, SmackConfiguration.getPacketReplyTimeout());

                // create a separate thread that will fetch the chat history and all future messages for this room
                this.messageReceivingThread = new Thread(new MessageReceiver(this.chat, this.customMessageListModel, this));
                this.messageReceivingThread.start();

                // Note: List of occupants is fine for public rooms, but for private rooms, would like to
                // display all of the users who are allowed access, but who are not currently online.
                final Iterator<String> occupants = this.chat.getOccupants();
                while (occupants.hasNext()) {
                    final String occupantJID = occupants.next();
                    System.out.println("Occupant: " + occupantJID);
                    final Occupant occupant = this.chat.getOccupant(occupantJID);
                    System.out.println("User JID: " + occupant.getJid());

                    final User user = customConnection.getUserListModel().get(StringUtils.parseBareAddress(occupant.getJid()));
                    if (null != user) {
                        this.occupantsModel.add(user);
                        this.participantMap.put(occupantJID, user);
                    }
                }

                // add all the chat listeners
                this.chat.addSubjectUpdatedListener(new SubjectUpdatedListenerImpl(this));
                this.chat.addParticipantStatusListener(new ParticipantStatusListenerImpl(this));

            }
            catch (XMPPException exception) {
                this.chat = null;
                throw new RuntimeException(exception);
            }
        }
    }

    private static class MessageReceiver extends SwingWorker<List<MessagePayload>, MessagePayload> {

        private final MultiUserChat chat;
        private final CustomMessageListModel customMessageListModel;
        private final Room room;

        public MessageReceiver(final MultiUserChat chat, final CustomMessageListModel customMessageListModel,
                               final Room room) {
            this.chat = chat;
            this.customMessageListModel = customMessageListModel;
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
                }
                catch (RuntimeException exception) {
                    System.out.println(exception);
                    // forced to use a local variable to stop the thread, since the InterruptedException is swallowed
                    // inside nextMessage
                    interrupted = true;
                }
            }

            return null;
        }

        @Override
        protected void process(List<MessagePayload> chunks) {
            for (MessagePayload messagePayload : chunks) {
                if (messagePayload.getCustomMessage() != null) {
                    this.customMessageListModel.add(messagePayload.getCustomMessage());
                }
                else if (messagePayload.getSubject() != null) {
                    this.room.setSubject(messagePayload.getSubject());
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

    private static class SubjectUpdatedListenerImpl implements SubjectUpdatedListener {

        private final Room room;

        public SubjectUpdatedListenerImpl(final Room room) {
            this.room = room;
        }

        @Override
        public void subjectUpdated(String subject, String from) {
            room.setSubject(subject);
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

            System.out.println("participant jid: " + occupant.getJid());

            final User user = this.room.customConnection.getUserListModel().get(StringUtils.parseBareAddress(occupant.getJid()));
            if (null != user) {
                this.room.occupantsModel.add(user);
                this.room.participantMap.put(participant, user);
            }
        }

        @Override
        public void left(String participant) {
            System.out.println("participant left: " + participant);

            final User user = this.room.participantMap.get(participant);
            if (null != user) {
                this.room.occupantsModel.remove(user);
                this.room.participantMap.remove(participant);
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
            }
            catch (XMPPException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    @Override
    public CustomMessageListModel getCustomMessageListModel() {
        return this.customMessageListModel;
    }

    @Override
    public UserListModel getOccupantsModel() {
        return this.occupantsModel;
    }

}
