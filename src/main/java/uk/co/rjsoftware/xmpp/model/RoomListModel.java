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

import org.jivesoftware.smack.util.StringUtils;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RoomListModel extends AbstractListModel<Room> implements PropertyChangeListener, Iterable<Room> {

    private final List<Room> rooms = new ArrayList<Room>();

    public RoomListModel() {
        super();
    }

    public void add(final Room room) {
        room.addPropertyChangeListener(this);
        this.rooms.add(room);
        fireIntervalAdded(this, this.rooms.size()-1, this.rooms.size()-1);
    }

    public void remove(final Room room) {
        final int index = this.rooms.indexOf(room);
        if (index != -1) {
            this.rooms.remove(index);
            room.removePropertyChangeListener(this);
            fireIntervalRemoved(this, index, index);
        }
    }

    @Override
    public int getSize() {
        return this.rooms.size();
    }

    @Override
    public Room getElementAt(int index) {
        return this.rooms.get(index);
    }

    private int indexOf(final String roomId) {
        for (int index = 0 ; index < this.rooms.size() ; index++) {
            if (this.rooms.get(index).getRoomId().equals(roomId)) {
                return index;
            }
        }

        return -1;
    }

    public Room get(final String roomId) {
        final String parsedRoomId = StringUtils.parseBareAddress(roomId);
        final int index = indexOf(parsedRoomId);
        if (index == -1) {
            return null;
        }
        return this.rooms.get(index);
    }

    public void sort() {
        Collections.sort(this.rooms);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final int index = this.rooms.indexOf(event.getSource());
        if (index != -1) {
            fireContentsChanged(this, index, index);
        }
    }

    @Override
    public Iterator<Room> iterator() {
        return this.rooms.iterator();
    }
}
