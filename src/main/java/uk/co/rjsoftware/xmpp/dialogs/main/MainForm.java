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
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import uk.co.rjsoftware.xmpp.client.CustomConnection;
import uk.co.rjsoftware.xmpp.client.YaccInvitationListener;
import uk.co.rjsoftware.xmpp.client.YaccProperties;
import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;
import uk.co.rjsoftware.xmpp.dialogs.createroom.CreateRoomForm;
import uk.co.rjsoftware.xmpp.dialogs.createroom.NewRoomListener;
import uk.co.rjsoftware.xmpp.dialogs.inviteusers.InviteUsersForm;
import uk.co.rjsoftware.xmpp.dialogs.settings.SettingsForm;
import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.LogoutListener;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.User;
import uk.co.rjsoftware.xmpp.model.UserStatus;
import uk.co.rjsoftware.xmpp.model.hipchat.emoticons.HipChatEmoticons;
import uk.co.rjsoftware.xmpp.model.hipchat.room.HipChatRoom;
import uk.co.rjsoftware.xmpp.view.CurrentChatOccupantsCellRenderer;
import uk.co.rjsoftware.xmpp.view.RecentChatListCellRenderer;
import uk.co.rjsoftware.xmpp.view.RoomListCellRenderer;
import uk.co.rjsoftware.xmpp.view.UserListCellRenderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.ParagraphView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;

public class MainForm extends JFrame {

    private JPanel chatTitlePanel;
    private JPanel chatTitleEditModePanel;
    private JPanel chatTitleViewModePanel;
    private JTextField chatTitleTextField;
    private JLabel chatTitleLabel;

    private final CustomConnection connection;
    private final YaccProperties yaccProperties;
    private final YaccPropertyChangeListener yaccPropertiesChangeListener;
    private final BeanAdapter adapter;

    private final HipChatEmoticons hipChatEmoticons;

    private final java.util.List<LogoutListener> listeners = new ArrayList<LogoutListener>();
    private ChatTarget currentChatTarget;

