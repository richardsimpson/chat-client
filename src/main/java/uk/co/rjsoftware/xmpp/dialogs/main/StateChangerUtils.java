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
package uk.co.rjsoftware.xmpp.dialogs.main;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;

public final class StateChangerUtils {

    private StateChangerUtils() {}

    private static Rectangle modelToView(final JTextComponent scrollableComponent, int pos) {
        try {
            return scrollableComponent.modelToView(pos);
        } catch (BadLocationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static boolean intersects(final Rectangle elementRect, final Rectangle viewportRect) {
        // check the location of the top of the element
        if ((elementRect.y < viewportRect.y) || (elementRect.y > viewportRect.y + viewportRect.height)) {
            return false;
        }

        // check the location of the bottom of the element
        if ((elementRect.y + elementRect.height < viewportRect.y) || (elementRect.y + elementRect.height > viewportRect.y + viewportRect.height)) {
            return false;
        }

        return true;
    }

    public static boolean isCompletelyVisible(final JTextComponent scrollableComponent, final int pos, final Rectangle visibleRect) {
        Rectangle r = modelToView(scrollableComponent, pos);
        return intersects(r, visibleRect);
    }

}
