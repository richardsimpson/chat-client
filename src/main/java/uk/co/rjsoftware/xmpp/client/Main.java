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
package uk.co.rjsoftware.xmpp.client;

import uk.co.rjsoftware.xmpp.dialogs.exceptions.ExceptionForm;
import uk.co.rjsoftware.xmpp.dialogs.login.LoginForm;
import uk.co.rjsoftware.xmpp.dialogs.login.LoginListener;
import uk.co.rjsoftware.xmpp.dialogs.main.MainForm;
import uk.co.rjsoftware.xmpp.model.LogoutListener;
import org.jivesoftware.smack.XMPPException;

import javax.swing.*;
import java.io.IOException;

// TODO: Add Jersey to the licence information

public final class Main {

    private LoginForm loginForm;
    private YaccProperties yaccProperties;
    private int maxRoomCount = -1;

    public static void main(String [ ] args) throws XMPPException, InterruptedException {
        new Main(args);
    }

    private Main(final String [ ] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // setup the global exception handler.
        final ExceptionHandler exceptionHandler = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

        for (String arg : args) {
            final String[] currentArg = arg.split("=");
            if (currentArg[0].equals("maxRoomCount")) {
                this.maxRoomCount = Integer.parseInt(currentArg[1]);
            }
        }

        try {
            this.yaccProperties = new YaccProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowLoginForm();
            }
        });
    }

    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            final ExceptionForm exceptionForm = new ExceptionForm(exception);
            exceptionForm.setVisible(true);
        }
    }
    // TODO: Bug Fix: If logout, then close the login form, the app doesn't terminate.  It does if close login form without logging in.

    private void createAndShowLoginForm() {
        this.loginForm = new LoginForm();
        this.loginForm.addLoginListener(new LoginListener() {
            @Override
            public void loginAttempt(String username, String password) {
                try {
                    createAndShowMainForm(username, password);
                } catch (YaccException exception) {
                    Main.this.loginForm.setLoginMessage(exception.getMessage());
                }
            }
        });
        this.loginForm.setVisible(true);
    }

    private void createAndShowMainForm(String username, String password) throws YaccException {
        final CustomConnection connection = new CustomConnection(username, password, this.maxRoomCount);

        // TODO: Check if this is the correct way to 'close' a JFrame
        this.loginForm.setVisible(false);

        final MainForm mainForm = new MainForm("YACC", connection, yaccProperties);
        mainForm.addLogoutListener(new LogoutListener() {
            @Override
            public void logout() {
                mainForm.setVisible(false);
                Main.this.loginForm.setVisible(true);
            }
        });
        mainForm.setVisible(true);
    }
}
