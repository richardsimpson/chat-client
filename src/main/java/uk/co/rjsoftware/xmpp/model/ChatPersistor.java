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

import com.google.gson.Gson;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ChatPersistor {

    public static final String CHAT_HISTORY_DIR = "chathistory";

    private final String currentUserId;
    private final String chatJid;
    private final CustomMessageListModel customMessageListModel;
    private final ChatListDataListener listener;
    private final String filename;
    private final Gson gson = new Gson();

    public ChatPersistor(final String currentUserId, final String chatJid, final CustomMessageListModel customMessageListModel) {
        this.currentUserId = currentUserId;
        this.chatJid = chatJid;
        this.customMessageListModel = customMessageListModel;

        this.filename = CHAT_HISTORY_DIR + File.separator + this.currentUserId + File.separator + this.chatJid + ".txt";
        this.listener = new ChatListDataListener(this.customMessageListModel, this.filename);
        this.customMessageListModel.addListDataListener(this.listener);
    }

    private CustomMessage stringToMessage(final String messageString) {
        CustomMessage message = this.gson.fromJson(messageString, CustomMessage.class);
        return message;
    }

    public void readChatHistory() {
        final File historyFile = new File(this.filename);
        if (historyFile.exists()) {
            // read the history from the file system, and update the message list model
            this.customMessageListModel.removeListDataListener(this.listener);

            try {
                final BufferedReader reader = new BufferedReader(new FileReader(historyFile));

                String line = reader.readLine();
                while (null != line) {
                    final CustomMessage message = stringToMessage(line);
                    this.customMessageListModel.add(message);

                    line = reader.readLine();
                }
            } catch (FileNotFoundException exception) {
                throw new RuntimeException(exception);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            } finally {
                this.customMessageListModel.addListDataListener(this.listener);
            }
        }
    }

    private static final class ChatListDataListener implements ListDataListener {

        private final CustomMessageListModel customMessageListModel;
        private final String filename;
        private final Gson gson = new Gson();
        private PrintWriter printWriter;


        private ChatListDataListener(final CustomMessageListModel customMessageListModel, final String filename) {
            this.customMessageListModel = customMessageListModel;
            this.filename = filename;
        }

        private String messageToString(final CustomMessage message) {
            final String messageString = this.gson.toJson(message);
            return messageString;
        }

        private void ensureWriterCreated() {
            if (null != this.printWriter) {
                return;
            }

            final File file = new File(this.filename);
            file.getParentFile().mkdirs();
            try {
                this.printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

        }

        @Override
        public void intervalAdded(ListDataEvent event) {
            ensureWriterCreated();
            final CustomMessage message = this.customMessageListModel.get(event.getIndex0());
            this.printWriter.println(messageToString(message));
            this.printWriter.flush();
        }

        @Override
        public void intervalRemoved(ListDataEvent event) {
            // should never happen
        }

        @Override
        public void contentsChanged(ListDataEvent event) {
            // should never happen
        }

        @Override
        protected void finalize() throws Throwable {
            if (null != this.printWriter) {
                this.printWriter.close();
            }
        }

    }
}
