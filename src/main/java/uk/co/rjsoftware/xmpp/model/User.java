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
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import javax.swing.event.ListDataListener;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class User implements Comparable<User>, ChatTarget {

    private final String userId;
    private final String name;
    private Map<String, UserStatus> statuses = new HashMap<String, UserStatus>();

    private Chat chat;
    private CustomMessageListModel customMessageListModel = new CustomMessageListModel();
    private CustomConnection customConnection;

    public User(final String userId, final String name) {
        this.userId = userId;
        this.name = name;
        this.statuses.put("", UserStatus.UNAVAILABLE);
    }

    public String getUserId() {
        return this.userId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setStatus(final String resource, final UserStatus status) {
        this.statuses.put(resource, status);
    }

    public Map<String, UserStatus> getStatuses() {
        return this.statuses;
    }

    @Override
    public String toString() {
        String result = this.name + " (";
        final Iterator<Map.Entry<String, UserStatus>> iterator = this.statuses.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<String, UserStatus> entry = iterator.next();
            final String resource;
            if (entry.getKey().equals("")) {
                resource = "default";
            }
            else {
                resource = entry.getKey();
            }
            result = result + resource + ":" + entry.getValue().getDescription();
            if (iterator.hasNext()) {
                result = result + ", ";
            }
        }

        result = result + ")";
        return result;
    }

    @Override
    public int compareTo(final User user) {
        return this.name.toUpperCase(Locale.getDefault()).compareTo(user.name.toUpperCase(Locale.getDefault()));
    }

    @Override
    public void join(final CustomConnection customConnection) {
        if (this.chat == null) {
            this.customConnection = customConnection;
            this.chat = customConnection.createChat(this.userId);
            this.chat.addMessageListener(new UserMessageListener(this.name, this.customMessageListModel));
        }
    }

    private static class UserMessageListener implements MessageListener {

        private final String senderName;
        private final CustomMessageListModel customMessageListModel;

        public UserMessageListener(final String senderName, final CustomMessageListModel customMessageListModel) {
            this.senderName = senderName;
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
                        final CustomMessage customMessage = new CustomMessage(extractTimestamp(message), this.senderName, message.getBody());
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
                this.customMessageListModel.add(new CustomMessage(System.currentTimeMillis(),
                        this.customConnection.getCurrentUser().getName(), messageText));
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
    public void addListDataListener(ListDataListener listener) {
        this.customMessageListModel.addListDataListener(listener);
    }

    @Override
    public void removeListDataListener(ListDataListener listener) {
        this.customMessageListModel.removeListDataListener(listener);
    }
}