    public MainForm(final String title, final CustomConnection connection, final YaccProperties yaccProperties,
                    final HipChatEmoticons hipChatEmoticons) {
        super(title);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                connection.disconnect();
            }
        });

        this.connection = connection;
        this.yaccProperties = yaccProperties;
        this.hipChatEmoticons = hipChatEmoticons;

        this.yaccPropertiesChangeListener = new YaccPropertyChangeListener(this.yaccProperties, this.hipChatEmoticons);
        this.yaccProperties.addPropertyChangeListener(YaccProperties.PROPERTY_NAME_HIPCHAT_API_AUTH_TOKEN, this.yaccPropertiesChangeListener);
        this.yaccProperties.addPropertyChangeListener(YaccProperties.PROPERTY_NAME_HIPCHAT_API_ENDPOINT, this.yaccPropertiesChangeListener);

        this.adapter = new BeanAdapter(connection, true);

        JTabbedPane chatSourceTabs = new JTabbedPane();
        JList<ChatTarget> chatList = setupTabbedPane(chatSourceTabs);

        setupCurrentChatComponents();

        setupMainMenu(chatSourceTabs, chatList);

        setupInvitationListener(chatSourceTabs, chatList);

        setPreferredSize(new Dimension(1150, 512));
        pack();

        DialogUtils.centerDialog(this);
    }

    @Override
    public void dispose() {
        this.yaccProperties.removePropertyChangeListener(YaccProperties.PROPERTY_NAME_HIPCHAT_API_AUTH_TOKEN, this.yaccPropertiesChangeListener);
        this.yaccProperties.removePropertyChangeListener(YaccProperties.PROPERTY_NAME_HIPCHAT_API_ENDPOINT, this.yaccPropertiesChangeListener);
        this.connection.saveRecentChats();

        super.dispose();

        for (LogoutListener listener : this.listeners) {
            listener.logout();
        }
    }

    private static class YaccPropertyChangeListener implements PropertyChangeListener {

        private final YaccProperties yaccProperties;
        private final HipChatEmoticons hipChatEmoticons;

        public YaccPropertyChangeListener(final YaccProperties yaccProperties, final HipChatEmoticons hipChatEmoticons) {
            this.yaccProperties = yaccProperties;
            this.hipChatEmoticons = hipChatEmoticons;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            this.hipChatEmoticons.updateEmoticons();
        }
    }

    private JList<ChatTarget> setupTabbedPane(final JTabbedPane chatSourceTabs) {
        //Add the list of rooms.
        final ValueModel roomListModel = adapter.getValueModel(CustomConnection.ROOM_LIST_MODEL_PROPERTY_NAME);
        final JList<Room> roomList = BasicComponentFactory.createList(new SelectionInList(roomListModel), new RoomListCellRenderer());
        final JScrollPane roomListScrollPane = new JScrollPane(roomList);
        chatSourceTabs.addTab("Rooms", roomListScrollPane);

        //Add the list of users.
        final ValueModel userListModel = adapter.getValueModel(CustomConnection.USER_LIST_MODEL_PROPERTY_NAME);
        final JList<User> userList = BasicComponentFactory.createList(new SelectionInList<Object>(userListModel), new UserListCellRenderer());
        final JScrollPane userListScrollPane = new JScrollPane(userList);
        chatSourceTabs.addTab("Users", userListScrollPane);

        //Add the list of recent chats.
        final ValueModel chatListModel = adapter.getValueModel(CustomConnection.CHAT_LIST_MODEL_PROPERTY_NAME);
        final JList<ChatTarget> chatList = BasicComponentFactory.createList(new SelectionInList(chatListModel), new RecentChatListCellRenderer());
        final JScrollPane chatListScrollPane = new JScrollPane(chatList);
        chatSourceTabs.addTab("Recent", chatListScrollPane);

        getContentPane().add(chatSourceTabs, BorderLayout.LINE_START);

        roomList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // update the chat target
                    final ChatTarget chatTarget = roomList.getSelectedValue();
                    connection.setCurrentChatTarget(chatTarget);
                    connection.getCurrentChatTarget().join(connection);
                    chatList.setSelectedValue(chatTarget, true);
                    // switch to the 'recent' tab
                    chatSourceTabs.setSelectedIndex(2);
                }
            }
        });

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // update the chat target
                    final ChatTarget chatTarget = userList.getSelectedValue();
                    connection.setCurrentChatTarget(chatTarget);
                    connection.getCurrentChatTarget().join(connection);
                    chatList.setSelectedValue(chatTarget, true);
                    // switch to the 'recent' tab
                    chatSourceTabs.setSelectedIndex(2);
                }
            }
        });

        chatList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                // update the chat target
                connection.setCurrentChatTarget(chatList.getSelectedValue());
                connection.getCurrentChatTarget().join(connection);
            }
        });

        return chatList;
    }

    private void setupCurrentChatComponents() {
        // create a panel to contain the current chat information
        final JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        getContentPane().add(chatPanel, BorderLayout.CENTER);

        // create a panel for the chat header
        final JPanel chatHeaderPanel = new JPanel();
        chatHeaderPanel.setLayout(new BorderLayout());
        chatPanel.add(chatHeaderPanel, BorderLayout.PAGE_START);

        setupChatHeader(chatHeaderPanel);

        // add a list for the list of room occupants
        // TODO: Stop the horizontal scroll bar from hiding the bottom column entries
        // TODO: Display a different view if in a private chat (as there will only be one user to display)
        final ValueModel currentChatTargetOccupantsModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_OCCUPANTS_PROPERTY_NAME);
        final JList chatOccupantsList = BasicComponentFactory.createList(new SelectionInList(currentChatTargetOccupantsModel), new CurrentChatOccupantsCellRenderer());
        chatOccupantsList.setLayoutOrientation(JList.VERTICAL_WRAP);
        chatOccupantsList.setVisibleRowCount(4);
        final JScrollPane chatOccupantsScrollPane = new JScrollPane(chatOccupantsList);
        chatOccupantsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        chatHeaderPanel.add(chatOccupantsScrollPane, BorderLayout.CENTER);

        //Add the message history window
        final ValueModel messagesListModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_MESSAGES_DOCUMENT_PROPERTY_NAME);
        final JTextPane messageTextPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        messageTextPane.setEditable(false);
        messageTextPane.setContentType("text/html");
        messageTextPane.setEditorKit(new WrapHTMLEditorKit());
        messageTextPane.addHyperlinkListener(new HyperlinkActivator());

        //Bindings.bind(messageList, new SelectionInList(messagesListModel));

