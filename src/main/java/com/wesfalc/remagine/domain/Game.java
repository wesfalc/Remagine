package com.wesfalc.remagine.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

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

    public Game(String code) {
        this.code = code;
        history.add(new Event(Event.Type.GAME_CREATED, code));
    }

    public void addPlayer(Player player) {
        players.add(player);
        history.add(new Event(Event.Type.PLAYER_JOINED, player.name()));
    }

    public void host(Player host) {
        history.add(new Event(Event.Type.HOST_JOINED, host.name()));
    }

    public void start() {
        history.add(new Event(Event.Type.GAME_STARTED, "Game started."));
        Collections.shuffle(players);
        nextRound();
    }

    public void nextRound() {
        round ++;
        topicSetter = getTopicSetterForThisRound();
        log.info("Topic to be set by " + topicSetter);
    }

    private Player getTopicSetterForThisRound() {
        playerIndex ++;
        if (playerIndex >= players.size()) {
            playerIndex = 0;
        }
        return players.get(playerIndex);
    }
}
