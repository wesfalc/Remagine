package com.wesfalc.remagine.domain;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Accessors (fluent = true)
@Slf4j
public class Game {
    private List<Player> players = new ArrayList<>();
    private Player host;
    private String code;
    private boolean started = false;
    private boolean finished = true;

    private Player topicSetter;
    private int playerIndex = -1;
    private int round = 0;
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
        round ++;
        topicSetter = getTopicSetterForThisRound();
        log.info("Topic to be set by " + topicSetter);
        addNewEvent(new Event(Event.Type.NEW_ROUND, "Round " + round));
        Round round = new Round();
        round.topicSetter(topicSetter.name());
        sendNewRoundMessage(round);
    }

    private void sendNewRoundMessage(Round round) {
        messagingTemplate.convertAndSend("/game/newRound/" + code, gson.toJson(round));
    }

    private Player getTopicSetterForThisRound() {
        playerIndex ++;
        if (playerIndex >= players.size()) {
            playerIndex = 0;
        }
        return players.get(playerIndex);
    }
}
