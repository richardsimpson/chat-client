package uk.co.rjsoftware.xmpp.model;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public enum RoomPrivacy {

    PUBLIC("public", "group-chat.png"),
    PRIVATE("private", "private-chat.png");

    private String description;
    private final ImageIcon imageIcon;

    RoomPrivacy(final String description, final String filename) {
        this.description = description;

        final InputStream inputStream = this.getClass().getResourceAsStream(filename);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[2048];

        try {
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.imageIcon = new ImageIcon(buffer.toByteArray(), this.description);
    }

    public String getDescription() {
        return this.description;
    }

    public ImageIcon getImageIcon() {
        return this.imageIcon;
    }

    public static RoomPrivacy fromDescription(final String description) {
        for (RoomPrivacy privacy : RoomPrivacy.values()) {
            if (privacy.description.equals(description)) {
                return privacy;
            }
        }

        return null;
    }
}
