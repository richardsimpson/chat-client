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
package uk.co.rjsoftware.xmpp.model.hipchat.emoticons;

import uk.co.rjsoftware.xmpp.client.YaccProperties;
import uk.co.rjsoftware.xmpp.model.Emoticon;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

// This class is a nasty hack to avoid passing the list of emoticons around.
// It needs to be constructed before the static method 'getEmoticons' will return
// anything.
// TODO: Replace this will dependency injection!
public class HipChatEmoticons {

    private final YaccProperties yaccProperties;
    private static List<Emoticon> emoticons;

    public HipChatEmoticons(final YaccProperties yaccProperties) {
        this.yaccProperties = yaccProperties;
        emoticons = retrieveEmoticons();
    }

    private List<Emoticon> retrieveEmoticons() {
        final List<Emoticon> result = new ArrayList<Emoticon>();
        Client client = ClientBuilder.newClient();

        final String apiEndpoint = this.yaccProperties.getProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_ENDPOINT);
        final String authToken = this.yaccProperties.getProperty(YaccProperties.PROPERTY_NAME_HIPCHAT_API_AUTH_TOKEN);

        WebTarget emoticonTarget = client.target(apiEndpoint).path("emoticon").queryParam("auth_token", authToken)
                .queryParam("max-results", 0);

        EmoticonListResponse emoticonListResponse = requestEmoticons(emoticonTarget);
        addResponseToList(emoticonListResponse, result);

        while (null != emoticonListResponse.getLinks().getNext()) {
            emoticonTarget = client.target(emoticonListResponse.getLinks().getNext()).queryParam("auth_token", authToken);

            emoticonListResponse = requestEmoticons(emoticonTarget);
            addResponseToList(emoticonListResponse, result);
        }

        // TODO: Add the standard emoticons: :-), etc

        return result;
    }

    private EmoticonListResponse requestEmoticons(WebTarget emoticonTarget) {
        Response response = emoticonTarget.request().get();

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException(response.readEntity(String.class));
        }

        return response.readEntity(EmoticonListResponse.class);

    }

    private void addResponseToList(EmoticonListResponse emoticonListResponse, final List<Emoticon> emoticons) {
        for (EmoticonListResponse.Item item : emoticonListResponse.getItems()) {
            final Emoticon newEmoticon = new Emoticon("(" + item.getShortcut() + ")", item.getUrl());
            emoticons.add(newEmoticon);
        }
    }

    /**
     * Returns a list of all the known emoticons
     */
    public static List<Emoticon> getEmoticons() {
        return emoticons;
    }

}
