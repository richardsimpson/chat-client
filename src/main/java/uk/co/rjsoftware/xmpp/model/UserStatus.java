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

import org.jivesoftware.smack.packet.Presence;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public enum UserStatus {
    FREE_TO_CHAT(1, "Free to chat", "user-available.png"),
    AVAILABLE(2, "Available", "user-available.png"),
    AWAY(3, "Away", "user-extended-away.png"),
    EXTENDED_AWAY(4, "Away", "user-extended-away.png"),
    DO_NOT_DISTURB(5, "Do not disturb", "user-busy.png"),
    UNAVAILABLE(6, "Unavailable", "user-offline.png");

    private final int priority;
    private final String description;
    private final ImageIcon imageIcon;

    UserStatus(final int priority, final String description, final String filename) {
        this.priority = priority;
        this.description = description;

        final InputStream inputStream = this.getClass().getResourceAsStream(filename);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[2048];

        try {
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.imageIcon = new ImageIcon(buffer.toByteArray(), this.description);
    }

    public String getDescription() {
        return this.description;
    }

    public int getPriority() {
        return this.priority;
    }

    public ImageIcon getImageIcon() {
        return this.imageIcon;
    }

    public static UserStatus fromPresence(CustomPresence presence) {
        if (presence.getType() == null) {
            return UserStatus.UNAVAILABLE;
        }

        if (presence.getType().equals(Presence.Type.unavailable)) {
            return UserStatus.UNAVAILABLE;
        }

        if (presence.getMode() == null) {
            return UserStatus.AVAILABLE;
        }

        switch (presence.getMode()) {
            case available: return UserStatus.AVAILABLE;
            case away:      return UserStatus.AWAY;
            case chat:      return UserStatus.FREE_TO_CHAT;
            case dnd:       return UserStatus.DO_NOT_DISTURB;
            case xa:        return UserStatus.EXTENDED_AWAY;
            default:        return UserStatus.AVAILABLE;
        }
    }
}
