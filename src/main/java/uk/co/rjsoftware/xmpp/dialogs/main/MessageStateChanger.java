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

import uk.co.rjsoftware.xmpp.model.ChatTarget;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageStateChanger {

    public void addChangeListener(final JScrollPane scrollPane, final MainForm mainForm) {
        scrollPane.getVerticalScrollBar().getModel().addChangeListener(new MessageChangeListener(scrollPane, mainForm));
    }

    // TODO: Messages are not marked as read unless the window resizes or the scroll bar value changes.  So new chats don't get marked as unread.

    private static final class MessageChangeListener implements ChangeListener {

        // is pendingElementsToChange really required?
        private final Map<ChatTarget, Set<Element>> pendingElementsToChange = new HashMap<>();
        private final JTextComponent scrollableComponent;
        private final MainForm mainForm;

        private MessageChangeListener(final JScrollPane scrollPane, final MainForm mainForm) {
            final Component tempComponent = scrollPane.getViewport().getView();
            if (!(tempComponent instanceof JTextComponent)) {
                throw new RuntimeException("Cannot add listener to the scrollpane, as it does not contain a JTextComponent.");
            }

            this.scrollableComponent = (JTextComponent)tempComponent;
            this.mainForm = mainForm;
        }

        @Override
        public void stateChanged(final ChangeEvent event) {
            final StateChanger stateChanger = new StateChanger(this.pendingElementsToChange, this.scrollableComponent, this.mainForm, event);
            SwingUtilities.invokeLater(stateChanger);
        }

        private static final class StateChanger implements Runnable {

            // is pendingElementsToChange really required?
            private final Map<ChatTarget, Set<Element>> pendingElementsToChange;
            private final JTextComponent scrollableComponent;
            private final MainForm mainForm;
            private final ChangeEvent event;

            private StateChanger(final Map<ChatTarget, Set<Element>> pendingElementsToChange, final JTextComponent scrollableComponent,
                                 final MainForm mainForm, final ChangeEvent event) {

                this.pendingElementsToChange = pendingElementsToChange;
                this.scrollableComponent = scrollableComponent;
                this.mainForm = mainForm;
                this.event = event;
            }

            @Override
            public void run() {
                final ChatTarget currentChatTarget = this.mainForm.getCurrentChatTarget();

                if (!((BoundedRangeModel)event.getSource()).getValueIsAdjusting()) {
                    Rectangle visibleRect = this.scrollableComponent.getVisibleRect();

                    final java.util.List<Element> elementsToChange = new ArrayList<Element>();
                    final java.util.List<Integer> elementIdsToChange = new ArrayList<Integer>();

                    // get the first visible sender tag
                    Element element = getFirstVisibleSenderTableCell();
                    if (null == element) {
                        return;
                    }

                    int i = getIdOfElement(element);

                    while (null != element) {

                        final Element paragraphElement = ((HTMLDocument)this.scrollableComponent.getDocument()).getParagraphElement(element.getStartOffset());
                        final String classname = (String)paragraphElement.getAttributes().getAttribute(HTML.Attribute.CLASS);

                        // check if element is already 'read'
                        if ("unreadSender".equals(classname)) {
                            // check if visible
                            int pos = element.getStartOffset();
                            if (!StateChangerUtils.isCompletelyVisible(this.scrollableComponent, pos, visibleRect)) {
                                // not visible, so must have exhausted the visible items
                                break;
                            }
                            elementsToChange.add(element);
                            elementIdsToChange.add(i);
                        }

                        i++;
                        element = ((HTMLDocument)this.scrollableComponent.getDocument()).getElement("" + i);
                    }

                    if (!elementsToChange.isEmpty()) {
                        synchronized (this.pendingElementsToChange) {
                            if (!this.pendingElementsToChange.containsKey(currentChatTarget)) {
                                this.pendingElementsToChange.put(currentChatTarget, new HashSet<Element>());
                            }

                            this.pendingElementsToChange.get(currentChatTarget).addAll(elementsToChange);
                        }

                        final ReadActionListener listener = new ReadActionListener(this.mainForm, currentChatTarget, elementsToChange, elementIdsToChange,
                                this.scrollableComponent, this.pendingElementsToChange);
                        final ReadTimer timer = new ReadTimer(5000, listener);
                        timer.setRepeats(false);
                        timer.start();
                    }
                }
            }

            private Element getFirstVisibleSenderTableCell() {
                Rectangle visibleRect = this.scrollableComponent.getVisibleRect();

                int pos = this.scrollableComponent.viewToModel(visibleRect.getLocation());
                if (-1 == pos) {
                    return null;
                }

                while (pos < this.scrollableComponent.getDocument().getLength()) {
                    //attempt to get first table cell
                    Element element = ((HTMLDocument)this.scrollableComponent.getDocument()).getParagraphElement(pos);
                    if (null == element) {
                        return null;
                    }
                    element = element.getParentElement();
                    if (null == element) {
                        return null;
                    }

                    final HTML.Tag tag = getTag(element);
                    if (HTML.Tag.BODY == tag) {
                        return null;

                    }
                    if (HTML.Tag.TABLE == tag) {
                        element = element.getElement(0);
                    }

                    if (isElementASenderTableCell(element)) {
                        if (StateChangerUtils.isCompletelyVisible(this.scrollableComponent, pos, visibleRect)) {
                            return element;
                        }
                    }

                    pos = element.getEndOffset();
                }

                return null;
            }

            private HTML.Tag getTag(final Element element) {
                return (HTML.Tag)element.getAttributes().getAttribute(StyleConstants.NameAttribute);
            }

            private boolean isElementASenderTableCell(final Element element) {
                final HTML.Tag tag = (HTML.Tag)element.getAttributes().getAttribute(StyleConstants.NameAttribute);
                if ((tag != null) && (HTML.Tag.TD.equals(tag))) {
                    final String id = (String)element.getAttributes().getAttribute(HTML.Attribute.ID);
                    return null != id;
                }

                return false;
            }

            private int getIdOfElement(final Element element) {
                final String id = (String)element.getAttributes().getAttribute(HTML.Attribute.ID);

                if (null == id) {
                    return 0;
                }
                else {
                    return Integer.parseInt(id);
                }
            }

        }

        private static class ReadTimer extends Timer {

            public ReadTimer(final int delay, final ActionListener listener) {
                super(delay, listener);
            }
        }

        private static class ReadActionListener implements ActionListener {

            private final MainForm mainForm;
            private final ChatTarget currentChatTarget;
            private final List<Element> elements;
            private final List<Integer> elementIdsToChange;
            private final JTextComponent scrollableComponent;
            private final Map<ChatTarget, Set<Element>> pendingElementsToChange;

            public ReadActionListener(final MainForm mainForm, final ChatTarget currentChatTarget, final List<Element> elements, final List<Integer> elementIdsToChange,
                                      final JTextComponent scrollableComponent, final Map<ChatTarget, Set<Element>> pendingElementsToChange) {
                this.mainForm = mainForm;
                this.currentChatTarget = currentChatTarget;
                this.elements = elements;
                this.elementIdsToChange = elementIdsToChange;
                this.scrollableComponent = scrollableComponent;
                this.pendingElementsToChange = pendingElementsToChange;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final ChatTarget newCurrentChatTarget = this.mainForm.getCurrentChatTarget();
                Rectangle newVisibleRect = this.scrollableComponent.getVisibleRect();

                synchronized (this.pendingElementsToChange) {
                    for (int index = 0 ; index < this.elements.size() ; index++) {
                        final Element element = this.elements.get(index);

                        // only update the 'read / unread' status of the message if the current chat target has not changed
                        if (newCurrentChatTarget == this.currentChatTarget) {

                            // only update the 'read / unread status of the message if it is still completely visible
                            int pos = element.getStartOffset();
                            if (StateChangerUtils.isCompletelyVisible(this.scrollableComponent, pos, newVisibleRect)) {

                                // set element to be 'read' in the view
                                final MutableAttributeSet divAttributes = new SimpleAttributeSet();

                                divAttributes.addAttribute(HTML.Attribute.CLASS, "sender");

                                ((HTMLDocument)this.scrollableComponent.getDocument()).setParagraphAttributes(element.getStartOffset(),
                                        1, divAttributes, false);

                                // set the element to be 'read' in the model
                                this.currentChatTarget.getCustomMessageListModel().get(elementIdsToChange.get(index)).setRead(true);
                            }
                        }

                        this.pendingElementsToChange.remove(element);
                    }
                }

            }
        }

    }

}
