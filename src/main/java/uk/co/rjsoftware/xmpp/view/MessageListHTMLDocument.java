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
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageListHTMLDocument extends HTMLDocument {

    private long messageCount;

    public MessageListHTMLDocument() {
        // TODO: Get emoticons working

        // Setup the default HTML formatting
        final HTMLEditorKit editorKit = new HTMLEditorKit();
        final StyleSheet styles = editorKit.getStyleSheet();
        this.getStyleSheet().addStyleSheet(styles);

        setParser(new ParserDelegator());

        try {
            insertAfterStart(getRootElements()[0],
                    "<html><head></head><body><table id='table' style='width:100%'></table></body></html>");
        } catch (BadLocationException exception) {
            throw new RuntimeException(exception);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        Font font = new JLabel().getFont();
        String bodyRule = "body { font-family: " + font.getFamily() + "; "
                + "font-size: " + font.getSize() + "pt; }";
        getStyleSheet().addRule(bodyRule);
    }

    public void insertMessage(final CustomMessage message) {
        // TODO: Use a thread-safe date formatter, and change it into a class attribute, so it doesn't need creating here
        final DateFormat formatter = new SimpleDateFormat("HH:mm");
        final Date date = new Date(message.getTimestamp());

        final String messageBody = addLinks(message.getBody());
        try {
            if (this.messageCount == 0) {
                insertAfterStart(getElement("table"),
                        "<tr id='" + this.messageCount + "'>"
                                + "<td width='125' align='right' valign='top'>" + message.getSender() + "</td>"
                                + "<td valign='top'>" + messageBody + "</td>"
                                + "<td width='42' valign='top'>" + formatter.format(date) + "</td></tr>");
            }
            else {
                insertAfterEnd(getElement(String.valueOf(this.messageCount - 1)),
                        "<tr id='" + this.messageCount + "'>"
                                + "<td width='125' align='right' valign='top'>" + message.getSender() + "</td>"
                                + "<td valign='top'>" + messageBody + "</td>"
                                + "<td width='42' valign='top'>" + formatter.format(date) + "</td></tr>");
            }
        } catch (BadLocationException exception) {
            throw new RuntimeException(exception);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        this.messageCount++;
    }

    private String addLinks(String messageText) {
        Pattern p = Pattern.compile("<[iI][mM][gG] .+?/>|<[aA] .+?</[aA]>|\\(?https?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]");
        Matcher m = p.matcher(messageText);
        StringBuffer changedMessageText = new StringBuffer();
        while (m.find()) {
            // ingore existing <a> links
            if (m.group().substring(0, 3).toLowerCase(Locale.getDefault()).equals("<a ")) {
                m.appendReplacement(changedMessageText, m.group());
            }
            // ignore <img> tags - they contain href links we don't want to wrap
            else if (m.group().substring(0, 5).toLowerCase(Locale.getDefault()).equals("<img ")) {
                m.appendReplacement(changedMessageText, m.group());
            }
            else {
                m.appendReplacement(changedMessageText, "<a href='" + m.group() + "'s>" + m.group() + "</a>");
            }
        }
        m.appendTail(changedMessageText);

        return changedMessageText.toString();
    }
}
