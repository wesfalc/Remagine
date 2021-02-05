package com.wesfalc.remagine.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Data
@Getter
@Setter
@Accessors (fluent = true)
public class Message {
    private String destination;
    private Object content;

    public Message(String destination, Object messageContent) {
        this.destination = destination;
        this.content = messageContent;
    }
}
