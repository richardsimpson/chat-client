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

import com.jgoodies.common.collect.ArrayListModel;

import java.util.List;

public class UserListModel extends ArrayListModel<User> {

    private String ownerId;

    public UserListModel() {
        super();
    }

    public UserListModel(final List<User> users) {
        super(users);
    }

    private int indexOf(final String userId) {
        for (int index = 0 ; index < size() ; index++) {
            if (get(index).getUserId().equals(userId)) {
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
        return get(index);
    }

    public void updateUserStatus(final String userId, final String resource, final UserStatus userStatus) {
        final int index = indexOf(userId);

        if (index != -1) {
            get(index).setStatus(resource, userStatus);
            fireContentsChanged(index);
        }
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        final int ownerIndex = indexOf(ownerId);
        if (ownerIndex != -1) {
            fireContentsChanged(ownerIndex);
        }
    }
}
