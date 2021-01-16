package com.wesfalc.remagine.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors (fluent = true)
public class Round {
    private String topicSetter;
    private String topic = "";
    private int roundNumber;


    private transient Map<String, Story> storyMap = new HashMap<>();

    public void addStory(String playerName, Story story) {
        storyMap.put(playerName, story);
    }

    public Story getStory(String playerName) {
        return storyMap.get(playerName);
    }
}
