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
import java.util.List;

public class UserListModel extends AbstractListModel<User> implements PropertyChangeListener {

    private final List<User> users = new ArrayList<User>();

    // the userId of the room owner - only used when this UserListModel represents the occupants of a room.
    private String ownerId;

    public UserListModel() {
        super();
    }

    public UserListModel(final List<User> users) {
        for (User user : users) {
            user.addPropertyChangeListener(this);
        }

        this.users.addAll(users);
    }

    public void add(final User user) {
        user.addPropertyChangeListener(this);
        this.users.add(user);
        fireIntervalAdded(this, this.users.size() - 1, this.users.size() - 1);
    }

    public void remove(final User user) {
        final int index = this.users.indexOf(user);
        if (index != -1) {
            this.users.remove(index);
            user.removePropertyChangeListener(this);
            fireIntervalRemoved(this, index, index);
        }
    }

    private int indexOf(final String userId) {
        for (int index = 0 ; index < this.users.size() ; index++) {
            if (this.users.get(index).getUserId().equals(userId)) {
                return index;
            }
        }

        return -1;
    }

    public User get(final String userId) {
        final int index = indexOf(userId);
        if (index == -1) {
            return null;
        }
        return this.users.get(index);
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        final int ownerIndex = indexOf(ownerId);
        if (ownerIndex != -1) {
            fireContentsChanged(this, ownerIndex, ownerIndex);
        }
    }

    @Override
    public int getSize() {
        return this.users.size();
    }

    @Override
    public User getElementAt(int index) {
        return this.users.get(index);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        final int index = this.users.indexOf(event.getSource());
        if (index != -1) {
            fireContentsChanged(this, index, index);
        }
    }
}
