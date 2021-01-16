package com.wesfalc.remagine.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class Event {
    private Type type;
    public enum Type
    {
        GAME_CREATED,
        GAME_STARTED,
        HOST_JOINED,
        PLAYER_JOINED,
        NEW_ROUND,
        NEW_TOPIC_SETTER,
        TOPIC_SET,
        STORY_SUBMITTED,
    }
    private String description;

    public Event(Type type, String description) {
        this.type = type;
        this.description = description;
    }
}
