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
package uk.co.rjsoftware.xmpp.dialogs.main;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import uk.co.rjsoftware.xmpp.client.YaccProperties;
import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;
import uk.co.rjsoftware.xmpp.model.hipchat.room.HipChatRoom;
import uk.co.rjsoftware.xmpp.view.CurrentChatOccupantsCellRenderer;
import uk.co.rjsoftware.xmpp.view.MessageListCellRenderer;
import uk.co.rjsoftware.xmpp.view.RecentChatListCellRenderer;
import uk.co.rjsoftware.xmpp.view.RoomListCellRenderer;
import uk.co.rjsoftware.xmpp.view.UserListCellRenderer;
import uk.co.rjsoftware.xmpp.client.CustomConnection;
import uk.co.rjsoftware.xmpp.dialogs.createroom.CreateRoomForm;
import uk.co.rjsoftware.xmpp.dialogs.createroom.NewRoomListener;
import uk.co.rjsoftware.xmpp.dialogs.settings.SettingsForm;
import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.CustomMessage;
import uk.co.rjsoftware.xmpp.model.LogoutListener;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.User;
import uk.co.rjsoftware.xmpp.model.UserStatus;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class MainForm extends JFrame {

    private final JTabbedPane chatSourceTabs;

    private final JList<Room> roomList;
    private final JScrollPane roomListScrollPane;
    private final JList<User> userList;
    private final JScrollPane userListScrollPane;
    private final JList<User> chatList;
    private final JScrollPane chatListScrollPane;

    private final JPanel chatPanel;

    private final JPanel chatHeaderPanel;
    private final JLabel chatTitleLabel;
    private final JList chatOccupantsList;
    private final JScrollPane chatOccupantsScrollPane;

    private final JList<CustomMessage> messageList;
    private final JScrollPane messageListScrollPane;
    private final JTextArea message;
//    private final JButton newRoomButton;

    private final java.util.List<LogoutListener> listeners = new ArrayList<LogoutListener>();

    public MainForm(final String title, final CustomConnection connection, final YaccProperties yaccProperties) {
        super(title);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                connection.disconnect();
            }
        });

        final Container pane = getContentPane();

        this.chatSourceTabs = new JTabbedPane();

        final BeanAdapter adapter = new BeanAdapter(connection, true);

        //Add the list of rooms.
        final ValueModel roomListModel = adapter.getValueModel(CustomConnection.ROOM_LIST_MODEL_PROPERTY_NAME);
        this.roomList = BasicComponentFactory.createList(new SelectionInList(roomListModel), new RoomListCellRenderer());
        this.roomListScrollPane = new JScrollPane(roomList);
        this.chatSourceTabs.addTab("Rooms", this.roomListScrollPane);

        //Add the list of users.
        final ValueModel userListModel = adapter.getValueModel(CustomConnection.USER_LIST_MODEL_PROPERTY_NAME);
        this.userList = BasicComponentFactory.createList(new SelectionInList<Object>(userListModel), new UserListCellRenderer());
        this.userListScrollPane = new JScrollPane(userList);
        this.chatSourceTabs.addTab("Users", this.userListScrollPane);

        //Add the list of recent chats.
        final ValueModel chatListModel = adapter.getValueModel(CustomConnection.CHAT_LIST_MODEL_PROPERTY_NAME);
        this.chatList = BasicComponentFactory.createList(new SelectionInList(chatListModel), new RecentChatListCellRenderer());
        this.chatListScrollPane = new JScrollPane(chatList);
        this.chatSourceTabs.addTab("Recent", this.chatListScrollPane);

        pane.add(this.chatSourceTabs, BorderLayout.LINE_START);

        // create a panel to contain the current chat information
        this.chatPanel = new JPanel();
        this.chatPanel.setLayout(new BorderLayout());
        pane.add(this.chatPanel, BorderLayout.CENTER);

        // create a panel for the chat header
        this.chatHeaderPanel = new JPanel();
        this.chatHeaderPanel.setLayout(new BorderLayout());
        this.chatPanel.add(this.chatHeaderPanel, BorderLayout.PAGE_START);

        // create a panel for the chat title
        final JPanel chatTitlePanel = new JPanel();
        chatTitlePanel.setLayout(new BorderLayout());
        this.chatHeaderPanel.add(chatTitlePanel, BorderLayout.PAGE_START);

        // add a label for the room / chat title into the chat header.
        final ValueModel currentChatTargetTitleModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_TITLE_PROPERTY_NAME);
        this.chatTitleLabel = BasicComponentFactory.createLabel(currentChatTargetTitleModel);
        chatTitlePanel.add(this.chatTitleLabel, BorderLayout.LINE_START);

        // add a room settings icon and handler
        final JPopupMenu settingsPopupMenu = new JPopupMenu();
        final JMenuItem inviteUsersMenuItem = new JMenuItem("Invite Users...");
        final JMenuItem changeTopicMenuItem = new JMenuItem("Change Topic");
        final JMenuItem renameRoomMenuItem = new JMenuItem("Rename...");
        final JMenuItem deleteRoomMenuItem = new JMenuItem("Delete...");

        inviteUsersMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // TODO: Display invite user dialog
            }
        });

        changeTopicMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // TODO: Allow user to change the room subject
            }
        });

        renameRoomMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // Display rename room dialog
                final String result = (String)JOptionPane.showInputDialog(MainForm.this, "Enter the new name for the room:",
                        "Rename Room", JOptionPane.PLAIN_MESSAGE, null, null, connection.getCurrentChatTarget().getTitle());

                if ((null != result) && (result.length() > 0)) {
                    HipChatRoom hipChatRoom = new HipChatRoom(yaccProperties);
                    hipChatRoom.renameRoom((Room)connection.getCurrentChatTarget(), result, connection.getHipChatUser());

                    final Room room = connection.getRoomListModel().get(((Room)connection.getCurrentChatTarget()).getRoomId());
                    if (room != null) {
                        room.setName(result);
                    }
                }
            }
        });

        deleteRoomMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // Display delete room dialog
                final Object[] options = {"Delete", "Cancel"};
                final int selectedOption = JOptionPane.showOptionDialog(MainForm.this, "Are you sure you want to delete the room?",
                                            "Delete Room", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                            options, options[1]);
                if (0 == selectedOption) {
                    connection.deleteCurrentChatTarget();
                }
            }
        });

        settingsPopupMenu.add(inviteUsersMenuItem);
        settingsPopupMenu.addSeparator();
        settingsPopupMenu.add(changeTopicMenuItem);
        settingsPopupMenu.add(renameRoomMenuItem);
        settingsPopupMenu.addSeparator();
        settingsPopupMenu.add(deleteRoomMenuItem);

        final JButton roomSettingsButton = new JButton("settings");
        chatTitlePanel.add(roomSettingsButton, BorderLayout.LINE_END);
        roomSettingsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowPopup(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowPopup(event);
            }

            private void maybeShowPopup(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    settingsPopupMenu.show(event.getComponent(),
                            0, roomSettingsButton.getHeight());
                }
            }
        });
        // by default the settings button is not displayed, as the app starts with no chat open
        roomSettingsButton.setVisible(false);
        // add a listener to the current chat target that shows/hides the settings menu and enables/disables the items as appropriate
        final ValueModel currentChatTargetModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_PROPERTY_NAME);
        currentChatTargetModel.addValueChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getNewValue() instanceof Room) {
                    final Room currentRoom = (Room)event.getNewValue();
                    final boolean isOwner = connection.getCurrentUser().getUserId().equals(currentRoom.getOwnerId());
                    roomSettingsButton.setVisible(true);
                    renameRoomMenuItem.setEnabled(isOwner);
                    deleteRoomMenuItem.setEnabled(isOwner);
                }
                else {
                    roomSettingsButton.setVisible(false);
                }
            }
        });

        // add a list for the list of room occupants
        // TODO: Stop the horizontal scroll bar from hiding the bottom column entries
        // TODO: Display a different view if in a private chat (as there will only be one user to display)
        final ValueModel currentChatTargetOccupantsModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_OCCUPANTS_PROPERTY_NAME);
        this.chatOccupantsList = BasicComponentFactory.createList(new SelectionInList(currentChatTargetOccupantsModel), new CurrentChatOccupantsCellRenderer());
        this.chatOccupantsList.setLayoutOrientation(JList.VERTICAL_WRAP);
        this.chatOccupantsList.setVisibleRowCount(4);
        this.chatOccupantsScrollPane = new JScrollPane(this.chatOccupantsList);
        this.chatOccupantsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        this.chatHeaderPanel.add(this.chatOccupantsScrollPane, BorderLayout.CENTER);

        //Add the message history window
        final ValueModel messagesListModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_MESSAGES_LIST_PROPERTY_NAME);
        this.messageList = new JList<CustomMessage>() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        Bindings.bind(this.messageList, new SelectionInList(messagesListModel));

        this.messageList.setCellRenderer(new MessageListCellRenderer());
        this.messageList.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // next line possible if list is of type JXList
                // MainForm.this.messageList.invalidateCellSizeCache();
                // for core: force cache invalidation by temporarily setting fixed height
                MainForm.this.messageList.setFixedCellHeight(10);
                MainForm.this.messageList.setFixedCellHeight(-1);
            }
        });
        this.messageListScrollPane = new AutoScrollPane(messageList);
        this.chatPanel.add(this.messageListScrollPane, BorderLayout.CENTER);

        // Add the message window
        this.message = new JTextArea();
        this.message.setLineWrap(true);
        this.chatPanel.add(this.message, BorderLayout.PAGE_END);

