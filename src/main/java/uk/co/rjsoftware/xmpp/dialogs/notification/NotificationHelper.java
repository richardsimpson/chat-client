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
package uk.co.rjsoftware.xmpp.dialogs.notification;

import uk.co.rjsoftware.xmpp.model.ChatTarget;
import uk.co.rjsoftware.xmpp.model.CustomMessage;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class NotificationHelper {

    private static NotificationForm notificationForm;
    private static int currentMessageCount;
    private static NotificationCloseTimer currentTimer;

    private NotificationHelper() {}

    public static synchronized void addMessage(final ChatTarget chatTarget, final CustomMessage message) {
        // stop the old timer
        if (null != currentTimer) {
            currentTimer.stop();
        }

        // recreate the popup form if it's been destroyed
        if (null == notificationForm) {
            notificationForm = new NotificationForm();
        }

        currentMessageCount++;

        notificationForm.addMessage(chatTarget, message);
        notificationForm.setVisible(true);

        // setup the closing of the notification form in 5 seconds
        final NotificationCloseListener listener = new NotificationCloseListener(currentMessageCount);
        currentTimer = new NotificationCloseTimer(5000, listener);
        currentTimer.setRepeats(false);
        currentTimer.start();
    }

    private static synchronized void removeNotificationForm(final int originalMessageCount) {
        // check that another message has not been received since the timer was started.
        if (currentMessageCount != originalMessageCount) {
            return;
        }

        // no new message, so remove the notification form.
        if (null != notificationForm) {
            notificationForm.dispose();
            notificationForm = null;
        }
    }

    private static class NotificationCloseTimer extends Timer {

        public NotificationCloseTimer(final int delay, final ActionListener listener) {
            super(delay, listener);
        }
    }

    private static final class NotificationCloseListener implements ActionListener {

        private final int currentMessageCount;

        private NotificationCloseListener(final int currentMessageCount) {
            this.currentMessageCount = currentMessageCount;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            removeNotificationForm(this.currentMessageCount);
        }
    }

}
