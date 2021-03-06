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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Emoticon {

    private final String shortcut;
    private final String regexShortcut;
    private final String url;

    public Emoticon(final String shortcut, final String url) {
        this.shortcut = shortcut;
        this.regexShortcut = escapeRegexCharacters(shortcut);
        this.url = url;
    }

    private String escapeRegexCharacters(final String shortcut) {

        // note that we MUST replace the \ character first.  Since the regex escape character is a \, doing
        // other replacements first will add extra \ characters to the string, and we don't want to escape
        // those ones.
        String regexShortcut = shortcut.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("("), Matcher.quoteReplacement("\\("));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote(")"), Matcher.quoteReplacement("\\)"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("."), Matcher.quoteReplacement("\\."));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("+"), Matcher.quoteReplacement("\\+"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("*"), Matcher.quoteReplacement("\\*"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("?"), Matcher.quoteReplacement("\\?"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("["), Matcher.quoteReplacement("\\["));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("]"), Matcher.quoteReplacement("\\]"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("^"), Matcher.quoteReplacement("\\^"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("$"), Matcher.quoteReplacement("\\$"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("{"), Matcher.quoteReplacement("\\{"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("}"), Matcher.quoteReplacement("\\}"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("="), Matcher.quoteReplacement("\\="));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("!"), Matcher.quoteReplacement("\\!"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("<"), Matcher.quoteReplacement("\\<"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote(">"), Matcher.quoteReplacement("\\>"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("|"), Matcher.quoteReplacement("\\|"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote(":"), Matcher.quoteReplacement("\\:"));
        regexShortcut = regexShortcut.replaceAll(Pattern.quote("-"), Matcher.quoteReplacement("\\-"));

        return regexShortcut;
    }

    public String getRegexShortcut() {
        return this.regexShortcut;
    }

    public String getUrl() {
        return this.url;
    }
}