//        // Add the create room button
//        this.newRoomButton = new JButton("Create room");
//        pane.add(this.newRoomButton, BorderLayout.PAGE_START);

        this.roomList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // update the chat target
                    final ChatTarget chatTarget = MainForm.this.roomList.getSelectedValue();
                    connection.setCurrentChatTarget(chatTarget);
                    connection.getCurrentChatTarget().join(connection);
                    MainForm.this.chatList.setSelectedValue(chatTarget, true);
                    // switch to the 'recent' tab
                    MainForm.this.chatSourceTabs.setSelectedIndex(2);
                }
            }
        });

        this.userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // update the chat target
                    final ChatTarget chatTarget = MainForm.this.userList.getSelectedValue();
                    connection.setCurrentChatTarget(chatTarget);
                    connection.getCurrentChatTarget().join(connection);
                    MainForm.this.chatList.setSelectedValue(chatTarget, true);
                    // switch to the 'recent' tab
                    MainForm.this.chatSourceTabs.setSelectedIndex(2);
                }
            }
        });

        this.chatList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                // update the chat target
                connection.setCurrentChatTarget(MainForm.this.chatList.getSelectedValue());
                connection.getCurrentChatTarget().join(connection);
            }
        });

        this.message.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if ((event.getKeyCode() == 13) || (event.getKeyCode() == 10)) {
                    connection.getCurrentChatTarget().sendMessage(MainForm.this.message.getText());
                    MainForm.this.message.setText("");
                    // stop the carriage return from appearing in the text area.
                    event.consume();
                }
            }
        });

