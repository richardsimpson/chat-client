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
package uk.co.rjsoftware.xmpp.view;

import uk.co.rjsoftware.xmpp.model.CustomMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageListCellRenderer implements ListCellRenderer<CustomMessage> {

    private static final int MESSAGE_BORDER_TOP = 10;
    private static final int SENDER_COLUMN_WIDTH = 100;
    private static final int TIME_COLUMN_WIDTH = 42;

    private JPanel mainPanel;
    private JPanel senderPanel;
    private JLabel senderLabel;
    private JPanel timePanel;
    private JLabel timeLabel;
    private JPanel messagePanel;
    private JTextArea messageTextArea;

    public MessageListCellRenderer() {
        this.mainPanel = new JPanel();
        this.mainPanel.setOpaque(false);
        this.mainPanel.setLayout(new BorderLayout());

        // sender
        this.senderPanel = new JPanel(new BorderLayout());
        this.senderPanel.setOpaque(false);
        this.senderPanel.setBorder(new EmptyBorder(MESSAGE_BORDER_TOP, 10, 0, 15));
        this.senderLabel = new JLabel();
        this.senderLabel.setOpaque(false);
        this.senderLabel.setPreferredSize(new Dimension(SENDER_COLUMN_WIDTH, 10));
        this.senderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.senderPanel.add(this.senderLabel, BorderLayout.PAGE_START);
        this.mainPanel.add(this.senderPanel, BorderLayout.LINE_START);

        // text
        this.messagePanel = new JPanel(new BorderLayout());
        this.messagePanel.setOpaque(false);
        // setting the border top to be 2 less, so it appears at the same height as the other columns :(
        this.messagePanel.setBorder(new EmptyBorder(MESSAGE_BORDER_TOP - 2, 0, 0, 15));
        this.messageTextArea = new JTextArea();
        this.messageTextArea.setBorder(null);
        this.messageTextArea.setOpaque(false);
        this.messageTextArea.setLineWrap(true);
        this.messageTextArea.setWrapStyleWord(true);
        this.messageTextArea.setEditable(false);
        this.messageTextArea.setFont(this.senderLabel.getFont());
        this.messagePanel.add(this.messageTextArea, BorderLayout.PAGE_START);
        this.mainPanel.add(this.messagePanel, BorderLayout.CENTER);

        // time
        this.timePanel = new JPanel(new BorderLayout());
        this.timePanel.setOpaque(false);
        this.timePanel.setBorder(new EmptyBorder(MESSAGE_BORDER_TOP, 0, 0, 10));
        this.timeLabel = new JLabel();
        this.timeLabel.setOpaque(false);
        this.timeLabel.setPreferredSize(new Dimension(TIME_COLUMN_WIDTH, 10));

        this.timePanel.add(this.timeLabel, BorderLayout.PAGE_START);
        this.mainPanel.add(this.timePanel, BorderLayout.LINE_END);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends CustomMessage> list, CustomMessage message, int index, boolean isSelected, boolean cellHasFocus) {
        // TODO: Use a thread-safe date formatter, and change it into a class attribute, so it doesn't need creating here
        final DateFormat formatter = new SimpleDateFormat("HH:mm");
        final Date date = new Date(message.getTimestamp());

        this.senderLabel.setText(message.getSender());
        this.timeLabel.setText(formatter.format(date));
        this.messageTextArea.setText(message.getBody());
        int width = this.messageTextArea.getWidth();

        // this is just to lure the text areas internal sizing mechanism into action
        if (width > 0) {
            this.messageTextArea.setSize(width, Short.MAX_VALUE);
        }
        return this.mainPanel;
    }
}
