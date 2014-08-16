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
package uk.co.rjsoftware.xmpp.dialogs.notification;

import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.CustomMessage;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.view.Colours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class NotificationForm extends JDialog {

    public NotificationForm(final ChatTarget chatTarget, final CustomMessage message) {
        setSize(200, 75);
        setUndecorated(true);
        setLayout(new GridBagLayout());

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setBackground(Colours.DARK_ORANGE);
        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1.0f, 0f, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        add(headerPanel, constraints);

        JLabel headerLabel = new JLabel("YACC");
        headerLabel.setOpaque(false);
        final Font font = headerLabel.getFont();
        final Font newFont = new Font(font.getName(), Font.BOLD, font.getSize());
        headerLabel.setFont(newFont);
        constraints = new GridBagConstraints(0, 0, 1, 1, 1.0f, 0f, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0);
        headerPanel.add(headerLabel, constraints);

        JButton closeButton = new JButton(new AbstractAction("x") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                NotificationForm.this.dispose();
            }
        });
        closeButton.setMargin(new Insets(1, 4, 1, 4));
        closeButton.setFocusable(false);
        constraints = new GridBagConstraints(1, 0, 1, 1, 0f, 0f, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);
        headerPanel.add(closeButton, constraints);

        String sender;
        if (chatTarget instanceof Room) {
            sender = chatTarget.getName() + ": " + message.getSender();
        }
        else {
            sender = message.getSender();
        }

        JLabel senderLabel = new JLabel(sender);
        senderLabel.setIcon(chatTarget.getStatusIcon());
        senderLabel.setOpaque(false);
        senderLabel.setFont(newFont);
        constraints = new GridBagConstraints(0, 1, 1, 1, 1.0f, 0f, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0);
        add(senderLabel, constraints);

        JLabel messageLabel = new JLabel(message.getBody());
        constraints = new GridBagConstraints(0, 2, 1, 1, 1.0f, 1.0f, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0);
        add(messageLabel, constraints);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // move to bottom right of screen.
        Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
        Insets toolHeight = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration()); // height of the task bar
        setLocation(scrSize.width - getWidth(), scrSize.height - toolHeight.bottom - getHeight());

        setAlwaysOnTop(true);

        setVisible(true);

//        // close after 5 seconds
//        new Thread(){
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(5000);
//                    NotificationForm.this.dispose();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            };
//        }.start();
    }

}
