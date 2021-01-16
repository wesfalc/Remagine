package com.wesfalc.remagine.domain;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.*;

@Getter
@Setter
@Accessors (fluent = true)
@Slf4j
public class Game {
    private List<Player> players = new ArrayList<>();
    private Map<String, Player> playerMap = new HashMap<>(); // for convenience and due to time contraints!
    private Player host;
    private String code;
    private boolean started = false;
    private boolean finished = true;

    private Player topicSetter;
    private int playerIndex = -1;
    private Round currentRound;

    private List<Event> history = new ArrayList<>();
    Gson gson = new Gson();

    private SimpMessageSendingOperations messagingTemplate;

    public Game(SimpMessageSendingOperations messagingTemplate, String code) {
        this.messagingTemplate = messagingTemplate;
        this.code = code;
        addNewEvent(new Event(Event.Type.GAME_CREATED, code));
    }

    public void addPlayer(Player player) {
        players.add(player);
        playerMap.put(player.name(), player);
        addNewEvent(new Event(Event.Type.PLAYER_JOINED, player.name()));
    }

    public void host(Player host) {
        addNewEvent(new Event(Event.Type.HOST_JOINED, host.name()));
    }

    public void start() {
        addNewEvent(new Event(Event.Type.GAME_STARTED, "Game started."));
        Collections.shuffle(players);
        nextRound();
    }

    private void addNewEvent(Event event) {
        history.add(event);
        messagingTemplate.convertAndSend("/game/messages/" + code, gson.toJson(event));
    }

    public void nextRound() {
        if (currentRound == null) {
            currentRound = new Round();
            currentRound.roundNumber(1);
        }
        else {
            int previousRoundNumber = currentRound.roundNumber();
            currentRound = new Round();
            currentRound.roundNumber(previousRoundNumber + 1);
        }

        topicSetter = getTopicSetterForThisRound();
        currentRound.topicSetter(topicSetter.name());


        addNewEvent(new Event(Event.Type.NEW_ROUND, "" + currentRound.roundNumber()));

        log.info("Topic to be set by " + topicSetter);

        addNewEvent(new Event(Event.Type.NEW_TOPIC_SETTER, topicSetter.name()));

        sendNewRoundMessage();
    }

    public void setTopic(String topic) {
        currentRound.topic(topic);
        log.info("Topic set to " + topic);

        addNewEvent(new Event(Event.Type.TOPIC_SET, topic));
        sendTopicSetMessage();
    }

    private void sendTopicSetMessage() {
        messagingTemplate.convertAndSend("/game/topicSet/" + code, gson.toJson(currentRound));
    }

    private void sendNewRoundMessage() {
        messagingTemplate.convertAndSend("/game/newRound/" + code, gson.toJson(currentRound));
    }

    private Player getTopicSetterForThisRound() {
        playerIndex ++;
        if (playerIndex >= players.size()) {
            playerIndex = 0;
        }
        return players.get(playerIndex);
    }

    public void submitPlayerStory(String playerName, String storyHint, String storyType, String likeDislike) {
        Player player = playerMap.get(playerName);
        if (player == null) {
            log.warn("Received player story for unknown player " + playerName + " gameCode = " + code);
            return;
        }

        boolean liked = false;

        if("Like".toLowerCase().equals(likeDislike.toLowerCase())) {
            liked = true;
        }

        boolean trueStory = false;

        if("True".toLowerCase().equals(storyType.toLowerCase())) {
            trueStory = true;
        }

        Story story = new Story();
        story.storyHint(storyHint);
        story.liked(liked);
        story.trueStory(trueStory);
        story.player(playerName);

        addNewEvent(new Event(Event.Type.STORY_SUBMITTED, playerName));
    }
}
