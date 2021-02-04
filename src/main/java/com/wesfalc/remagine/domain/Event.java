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
        GAME_TERMINATED,
        GAME_TERMINATED_DUE_TO_INACTIVITY,
        GAME_TERMINATED_DUE_TO_TOO_MANY_SIMULTANEOUS_GAMES,
        HOST_JOINED,
        HOST_CHANGED,
        HOST_RECONNECTED,
        PLAYER_JOINED,
        PLAYER_REJOINED,
        PLAYER_LEFT,
        PLAYER_RECONNECTED,
        NEW_ROUND,
        NEW_TOPIC_SETTER,
        TOPIC_SET,
        STORY_SUBMITTED,
        STORY_GUESSED,
        STORY_REVEALED,
        SCORE_UPDATED,
    }
    private String description;

    public Event(Type type, String description) {
        this.type = type;
        this.description = description;
    }
}
