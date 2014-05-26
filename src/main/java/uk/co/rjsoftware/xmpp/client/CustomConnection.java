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
package uk.co.rjsoftware.xmpp.client;

import com.jgoodies.binding.beans.Model;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.ChatListModel;
import uk.co.rjsoftware.xmpp.model.CustomMessageListModel;
import uk.co.rjsoftware.xmpp.model.CustomPresence;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.RoomListModel;
import uk.co.rjsoftware.xmpp.model.User;
import uk.co.rjsoftware.xmpp.model.UserListModel;
import uk.co.rjsoftware.xmpp.model.UserStatus;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CustomConnection extends Model {

    public static final String USER_LIST_MODEL_PROPERTY_NAME = "userListModel";
    public static final String ROOM_LIST_MODEL_PROPERTY_NAME = "roomListModel";
    public static final String CHAT_LIST_MODEL_PROPERTY_NAME = "chatListModel";
    public static final String CURRENT_CHAT_TARGET_PROPERTY_NAME = "currentChatTarget";
    public static final String CURRENT_CHAT_TARGET_TITLE_PROPERTY_NAME = "currentChatTargetTitle";
    public static final String CURRENT_CHAT_TARGET_MESSAGES_LIST_PROPERTY_NAME = "currentChatTargetMessagesList";
    public static final String CURRENT_CHAT_TARGET_OCCUPANTS_PROPERTY_NAME = "currentChatTargetOccupants";
    
    private final Connection connection;
    private final Roster roster;
    private final UserListModel userListModel;
    private final RoomListModel roomListModel;
    private final ChatListModel chatListModel = new ChatListModel();
    private User currentUser;
    private final String hipChatClientPrefix;

    private ChatTarget currentChatTarget;
    private final TitlePropertyChangeListener titleListener = new TitlePropertyChangeListener();

    public CustomConnection(final String username, final String password) throws YaccException {
        // TODO: Put 'chat.hipchat.com' into config
        this.connection = new XMPPConnection("chat.hipchat.com");
        try {
            this.connection.connect();
            // TODO: Put 'xmpp' into config
            this.connection.login(username, password, "xmpp");
        }
        catch (XMPPException exception) {
            if (exception.getMessage().contains("invalid-authzid")) {
                throw new YaccException("The password or email address is invalid.");
            }
            else {
                // TODO: deal with invalid credentials properly
                throw new YaccException(exception.getMessage());
            }
        }

        final String[] usernameParts = username.split("_");
        this.hipChatClientPrefix = usernameParts[0];

        this.roster = connection.getRoster();

        // get current user id without the resource (e.g. /xmpp)
        String currentUserId = this.connection.getUser();
        int index = currentUserId.indexOf("/");
        if (index > -1) {
            currentUserId = currentUserId.substring(0, index);
        }

        // setup the user model
        final List<User> userList = new ArrayList<User>();
        final Collection<RosterEntry> rosterEntries = this.roster.getEntries();
        for (RosterEntry user : rosterEntries) {
            final User newUser = new User(user.getUser(), user.getName());

            // update the user presence's
            final Iterator<Presence> presences = this.roster.getPresences(user.getUser());
            while (presences.hasNext()) {
                final Presence presence = presences.next();
                final CustomPresence customPresence = new CustomPresence(presence);
                final String resource = customPresence.getResource();
                newUser.setStatus(resource, UserStatus.fromPresence(customPresence));
            }

            System.out.println("User: " + user.getUser() + ", Name: " + user.getName());
            userList.add(newUser);
            if (user.getUser().equals(currentUserId)) {
                this.currentUser = newUser;
            }
        }

        // TODO: Create the listener BEFORE iterating through the users and getting their presence.  Without this, we may miss
        // some presence updates if the listener is registered too late.

        if (null == this.currentUser) {
            disconnect();
            throw new YaccException("Cannot locate current user information.");
        }

        // TODO: Create a SortedListModel to decorate the UserListModel with sorting instead (see http://www.oracle.com/technetwork/articles/javase/sorted-jlist-136883.html)
        Collections.sort(userList);
        this.userListModel = new UserListModel(userList);

        // ensure user model gets updated when changes occur
        this.roster.addRosterListener(new ClientRosterListener(this.userListModel));

        // TODO: Create a SortedListModel to decorate the RoomListModel with sorting instead (see http://www.oracle.com/technetwork/articles/javase/sorted-jlist-136883.html)
        // setup the room list
        this.roomListModel = new RoomListModel();
        Collection<HostedRoom> rooms;
        try {
            rooms = MultiUserChat.getHostedRooms(connection, "conf.hipchat.com");
        }
        catch (XMPPException exception) {
            throw new RuntimeException(exception);
        }
        for (HostedRoom room : rooms) {
            final Room newRoom = new Room(room.getJid(), room.getName());
            addRoom(newRoom);
            System.out.println("name:" + room.getName() + ", JID: " + room.getJid());
        }

        //setup the chat listener, to listen for new incomming chats
        this.connection.getChatManager().addChatListener(new ChatManagerListener() {
            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                if (!createdLocally) {
                    System.out.println("New Chat: Participant: " + chat.getParticipant());
                    final String participantId = StringUtils.parseBareAddress(chat.getParticipant());
                    final User user = CustomConnection.this.userListModel.get(participantId);

                    if (user != null) {
                        CustomConnection.this.chatListModel.add(user);
                        user.joinExistingChat(CustomConnection.this, chat);
                        return;
                    }

                    // check for a room
                    final Room room = CustomConnection.this.roomListModel.get(participantId);

                    if (room != null) {
                        // don't 'join an existing chat' for rooms, so that we get the chat history.
                        // once we start caching the chat history locally, perhaps we can optimise this
                        // by implementing joinExistingChat on the Room class (and ChatTarget interface)
                        // TODO: Fix: Incomming chats for rooms don't seem to include current user in the occupants list
                        room.join(CustomConnection.this);
                    }
                }
            }
        });

        // TODO: Add an invitation listener to the connection, so can automatically join rooms we are invited to.

//        this.connection.addPacketListener(new PacketListener() {
//            @Override
//            public void processPacket(Packet packet) {
//                System.out.println("Incomming Packet: " + packet.toString());
//            }
//        }, null);
    }

    public void disconnect() {
        System.out.println("Disconnecting...");
        if (null != roomListModel) {
            for (Room room : this.roomListModel) {
                room.cleanUp();
            }
        }

        this.connection.disconnect();
        System.out.println("Disconnected");
    }

    public UserListModel getUserListModel() {
        return this.userListModel;
    }

    public RoomListModel getRoomListModel() {
        return this.roomListModel;
    }

    public ChatListModel getChatListModel() {
        return this.chatListModel;
    }

    public void addRoom(final Room newRoom) {
        final YaccRoomInfo roomInfo = getRoomInfo(newRoom);
        newRoom.setPrivacy(roomInfo.getPrivacy());
        newRoom.setOwnerId(roomInfo.getOwnerId());
        this.roomListModel.add(newRoom);
    }

    private YaccRoomInfo getRoomInfo(final Room room) {
        DiscoverInfo info;
        try {
            info = ServiceDiscoveryManager.getInstanceFor(this.connection).discoverInfo(room.getRoomId());
        }
        catch (XMPPException exception) {
            throw new RuntimeException(exception);
        }
        return new YaccRoomInfo(info);
    }

    // TODO: Stop leaking Smack classes to the rest of the application
    public MultiUserChat joinRoom(final Room room) {
        this.chatListModel.add(room);
        return new MultiUserChat(this.connection, room.getRoomId());
    }

