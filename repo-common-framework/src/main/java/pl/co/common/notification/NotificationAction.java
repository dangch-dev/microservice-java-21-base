package pl.co.common.notification;

public enum NotificationAction {
    // Ticket
    TICKET_CREATED("Ticket created", "Ticket '%s' created"),
    TICKET_STATUS_UPDATED("Ticket status changed", "Ticket '%s' changed to %s"),
    TICKET_ASSIGNED("Ticket assigned", "Ticket '%s' assigned to %s"),
    TICKET_COMMENT_ADDED("New ticket comment", "New comment on ticket '%s'"),
    // OTHER
    OTHER("Notification", "");

    private final String title;
    private final String message;

    NotificationAction(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String title(String... args) {
        if (args == null || args.length == 0) {
            return title;
        }
        return String.format(title, (Object[]) args);
    }

    public String message(String... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, (Object[]) args);
    }
}
