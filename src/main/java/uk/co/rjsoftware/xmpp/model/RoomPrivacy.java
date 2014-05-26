package uk.co.rjsoftware.xmpp.model;

public enum RoomPrivacy {

    PUBLIC("public"),
    PRIVATE("private");

    private String description;

    RoomPrivacy(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
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
