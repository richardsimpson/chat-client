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
package uk.co.rjsoftware.xmpp.model.hipchat.room;

import uk.co.rjsoftware.xmpp.client.YaccProperties;
import uk.co.rjsoftware.xmpp.model.Room;
import uk.co.rjsoftware.xmpp.model.RoomPrivacy;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class HipChatRoom {

    private final YaccProperties yaccProperties;

    public HipChatRoom(final YaccProperties yaccProperties) {
        this.yaccProperties = yaccProperties;
    }

    /**
     * Creates a new hipchat room.  This uses the hipchat rest API, since it doesn't seem possible
     * to create rooms via it's XMPP interface.
     *
     * @param roomName
     *     The name of the new room
     * @param privacy
     *     The privacy setting for the new room
     *
     * @return
     *     The new rooms JID
     */
    public Room createRoom(final String roomName, final RoomPrivacy privacy) {
        Client client = ClientBuilder.newClient();

        final String apiEndpoint = this.yaccProperties.getProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_ENDPOINT);
        final String authToken = this.yaccProperties.getProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_AUTH_TOKEN);

        WebTarget createRoomTarget = client.target(apiEndpoint).path("room").queryParam("auth_token", authToken);

        //generate the request to create the room
        final CreateRoomRequest createRoomRequest = new CreateRoomRequest();
        createRoomRequest.setName(roomName);
        createRoomRequest.setPrivacy(privacy.getDescription());

        Response response1 = createRoomTarget.request().post(Entity.entity(createRoomRequest, MediaType.APPLICATION_JSON_TYPE));

        if (response1.getStatus() != Response.Status.CREATED.getStatusCode()) {
            throw new RuntimeException(response1.readEntity(String.class));
        }

        CreateRoomResponse createRoomResponse = response1.readEntity(CreateRoomResponse.class);

        // When room is created successfully, the id returned is NOT the JID, so we need to make a second request to
        // convert the id into a JID.
        WebTarget getRoomTarget = client.target(apiEndpoint).path("room").path(createRoomResponse.getId())
                .queryParam("auth_token", authToken);

        Response response2 = getRoomTarget.request().get();
        final RoomPojo roomPojo = response2.readEntity(RoomPojo.class);

        return new Room(roomPojo.getJid(), roomPojo.getName());
    }
}
