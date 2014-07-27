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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomMessage {

    private final long timestamp;
    private final String sender;
    private final String body;
    private boolean read;

    /**
     *
     * @param timestamp
     * @param sender The sender in the format {roomname}@conf.hipchat.com/{username}
     * @param body
     */
    public CustomMessage(final long timestamp, final String sender, final String body) {
        this(timestamp, sender, body, false);
    }

    /**
     *
     * @param timestamp
     * @param sender The sender in the format {roomname}@conf.hipchat.com/{username}
     * @param body
     * @param read Indicates whether or not the message has already been read or not.
     */
    public CustomMessage(final long timestamp, final String sender, final String body, final boolean read) {
        this.timestamp = timestamp;
        final int index = sender.indexOf("/");
        if (index == -1) {
            this.sender = sender;
        }
        else {
            this.sender = sender.substring(index+1);
        }
        this.body = body;
        this.read = read;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getSender() {
        return this.sender;
    }

    public String getBody() {
        return this.body;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(final boolean read) {
        this.read = read;
    }

    @Override
    public String toString() {
        final DateFormat formatter = new SimpleDateFormat("HH:mm");
        final Date date = new Date(this.timestamp);

        return this.sender + " (" + formatter.format(date) + "): " + this.body;
    }

}
