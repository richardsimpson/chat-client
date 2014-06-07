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
package uk.co.rjsoftware.xmpp.dialogs.exceptions;

import uk.co.rjsoftware.xmpp.dialogs.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionForm extends JDialog {

    public ExceptionForm(final Throwable exception) {
        super(null, "Exception", ModalityType.APPLICATION_MODAL);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final Container pane = getContentPane();
        pane.setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // extract the stack trace from the exception
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        exception.printStackTrace(printStream);

        // create the text arear to display the exception
        final JTextArea exceptionTextArea = new JTextArea(outputStream.toString());
        exceptionTextArea.setEditable(false);
        exceptionTextArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        exceptionTextArea.setWrapStyleWord(true);
        exceptionTextArea.setFont(new JLabel().getFont());

        constraints = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(10, 10, 2, 10), 0, 0);
        pane.add(new JScrollPane(exceptionTextArea), constraints);

        final JPanel buttonPanel =  new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder());
        constraints = new GridBagConstraints(0, 1, 1, 1, 1.0, 0, GridBagConstraints.LINE_END, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
        pane.add(buttonPanel, constraints);

        final JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                okButtonClicked();
            }
        });

        setPreferredSize(new Dimension(670, 300));
        setResizable(true);

        getRootPane().setDefaultButton(okButton);

        pack();

        DialogUtils.centerDialog(this);
    }

    private void okButtonClicked() {
        dispose();
    }
}
