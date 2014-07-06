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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageUtilsTest {

    @Test
    public void testEscapeHtmlWithEmptyString() {
        final String originalString = "";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", originalString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithSingleCharacter() {
        final String originalString = "t";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", originalString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithNoHtmlCharacters() {
        final String originalString = "this is a test";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", originalString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithASingleRogueHtmlElement() {
        final String originalString = "<boom>";
        final String expectedString = "&lt;boom&gt;";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", expectedString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithSeveralTags() {
        final String originalString = "this<boom>is<blaa>a<boom2>decent<boom3>test";
        final String expectedString = "this&lt;boom&gt;is&lt;blaa&gt;a&lt;boom2&gt;decent&lt;boom3&gt;test";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", expectedString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithSeveralConsecutiveTags() {
        final String originalString = "<boom><blaa><boom2><boom3>";
        final String expectedString = "&lt;boom&gt;&lt;blaa&gt;&lt;boom2&gt;&lt;boom3&gt;";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", expectedString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithSeveralConsecutiveValidTags() {
        final String originalString = "<a href='blaa'>link</a>another<boom2>decent<img src='blaa'/>";
        final String expectedString = "<a href='blaa'>link</a>another&lt;boom2&gt;decent<img src='blaa'/>";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", expectedString, escapedString);
    }

    @Test
    public void testEscapeHtmlWithSomeValidTags() {
        final String originalString = "this<boom>is<a href='blaa'>link</a>another<boom2>decent<img src='blaa'/><boom3>test";
        final String expectedString = "this&lt;boom&gt;is<a href='blaa'>link</a>another&lt;boom2&gt;decent<img src='blaa'/>&lt;boom3&gt;test";

        final String escapedString = MessageUtils.escapeHtml(originalString);

        assertEquals("incorrect string returned", expectedString, escapedString);
    }
}
