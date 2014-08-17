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

import uk.co.rjsoftware.xmpp.client.CustomConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class RecentChatPersistor {

    private final CustomConnection connection;
    private final UserListModel userListModel;
    private final RoomListModel roomListModel;

    public RecentChatPersistor(final CustomConnection connection) {
        this.connection = connection;
        this.userListModel = connection.getUserListModel();
        this.roomListModel = connection.getRoomListModel();
    }

    private String getChatListFilename(final String currentUserId) {
        return ChatPersistor.CHAT_HISTORY_DIR + File.separator + currentUserId + File.separator + "recentChatList.txt";
    }

    public void saveRecentChatList() {
        final String filename = getChatListFilename(this.connection.getCurrentUser().getUserId());

        final File file = new File(filename);
        file.getParentFile().mkdirs();

        final PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file, false)));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        for (int index = 0 ; index < this.connection.getChatListModel().getSize() ; index++) {
            final ChatTarget chatTarget = this.connection.getChatListModel().getElementAt(index);
            printWriter.println(chatTarget.getClass().getSimpleName() + ":" + chatTarget.getId());
        }

        printWriter.flush();
        printWriter.close();
    }

    public void loadRecentChatList() {
        final String filename = getChatListFilename(this.connection.getCurrentUser().getUserId());

        final File file = new File(filename);
        if (file.exists()) {
            // open the file
            final BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException exception) {
                throw new RuntimeException(exception);
            }

            // read the file and join all the chats
            try {
                String line = reader.readLine();
                while (null != line) {
                    final String[] lineArray = line.split(":");
                    joinChat(lineArray);

                    line = reader.readLine();
                }

            } catch (IOException exception) {
                throw new RuntimeException(exception);
            } finally {
                try {
                    reader.close();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
    }

    private void joinChat(String[] lineArray) {
        // join the specified chat
        switch (lineArray[0]) {

            case "User" : final User user = this.userListModel.get(lineArray[1]);
                          // user may no longer exist - check for this here
                          if (null != user) {
                              user.join(this.connection);
                          }
                          break;

            case "Room" : final Room room = this.roomListModel.get(lineArray[1]);
                          // room may no longer exist - check for this here
                          if (null != room) {
                              room.join(this.connection);
                          }
                          break;

            default: throw new RuntimeException("Unexpected chat type: " + lineArray[0]);
        }
    }
}
