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
package uk.co.rjsoftware.xmpp;

import uk.co.rjsoftware.xmpp.client.CustomConnection;
import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.CustomMessage;
import uk.co.rjsoftware.xmpp.model.LogoutListener;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.User;

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
    private final JPanel chatPanel;
    private final JList<CustomMessage> messageList;
    private final JScrollPane messageListScrollPane;
    private final JTextArea message;
//    private final JButton newRoomButton;
    private final JLabel chatTitle;
    private final TitleListener titleListener;

    private final java.util.List<LogoutListener> listeners = new ArrayList<LogoutListener>();

    private ChatTarget currentChatTarget;

    public MainForm(final String title, final CustomConnection connection) {
        super(title);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                connection.disconnect();
            }
        });

        final Container pane = getContentPane();

        this.chatSourceTabs = new JTabbedPane();

        //Add the list of rooms.
        this.roomList = new JList<Room>(connection.getRoomListModel());
        this.roomListScrollPane = new JScrollPane(roomList);
        this.chatSourceTabs.addTab("Rooms", this.roomListScrollPane);

        //Add the list of users.
        this.userList = new JList<User>(connection.getUserListModel());
        this.userList.setCellRenderer(new UserListCellRenderer());
        this.userListScrollPane = new JScrollPane(userList);
        this.chatSourceTabs.addTab("Users", this.userListScrollPane);

        pane.add(this.chatSourceTabs, BorderLayout.LINE_START);

        // create a panel to contain the current chat information
        this.chatPanel = new JPanel();
        this.chatPanel.setLayout(new BorderLayout());
        pane.add(this.chatPanel, BorderLayout.CENTER);

        // add a label for the room / chat title
        this.chatTitle = new JLabel();
        this.chatPanel.add(this.chatTitle, BorderLayout.PAGE_START);

        //Add the message history window
        this.messageList = new JList<CustomMessage>() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
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

        this.titleListener = new TitleListener(this.chatTitle);

        this.roomList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // update the chat target
                    if (MainForm.this.currentChatTarget != null) {
                        MainForm.this.currentChatTarget.removeTitleListener(MainForm.this.titleListener);
                    }
                    MainForm.this.currentChatTarget = MainForm.this.roomList.getSelectedValue();
                    MainForm.this.chatTitle.setText(MainForm.this.currentChatTarget.getTitle());
                    MainForm.this.currentChatTarget.addTitleListener(MainForm.this.titleListener);
                    MainForm.this.currentChatTarget.join(connection);
                    MainForm.this.messageList.setModel(MainForm.this.currentChatTarget.getCustomMessageListModel());
                }
            }
        });

        this.userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // update the chat target
                    if (MainForm.this.currentChatTarget != null) {
                        MainForm.this.currentChatTarget.removeTitleListener(MainForm.this.titleListener);
                    }
                    MainForm.this.currentChatTarget = MainForm.this.userList.getSelectedValue();
                    MainForm.this.chatTitle.setText(MainForm.this.currentChatTarget.getTitle());
                    MainForm.this.currentChatTarget.addTitleListener(MainForm.this.titleListener);
                    MainForm.this.currentChatTarget.join(connection);
                    MainForm.this.messageList.setModel(MainForm.this.currentChatTarget.getCustomMessageListModel());
                }
            }
        });

        this.message.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if ((event.getKeyCode() == 13) || (event.getKeyCode() == 10)) {
                    MainForm.this.currentChatTarget.sendMessage(MainForm.this.message.getText());
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
        final JMenuItem menuItem = new JMenuItem("Sign Out");
        menu.add(menuItem);
        menuBar.add(menu);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connection.disconnect();
                fireLogout();
            }
        });

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

    private static class TitleListener implements PropertyChangeListener {

        private final JLabel titleLabel;

        public TitleListener(final JLabel titleLabel) {
            this.titleLabel = titleLabel;
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            this.titleLabel.setText((String)event.getNewValue());
        }
    }

}
