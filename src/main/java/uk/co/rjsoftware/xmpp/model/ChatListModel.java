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

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChatListModel extends AbstractListModel<ChatTarget> implements PropertyChangeListener, Iterable<ChatTarget> {

    private final List<ChatTarget> chats = new ArrayList<ChatTarget>();

    public ChatListModel() {
        super();
    }

    public void add(final ChatTarget chat) {
        chat.addPropertyChangeListener(ChatTarget.LATEST_MESSAGE_TIMESTAMP_PROPERTY_NAME, this);
        chat.addPropertyChangeListener(ChatTarget.UNREAD_MESSAGE_COUNT_PROPERTY_NAME, this);
        this.chats.add(chat);
        fireIntervalAdded(this, this.chats.size() - 1, this.chats.size() - 1);
    }

    public void remove(final ChatTarget chat) {
        final int index = this.chats.indexOf(chat);
        if (index != -1) {
            this.chats.remove(index);
            chat.removePropertyChangeListener(ChatTarget.LATEST_MESSAGE_TIMESTAMP_PROPERTY_NAME, this);
            chat.removePropertyChangeListener(ChatTarget.UNREAD_MESSAGE_COUNT_PROPERTY_NAME, this);
            fireIntervalRemoved(this, index, index);
        }
    }

    public boolean contains(final ChatTarget chat) {
        return this.chats.contains(chat);
    }

    @Override
    public int getSize() {
        return this.chats.size();
    }

    @Override
    public ChatTarget getElementAt(int index) {
        return this.chats.get(index);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final int index = this.chats.indexOf(event.getSource());
        if (index != -1) {
            fireContentsChanged(this, index, index);
        }
    }

    @Override
    public Iterator<ChatTarget> iterator() {
        return this.chats.iterator();
    }
}