//        messageList.setCellRenderer(new MessageListCellRenderer());
//        messageList.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                // next line possible if list is of type JXList
//                // MainForm.this.messageList.invalidateCellSizeCache();
//                // for core: force cache invalidation by temporarily setting fixed height
//                messageList.setFixedCellHeight(10);
//                messageList.setFixedCellHeight(-1);
//            }
//        });
        final JScrollPane messageListScrollPane = new AutoScrollPane(messageTextPane);
        chatPanel.add(messageListScrollPane, BorderLayout.CENTER);

        // ensure that the messages get marked as read when appropriate
        MessageStateChanger stateChanger = new MessageStateChanger();
        stateChanger.addChangeListener(messageListScrollPane, this);

        messagesListModel.addValueChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (null == event.getNewValue()) {
                    messageTextPane.setStyledDocument(new HTMLDocument());
                }
                else {
                    messageTextPane.setStyledDocument((StyledDocument)event.getNewValue());
                }
            }
        });
        // Add the message window
        final JTextArea message = new JTextArea();
        message.setLineWrap(true);
        message.setFont(chatOccupantsList.getFont());
        chatPanel.add(message, BorderLayout.PAGE_END);

        final String textSubmit = "text-submit";
        final String insertBreak = "insert-break";

        InputMap input = message.getInputMap();
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        KeyStroke shiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK);
        input.put(shiftEnter, insertBreak);  // input.get(enter)) = "insert-break"
        input.put(enter, textSubmit);

        ActionMap actions = message.getActionMap();
        actions.put(textSubmit, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (connection.getCurrentChatTarget() != null) {
                    connection.getCurrentChatTarget().sendMessage(message.getText());
                    message.setText("");
                }
            }
        });

    }

    private void setupChatHeader(final JPanel chatHeaderPanel) {
        // create a panel for the room / chat title
        this.chatTitlePanel = new JPanel();
        this.chatTitlePanel.setLayout(new BorderLayout());
        chatHeaderPanel.add(this.chatTitlePanel, BorderLayout.PAGE_START);

        // create a panel for the room / chat title (view mode)
        this.chatTitleViewModePanel = new JPanel();
        this.chatTitleViewModePanel.setLayout(new BorderLayout());
        this.chatTitlePanel.add(this.chatTitleViewModePanel, BorderLayout.PAGE_START);

        // add a label for the room / chat title into the chat header.
        final ValueModel currentChatTargetTitleModel = adapter.getValueModel(CustomConnection.CURRENT_CHAT_TARGET_TITLE_PROPERTY_NAME);
        this.chatTitleLabel = BasicComponentFactory.createLabel(currentChatTargetTitleModel);
        this.chatTitleViewModePanel.add(this.chatTitleLabel, BorderLayout.LINE_START);

        // add a room settings icon and handler
        final JPopupMenu settingsPopupMenu = new JPopupMenu();
        final JMenuItem inviteUsersMenuItem = new JMenuItem("Invite Users...");
        final JMenuItem changeTopicMenuItem = new JMenuItem("Change Topic");
        final JMenuItem renameRoomMenuItem = new JMenuItem("Rename...");
        final JMenuItem deleteRoomMenuItem = new JMenuItem("Delete...");

        inviteUsersMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                // Display invite user dialog
                final InviteUsersForm inviteUsersForm = new InviteUsersForm(connection);
                inviteUsersForm.setVisible(true);
            }
        });

        changeTopicMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showChatTitleEditMode();
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
                    MainForm.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        hipChatRoom.renameRoom((Room)connection.getCurrentChatTarget(), result, connection.getHipChatUser());
                    } finally {
                        MainForm.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
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
        this.chatTitleViewModePanel.add(roomSettingsButton, BorderLayout.LINE_END);
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
                MainForm.this.currentChatTarget = (ChatTarget)event.getNewValue();
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

        // create a panel for the room / chat title (edit mode)
        this.chatTitleEditModePanel = new JPanel();
        this.chatTitleEditModePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        this.chatTitleTextField = new JTextField();
        this.chatTitleTextField.setColumns(40);
        this.chatTitleEditModePanel.add(this.chatTitleTextField);

        final JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(70, 20));
        this.chatTitleEditModePanel.add(okButton);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Update the current room's subject
                ((Room)MainForm.this.currentChatTarget).setSubject(MainForm.this.chatTitleTextField.getText());
                showChatTitleViewMode();
            }
        });

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(70, 20));
        this.chatTitleEditModePanel.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showChatTitleViewMode();
            }
        });

        // add a mouse listener to the 'title label', to trigger the switch to the editable version
        this.chatTitleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if ((currentChatTarget instanceof Room) && (event.getClickCount() == 2)) {
                    showChatTitleEditMode();
                }
            }
        });

    }

    private void setupInvitationListener(final JTabbedPane chatSourceTabs, final JList<ChatTarget> chatList) {
        this.connection.addInvitationListener(new YaccInvitationListener() {
            @Override
            public void invitationReceived(String roomJid, String roomName, String inviterJId, String inviterName, String reason, String password) {
                // Display delete room dialog
                final Object[] options = {"Join", "Cancel"};
                final int selectedOption = JOptionPane.showOptionDialog(MainForm.this,
                        "You have been invited to the room '" + roomName + "' by "  + inviterName + ".  Do you wish to join the room now?",
                        "Invite Received", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[1]);

                if (0 == selectedOption) {
                    // TODO: Fix the occupant list - it doesn't show up!
                    // check if the room is already known
                    Room room = connection.getRoomListModel().get(roomJid);
                    if (null == room) {
                        room = new Room(roomJid, roomName);
                        connection.addRoom(room);
                    }
                    // update the chat target
                    connection.setCurrentChatTarget(room);
                    connection.getCurrentChatTarget().join(connection);
                    chatList.setSelectedValue(room, true);
                    // switch to the 'recent' tab
                    chatSourceTabs.setSelectedIndex(2);
                }
            }
        });
    }

    private void showChatTitleViewMode() {
        this.chatTitlePanel.remove(this.chatTitleEditModePanel);
        this.chatTitlePanel.add(this.chatTitleViewModePanel, BorderLayout.PAGE_START);
        MainForm.this.pack();
    }

    private void showChatTitleEditMode() {
        this.chatTitleTextField.setText(this.chatTitleLabel.getText());
        this.chatTitlePanel.remove(this.chatTitleViewModePanel);
        this.chatTitlePanel.add(this.chatTitleEditModePanel, BorderLayout.PAGE_START);
        MainForm.this.pack();
    }

    private void setupMainMenu(final JTabbedPane chatSourceTabs, final JList<ChatTarget> chatList) {
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
                        chatList.setSelectedValue(newRoom, true);
                        // switch to the 'recent' tab
                        chatSourceTabs.setSelectedIndex(2);
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
                logout();
            }
        });

        menuBar.add(menu);
        setJMenuBar(menuBar);

    }

    private void logout() {
        connection.disconnect();
        dispose();
    }

    public void addLogoutListener(final LogoutListener listener) {
        this.listeners.add(listener);
    }

    public ChatTarget getCurrentChatTarget() {
        return this.currentChatTarget;
    }

    //TODO: Bug fix: when switch chats, the scroll bar doesn't switch to the last position for this chat
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
            // if state has changed such that the viewport is showing the last message, update the
            // cached value for the scrollBarValue (so we know if the viewport needs moving when the component
            // gets resized, e.g. by adding a new message)
            final BoundedRangeModel model = (BoundedRangeModel)event.getSource();
            final boolean isAtBottom = model.getValue() + model.getExtent() == model.getMaximum();
            if (isAtBottom) {
                lastScrollBarValueWhenAtBottom = model.getValue();
            }


        }
    }

    private static class HyperlinkActivator implements HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL url = event.getURL();

                DialogUtils.launchUrl(url.toString());
            }
        }
    }

    private static class WrapHTMLEditorKit extends HTMLEditorKit {
        final ViewFactory defaultFactory = new WrapHTMLFactory();

        @Override
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    private static class WrapHTMLFactory extends HTMLEditorKit.HTMLFactory {
        @Override
        public View create(Element elem) {
            View view = super.create(elem);

            if (view instanceof ParagraphView) {
                return new WrapParagraphView(elem);
            }

            final AttributeSet attrs = elem.getAttributes();
            final Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);

            Object obj = null;
            if (elementName == null) {
                obj = attrs.getAttribute(StyleConstants.NameAttribute);
            }

            if (obj instanceof HTML.Tag) {
                HTML.Tag kind = (HTML.Tag)obj;
                if (kind == HTML.Tag.CONTENT) {
                    return new InlineViewWithEllipsesSupport(elem);
                }
            }

            return view;
        }

    }

    private static class WrapParagraphView extends ParagraphView {

        public WrapParagraphView(final Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }

    }

    private static class InlineViewWithEllipsesSupport extends InlineView {

        private boolean nowrap;
        private FontMetrics fontMetrics;
        private Shape shape;

        public InlineViewWithEllipsesSupport(final Element elem) {
            super(elem);
        }

        /**
         * Set the cached properties from the attributes.
         */
        protected void setPropertiesFromAttributes() {
            super.setPropertiesFromAttributes();

            AttributeSet a = getAttributes();

            Object whitespace = a.getAttribute(CSS.Attribute.WHITE_SPACE);
            if ((whitespace != null) && whitespace.equals("nowrap")) {
                nowrap = true;
            } else {
                nowrap = false;
            }
        }

        public void paint(Graphics graphics, Shape shape) {
            this.fontMetrics = graphics.getFontMetrics(getFont());
            this.shape = shape;
            super.paint(graphics, shape);
        }

        @Override
        public Segment getText(int p0, int p1) {
            final Segment segment = super.getText(p0, p1);

            if (!this.nowrap) {
                return segment;
            }

            if ((this.shape == null) || (this.fontMetrics == null)) {
                return segment;
            }

            final double maxWidth = this.shape.getBounds().getWidth();
            if (maxWidth == 0) {
                return segment;
            }

            // get the width of the string
            String text = String.valueOf(segment.array, segment.offset, segment.count);
            int width = this.fontMetrics.stringWidth(text);

            if (width <= maxWidth) {
                return segment;
            }

            // continue making the string shorter until it fits (or is empty)
            while ((width > maxWidth) && (text.length() > 0)) {
                text = text.substring(0, text.length()-1);
                width = this.fontMetrics.stringWidth(text + "...");
            }

            if (text.length() == 0) {
                return segment;
            }

            // put text back into the segment

            text = text + "...";
            return new Segment(text.toCharArray(), 0, text.length());
        }

    }
}