//    public MultiUserChat createInstantRoom(final String name) {
//        String roomId = name.replaceAll("[&<>@]", "");
//        roomId = roomId.replaceAll(" ", "_");
//        roomId = this.hipChatClientPrefix + "_" + roomId.toLowerCase(Locale.getDefault()) + "@conf.hipchat.com";
//
//        final MultiUserChat multiUserChat = joinRoom(roomId);
//        try {
//            multiUserChat.create(this.currentUser.getName());
//            // Send an empty room configuration form which indicates that we want
//            // an instant room
//            multiUserChat.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
//        }
//        catch (XMPPException exception) {
//            throw new RuntimeException(exception);
//        }
//
//        return multiUserChat;
//    }

    public Chat createChat(final User user) {
        this.chatListModel.add(user);
        return this.connection.getChatManager().createChat(user.getUserId(), null);
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    public ChatTarget getCurrentChatTarget() {
        return currentChatTarget;
    }

    public void setCurrentChatTarget(ChatTarget currentChatTarget) {
        if (this.currentChatTarget != currentChatTarget) {

            if (null != this.currentChatTarget) {
                this.currentChatTarget.removePropertyChangeListener(ChatTarget.TITLE_PROPERTY_NAME, this.titleListener);
            }

            final ChatTarget oldChatTarget = this.currentChatTarget;
            final String oldChatTitle = getCurrentChatTargetTitle();
            final CustomMessageListModel oldMessageList = getCurrentChatTargetMessagesList();
            final UserListModel oldOccupants = getCurrentChatTargetOccupants();

            this.currentChatTarget = currentChatTarget;

            if (null != this.currentChatTarget) {
                this.currentChatTarget.addPropertyChangeListener(ChatTarget.TITLE_PROPERTY_NAME, this.titleListener);
            }

            // TODO: Remove the need to fire all of these here - this doesn't seem right
            firePropertyChange(CURRENT_CHAT_TARGET_PROPERTY_NAME, oldChatTarget, currentChatTarget);
            firePropertyChange(CURRENT_CHAT_TARGET_TITLE_PROPERTY_NAME, oldChatTitle, currentChatTarget.getTitle());
            firePropertyChange(CURRENT_CHAT_TARGET_MESSAGES_LIST_PROPERTY_NAME, oldMessageList, currentChatTarget.getCustomMessageListModel());
            firePropertyChange(CURRENT_CHAT_TARGET_OCCUPANTS_PROPERTY_NAME, oldOccupants, currentChatTarget.getOccupantsModel());
        }
    }

    private class TitlePropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            firePropertyChange(CURRENT_CHAT_TARGET_TITLE_PROPERTY_NAME, event.getOldValue(), event.getNewValue());
        }
    }

    public void setPresence(final UserStatus userStatus) {
        final Presence presence = new Presence(Presence.Type.available);
        presence.setMode(userStatus.getMode());
        connection.sendPacket(presence);
    }

    public String getCurrentChatTargetTitle() {
        if (null == currentChatTarget) {
            return null;
        }

        return currentChatTarget.getTitle();
    }

    public CustomMessageListModel getCurrentChatTargetMessagesList() {
        if (null == currentChatTarget) {
            return null;
        }

        return currentChatTarget.getCustomMessageListModel();
    }

    public UserListModel getCurrentChatTargetOccupants() {
        if (null == currentChatTarget) {
            return null;
        }

        return currentChatTarget.getOccupantsModel();
    }
}
