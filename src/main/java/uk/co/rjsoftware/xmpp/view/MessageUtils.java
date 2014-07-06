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
package uk.co.rjsoftware.xmpp.view;

import org.apache.commons.lang3.StringEscapeUtils;
import uk.co.rjsoftware.xmpp.model.Emoticon;
import uk.co.rjsoftware.xmpp.model.hipchat.emoticons.HipChatEmoticons;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtils {

    private static final int TAB_SIZE = 4;

    private static final String IMAGE_AND_LINK_PATTERN_STRING = "<[iI][mM][gG] .+?/>|<[aA] .+?</[aA]>";
    // this regex will match all <img> tags, all <a> (link) tags, and any standalone http links
    private static final Pattern LINK_PATTERN = Pattern.compile(IMAGE_AND_LINK_PATTERN_STRING + "|https?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]");
    private static final Pattern ACCEPTABLE_HTML_PATTERN = Pattern.compile(IMAGE_AND_LINK_PATTERN_STRING);

    private MessageUtils() {
        // empty private constructor to prevent instantiation
    }

    public static String escapeHtml(String messageText) {
        final Matcher m = ACCEPTABLE_HTML_PATTERN.matcher(messageText);
        final StringBuffer changedMessageText = new StringBuffer();
        int lastEndPos = 0;

        while (m.find()) {
            // escape the text between the last match and this one, then append it to the result.
            changedMessageText.append(StringEscapeUtils.escapeHtml4(messageText.substring(lastEndPos, m.start())));

            // append the match
            changedMessageText.append(m.group());

            lastEndPos = m.end();
        }

        // append the remaining text
        changedMessageText.append(StringEscapeUtils.escapeHtml4(messageText.substring(lastEndPos)));

        return changedMessageText.toString();
    }

    public static String addLinks(String messageText) {
        final Matcher m = LINK_PATTERN.matcher(messageText);
        final StringBuffer changedMessageText = new StringBuffer();

        while (m.find()) {
            // ignore existing <a> links
            if (m.group().substring(0, 3).toLowerCase(Locale.getDefault()).equals("<a ")) {
                m.appendReplacement(changedMessageText, m.group());
            }
            // ignore <img> tags - they contain href links we don't want to wrap
            else if (m.group().substring(0, 5).toLowerCase(Locale.getDefault()).equals("<img ")) {
                m.appendReplacement(changedMessageText, m.group());
            }
            else {
                m.appendReplacement(changedMessageText, "<a href='" + m.group() + "'>" + m.group() + "</a>");
            }
        }
        m.appendTail(changedMessageText);

        return changedMessageText.toString();
    }

    public static String addEmoticons(final String messageText) {
        final java.util.List<Emoticon> emoticons = HipChatEmoticons.getEmoticons();

        String changedMessageText = messageText;

        for (Emoticon emoticon : emoticons) {
            changedMessageText = addEmoticon(emoticon, changedMessageText);
        }

        return changedMessageText;
    }

    private static String addEmoticon(final Emoticon emoticon, final String messageText) {
        final Pattern p = Pattern.compile("<[iI][mM][gG] .+?/>|<[aA] .+?</[aA]>|" + emoticon.getRegexShortcut());
        final Matcher m = p.matcher(messageText);
        final StringBuffer changedMessageText = new StringBuffer();

        while (m.find()) {
            // ignore <a> links
            if ((m.group().length() >= 3) && ((m.group().substring(0, 3).toLowerCase(Locale.getDefault()).equals("<a ")))) {
                m.appendReplacement(changedMessageText, Matcher.quoteReplacement(m.group()));
            }
            // ignore <img> tags
            else if ((m.group().length() >= 5) && (m.group().substring(0, 5).toLowerCase(Locale.getDefault()).equals("<img "))) {
                m.appendReplacement(changedMessageText, Matcher.quoteReplacement(m.group()));
            }
            else {
                // TODO: When copy an image from the message window, copy the 'alt' text
                // TODO: Align the sender name with the first line of the message text.
                m.appendReplacement(changedMessageText,
                        Matcher.quoteReplacement("<img align='bottom' alt='" + m.group() + "' src='" + emoticon.getUrl() + "'/>"));
            }
        }
        m.appendTail(changedMessageText);

        return changedMessageText.toString();
    }

    public static String convertCarriageReturns(final String messageText) {
        String result = messageText.replaceAll("\\r\\n", "<br>");
        result = result.replaceAll("\\r", "<br>");
        result = result.replaceAll("\\n", "<br>");
        return result;
    }

    public static String convertLeadingSpacesAndTabs(final String messageText) {
        final StringBuffer changedMessageText = new StringBuffer();

        int index = 0;
        boolean inWhitespace = true;

        while (inWhitespace && index < messageText.length()) {
            if (messageText.charAt(index) == ' ') {
                changedMessageText.append("&nbsp;");
            }
            else if (messageText.charAt(index) == '\t') {
                changedMessageText.append(generateNonBreakingSpaces(TAB_SIZE));
            }
            else {
                break;
            }

            index++;
        }

        changedMessageText.append(messageText.substring(index));
        return changedMessageText.toString();
    }

    private static String generateNonBreakingSpaces(final int numberOfSpaces) {
        final StringBuffer spaces = new StringBuffer();
        for (int index = 0 ; index < numberOfSpaces ; index++) {
            spaces.append("&nbsp;");
        }
        return spaces.toString();
    }

}
