package no.baardl.devops.applicationmap;


import org.slf4j.helpers.MessageFormatter;

import java.util.UUID;

public class DevOpsException extends RuntimeException {
    private final UUID uuid;


    public DevOpsException(String message) {
        super(message);
        this.uuid = UUID.randomUUID();
    }

    public DevOpsException(String message, Throwable throwable) {
        super(message, throwable);
        this.uuid = UUID.randomUUID();
    }

    public DevOpsException(String message, Throwable throwable, Object... parameters) {
        this(MessageFormatter.format(message, parameters).getMessage(), throwable);
    }



    public String getMessage() {
        String var10000 = super.getMessage();
        String message = var10000 + " MessageId: " + this.uuid.toString();
        if (this.getCause() != null) {
            message = message + "\n\tCause: " + this.getCause().getMessage();
        }

        return message;
    }

    public String getMessageId() {
        return this.uuid.toString();
    }


}