//        this.newRoomButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                // TODO: really should do this via the Room class
//                connection.createInstantRoom(MainForm.this.message.getText());
//            }
//        });


        // create the menu
        final JMenuBar menuBar = new JMenuBar();
        final JMenu menu = new JMenu("YACC");

        final JMenuItem createRoomMenuItem = new JMenuItem("Create room...");
        menu.add(createRoomMenuItem);
        createRoomMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final CreateRoomForm createRoomForm = new CreateRoomForm(yaccProperties);
                createRoomForm.addNewRoomListener(new NewRoomListener() {
                    @Override
                    public void onNewRoom(Room newRoom) {
                        connection.addRoom(newRoom);
                        // update the chat target
                        connection.setCurrentChatTarget(newRoom);
                        connection.getCurrentChatTarget().join(connection);
                        MainForm.this.chatList.setSelectedValue(newRoom, true);
                        // switch to the 'recent' tab
                        MainForm.this.chatSourceTabs.setSelectedIndex(2);
                    }
                });
                createRoomForm.setVisible(true);

            }
        });
        final JMenu submenu = new JMenu("Set Status");
        menu.add(submenu);
        final JMenuItem availableMenuItem = new JMenuItem("Available");
        final JMenuItem awayMenuItem = new JMenuItem("Away");
        final JMenuItem doNotDisturbMenuItem = new JMenuItem("Do Not Disturb");
        submenu.add(availableMenuItem);
        submenu.add(awayMenuItem);
        submenu.add(doNotDisturbMenuItem);
        availableMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connection.setPresence(UserStatus.AVAILABLE);
            }
        });
        awayMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connection.setPresence(UserStatus.AWAY);
            }
        });
        doNotDisturbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connection.setPresence(UserStatus.DO_NOT_DISTURB);
            }
        });

        final JMenuItem settingsMenuItem = new JMenuItem("Settings...");
        menu.addSeparator();
        menu.add(settingsMenuItem);
        settingsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SettingsForm settingsForm = new SettingsForm(yaccProperties);
                settingsForm.setVisible(true);
            }
        });

        final JMenuItem signOutMenuItem = new JMenuItem("Sign Out");
        menu.addSeparator();
        menu.add(signOutMenuItem);
        signOutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connection.disconnect();
                fireLogout();
            }
        });

        menuBar.add(menu);
        setJMenuBar(menuBar);

        setPreferredSize(new Dimension(1150, 512));
        pack();

        DialogUtils.centerDialog(this);
    }

    private void fireLogout() {
        for (LogoutListener listener : this.listeners) {
            listener.logout();
        }
    }

    public void addLogoutListener(final LogoutListener listener) {
        this.listeners.add(listener);
    }

    private static class AutoScrollPane extends JScrollPane implements ChangeListener {

        private int lastScrollBarValueWhenAtBottom;

        public AutoScrollPane(final JComponent component) {
            super(component);
            component.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    final BoundedRangeModel model = getVerticalScrollBar().getModel();
                    if (model.getValue() == lastScrollBarValueWhenAtBottom) {
                        // scroll bar hasn't been moved since it was last at the botton, so move
                        // it down to the bottom again
                        model.setValue(model.getMaximum());
                        super.componentResized(e);
                    }
                }
            });

            getVerticalScrollBar().getModel().addChangeListener(this);
        }

        @Override
        public void stateChanged(ChangeEvent event) {
            final BoundedRangeModel model = (BoundedRangeModel)event.getSource();
            final boolean isAtBottom = model.getValue() + model.getExtent() == model.getMaximum();
            if (isAtBottom) {
                lastScrollBarValueWhenAtBottom = model.getValue();
            }
        }
    }

}
