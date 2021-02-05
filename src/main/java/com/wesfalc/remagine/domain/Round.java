package com.wesfalc.remagine.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors (fluent = true)
public class Round {
    private String topicSetter;
    private String topic = "";
    private int roundNumber;
    private List<Message> messages = new ArrayList<>();

    private transient Map<String, Story> storyMap = new HashMap<>();

    public void addStory(String playerName, Story story) {
        storyMap.put(playerName, story);
    }

    public Story getStory(String playerName) {
        return storyMap.get(playerName);
    }

    public void addMessage(Message message) {
        messages.add(message);
    }
}
