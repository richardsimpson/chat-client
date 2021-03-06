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
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageListHTMLDocument extends HTMLDocument {

    private static final char[] NEWLINE;
    private static final String DODGER_BLUE = "#1E90FF";

    private int currentTableId;
    private Calendar lastMessageDate;
    private final DateFormat dateFormatter = new SimpleDateFormat("EEEE, d MMMM, yyyy");

    static {
        NEWLINE = new char[1];
        NEWLINE[0] = '\n';
    }

    private boolean firstMessage = true;

    public MessageListHTMLDocument() {
        // Setup the default HTML formatting
        final HTMLEditorKit editorKit = new HTMLEditorKit();
        final StyleSheet styles = editorKit.getStyleSheet();
        this.getStyleSheet().addStyleSheet(styles);

        setParser(new ParserDelegator());

        this.currentTableId = 1;
        clear();

        Font font = new JLabel().getFont();
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        String senderRule = ".sender {white-space: nowrap; color: " + DODGER_BLUE + ";}";
        String unreadSenderRule = ".unreadSender {white-space: nowrap; color: orange;}";
        String dateHeaderRule = ".dateHeader {font-weight:bold; padding-top: 4px; padding-bottom: 4px; margin-top: 5px; margin-left: 100px; border-style:solid; " +
                "border-width:0px; border-bottom-width:1px; border-top-width:1px; border-color:" + DODGER_BLUE + "; }";

        getStyleSheet().addRule(bodyRule);
        getStyleSheet().addRule(senderRule);
        getStyleSheet().addRule(unreadSenderRule);
        getStyleSheet().addRule(dateHeaderRule);

        this.lastMessageDate = Calendar.getInstance(Locale.getDefault());
        this.lastMessageDate.setTime(new Date(0));
    }

    public void clear() {
        try {
            remove(0, getLength());
            insertAfterStart(getRootElements()[0],
                    "<html><head></head><body><table id='t" + this.currentTableId + "' style='width:100%'></table></body></html>");
        } catch (BadLocationException exception) {
            throw new RuntimeException(exception);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void insertMessage(final CustomMessage message, final long messageNumber) {
        // TODO: Use a thread-safe date formatter, and change it into a class attribute, so it doesn't need creating here
        final DateFormat formatter = new SimpleDateFormat("HH:mm");
        final Date date = new Date(message.getTimestamp());

        final Calendar messageDate = Calendar.getInstance(Locale.getDefault());
        messageDate.setTime(date);
        messageDate.set(Calendar.HOUR_OF_DAY, 0);
        messageDate.set(Calendar.MINUTE, 0);
        messageDate.set(Calendar.SECOND, 0);
        messageDate.set(Calendar.MILLISECOND, 0);
        if (messageDate.after(this.lastMessageDate)) {
            outputDateHeader(messageDate);
            this.lastMessageDate = messageDate;
        }

        // escape all HTML, except for <img> and <a> tags
        String messageBody = MessageUtils.escapeHtml(message.getBody());
        // add HTML <a> tags around text that looks like web references (http(s)...)
        messageBody = MessageUtils.addLinks(messageBody);
        // convert emoticons into Image tags
        messageBody = MessageUtils.addEmoticons(messageBody);
        // convert carriage returns into <br> tags
        messageBody = MessageUtils.convertCarriageReturns(messageBody);
        // convert leading spaces into non-breaking spaces (&nbsp;)
        messageBody = MessageUtils.convertLeadingSpacesAndTabs(messageBody);

        try {
            final String senderClassName;
            if (message.isRead()) {
                senderClassName = "sender";
            }
            else {
                senderClassName = "unreadSender";
            }

            insertBeforeEnd(getElement("t" + this.currentTableId),
                    "<tr>" +
                            "<td id='" + messageNumber + "' width='125' class='" + senderClassName + "' align='right' valign='top'>" + message.getSender() + "</td>" +
                            "<td valign='top'>" + messageBody + "</td>" +
                            "<td width='42' valign='top'>" + formatter.format(date) + "</td></tr>");
        } catch (BadLocationException exception) {
            throw new RuntimeException(exception);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        this.firstMessage = false;
    }

    private void outputDateHeader(final Calendar messageDate) {
        try {
            final Element element = getElement("t" + this.currentTableId);

            if (this.firstMessage) {
                insertBeforeStart(element,
                        "<div class='dateHeader'>" + this.dateFormatter.format(messageDate.getTime())  + "</div>");
            }
            else {
                this.currentTableId++;
                insertAfterEnd(element,
                        "<div class='dateHeader'>" + this.dateFormatter.format(messageDate.getTime())  + "</div>" +
                                "<table id='t" + this.currentTableId + "' style='width:100%'></table>");
            }
        } catch (BadLocationException exception) {
            throw new RuntimeException(exception);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void insertBeforeEnd(Element elem, String htmlText) throws BadLocationException, IOException {
        if (elem != null && elem.isLeaf()) {
            throw new IllegalArgumentException("Can not set inner HTML before end of leaf");
        }
        if (elem != null) {
            int offset = elem.getEndOffset();
            if (elem.getElement(elem.getElementIndex(offset - 1)).isLeaf()
                    && getText(offset - 1, 1).charAt(0) == NEWLINE[0]) {
                offset--;
            }
            insertHTMLWithCustomHTMLReader(elem, offset, htmlText);
        }
    }

    @Override
    public void insertAfterStart(Element elem, String htmlText) throws BadLocationException, IOException {
        if (elem != null && elem.isLeaf()) {
            throw new IllegalArgumentException("Can not insert HTML after start of a leaf");
        }
        insertHTMLWithCustomHTMLReader(elem, elem.getStartOffset(), htmlText);
    }

    @Override
    public void insertAfterEnd(Element elem, String htmlText) throws BadLocationException, IOException {
        if (elem != null) {
            Element parent = elem.getParentElement();

            if (parent != null) {
                int offset = elem.getEndOffset();
                if (offset > getLength()) {
                    offset--;
                }
                else if (elem.isLeaf() && getText(offset - 1, 1).
                        charAt(0) == NEWLINE[0]) {
                    offset--;
                }
                insertHTMLWithCustomHTMLReader(parent, offset, htmlText);
            }
        }
    }

    /**
     * Inserts a string of HTML into the document at the given position.
     * <code>parent</code> is used to identify the location to insert the
     * <code>html</code>. If <code>parent</code> is a leaf this can have
     * unexpected results.
     */
    private void insertHTMLWithCustomHTMLReader(Element parent, int offset, String html)
            throws BadLocationException, IOException {
        if (parent != null && html != null) {
            HTMLEditorKit.Parser parser = getParser();
            if (parser != null) {
                int lastOffset = Math.max(0, offset - 1);
                Element charElement = getCharacterElement(lastOffset);
                Element commonParent = parent;
                int pop = 0;
                int push = 0;

                if (parent.getStartOffset() > lastOffset) {
                    while (commonParent != null && commonParent.getStartOffset() > lastOffset) {
                        commonParent = commonParent.getParentElement();
                        push++;
                    }
                    if (commonParent == null) {
                        throw new BadLocationException("No common parent",
                                offset);
                    }
                }
                while (charElement != null && charElement != commonParent) {
                    pop++;
                    charElement = charElement.getParentElement();
                }
                if (charElement != null) {
                    // Found it, do the insert.
                    HTMLReader reader = new YaccHTMLReader(offset, pop - 1, push);

                    parser.parse(new StringReader(html), reader, true);
                    reader.flush();
                }
            }
        }
    }

    public class YaccHTMLReader extends HTMLDocument.HTMLReader {

        YaccHTMLReader(final int offset, final int popDepth, final int pushDepth) {

            super(offset, popDepth, pushDepth, null);

            TagAction sa = new YaccSpecialAction();
            registerTag(HTML.Tag.IMG, sa);
            registerTag(HTML.Tag.BR, sa);

            // clear out the parseBuffer, which will have been added to by the super constructor.
            // we  don't want any of the end tags that it will have added.
            parseBuffer.clear();

            setPopDepth(popDepth);
            setPushDepth(pushDepth);
            setInsertAfterImplied(true);
            setFoundInsertTag(false);
            setMidInsert(false);
            setInsertInsertTag(true);
            setWantsTrailingNewline(false);

            // the next block from the superclass WAS NOT already executed - but we need it to be
            /**
             * This block initializes the <code>inParagraph</code> flag.
             * It is left in <code>false</code> value automatically
             * if the target document is empty or future inserts
             * were positioned into the 'body' tag.
             */
            int targetOffset = Math.max(getOffset() - 1, 0);
            Element elem =
                    MessageListHTMLDocument.this.getCharacterElement(targetOffset);
            /* Going up by the left document structure path */
            for (int i = 0; i <= getPopDepth(); i++) {
                elem = elem.getParentElement();
            }
            /* Going down by the right document structure path */
            for (int i = 0; i < getPushDepth(); i++) {
                int index = elem.getElementIndex(getOffset());
                elem = elem.getElement(index);
            }
            AttributeSet attrs = elem.getAttributes();
            if (attrs != null) {
                HTML.Tag tagToInsertInto =
                        (HTML.Tag) attrs.getAttribute(StyleConstants.NameAttribute);
                if (tagToInsertInto != null) {
                    setInParagraph(isParagraph(tagToInsertInto));
                }
            }
        }

        private void setPopDepth(final int popDepth) {
            setPackageProtectedField("popDepth", popDepth);
        }

        private void setPushDepth(final int pushDepth) {
            setPackageProtectedField("pushDepth", pushDepth);
        }

        private void setInsertAfterImplied(final boolean insertAfterImplied) {
            setPackageProtectedField("insertAfterImplied", insertAfterImplied);
        }

        private void setFoundInsertTag(final boolean foundInsertTag) {
            setPackageProtectedField("foundInsertTag", foundInsertTag);
        }

        private void setMidInsert(final boolean midInsert) {
            setPackageProtectedField("midInsert", midInsert);
        }

        private void setInsertInsertTag(final boolean insertInsertTag) {
            setPackageProtectedField("insertInsertTag", insertInsertTag);
        }

        private void setWantsTrailingNewline(final boolean wantsTrailingNewline) {
            setPackageProtectedField("wantsTrailingNewline", wantsTrailingNewline);
        }

        private void setInParagraph(final boolean inParagraph) {
            setPackageProtectedField("inParagraph", inParagraph);
        }

        private boolean getEmptyDocument() {
            return (Boolean)getPackageProtectedField("emptyDocument");
        }

        private boolean getMidInsert() {
            return (Boolean)getPackageProtectedField("midInsert");
        }

        private int getOffset() {
            return (Integer)getPackageProtectedField("offset");
        }

        private int getPopDepth() {
            return (Integer)getPackageProtectedField("popDepth");
        }

        private int getPushDepth() {
            return (Integer)getPackageProtectedField("pushDepth");
        }

        private void setPackageProtectedField(final String fieldname, final Object newValue) {
            try {
                final Field field = this.getClass().getSuperclass().getDeclaredField(fieldname);
                field.setAccessible(true);
                field.set(this, newValue);
            } catch (NoSuchFieldException exception) {
                throw new RuntimeException(exception);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }

        private Object getPackageProtectedField(final String fieldname) {
            try {
                final Field field = this.getClass().getSuperclass().getDeclaredField(fieldname);
                field.setAccessible(true);
                return field.get(this);
            } catch (NoSuchFieldException exception) {
                throw new RuntimeException(exception);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }

        private boolean isParagraph(final HTML.Tag tag) {
            return (
                    tag == HTML.Tag.P
                            || tag == HTML.Tag.IMPLIED
                            || tag == HTML.Tag.DT
                            || tag == HTML.Tag.H1
                            || tag == HTML.Tag.H2
                            || tag == HTML.Tag.H3
                            || tag == HTML.Tag.H4
                            || tag == HTML.Tag.H5
                            || tag == HTML.Tag.H6
            );
        }

        private class YaccSpecialAction extends TagAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                addSpecialElementForImg(t, a);
            }

        }

        /**
         * Modified version of HTMLReader.addSpecialElement that sets the
         * text of an IMG to be the same as the 'alt' text.
         */
        protected void addSpecialElementForImg(HTML.Tag t, MutableAttributeSet a) {
            final int parseBufferOriginalSize = parseBuffer.size();

            super.addSpecialElement(t, a);

            // check if any elements were actually added
            if (parseBufferOriginalSize < parseBuffer.size()) {
                // check if this is a tag that requires further processing
                final String tagName = t.toString().toUpperCase(Locale.getDefault());
                if ((tagName.equals("IMG")) || (tagName.equals("BR"))){

                    // if the tag was added, then there will either be one or two new elements.
                    // There is an optional end tag, and then there _may_ be an image tag,
                    // if the image could be added by the superclass.  So, we should check
                    // if the last tag added was the img tag, and if so, replace it.

                    final ElementSpec elementSpec = parseBuffer.get(parseBuffer.size()-1);

                    final HTML.Tag tag = (HTML.Tag)elementSpec.getAttributes().getAttribute(StyleConstants.NameAttribute);

                    if ((tag != null) && (HTML.Tag.IMG.equals(tag))) {
                        final String altText = (String)a.getAttribute(HTML.Attribute.ALT);

                        if ((altText != null) && (!altText.equals(""))) {
                            // remove the last element
                            parseBuffer.remove(parseBuffer.size()-1);

                            // then add a replacement tag (containing the images alt text in the plain text)
                            ElementSpec es = new ElementSpec(
                                    a.copyAttributes(), ElementSpec.ContentType, altText.toCharArray(), 0, altText.length());
                            parseBuffer.addElement(es);
                        }
                    }
                    else if ((tag != null) && (HTML.Tag.BR.equals(tag))) {
                        // remove the last element
                        parseBuffer.remove(parseBuffer.size()-1);

                        // then add a replacement tag (containing a carriage return in the plain text)
                        ElementSpec es = new ElementSpec(
                                a.copyAttributes(), ElementSpec.ContentType, System.lineSeparator().toCharArray(), 0, System.lineSeparator().length());
                        parseBuffer.addElement(es);
                    }
                }
            }
        }

    }
}
