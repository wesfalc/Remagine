package com.wesfalc.remagine.controller;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.wesfalc.remagine.domain.Game;
import com.wesfalc.remagine.domain.Player;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class GameController {

    private static final int MAXIMUM_SIMULTANEOUS_GAMES = 10000;
    private static final int INACTIVITY_TIMEOUT_MINUTES = 60;
    private Cache<String, Game> games;
    Gson gson = new Gson();

    public GameController() {
        games = CacheBuilder.newBuilder()
                .maximumSize(MAXIMUM_SIMULTANEOUS_GAMES)
                .expireAfterAccess(INACTIVITY_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .removalListener(removalNotification -> {
                    Game game = (Game) removalNotification.getValue();
                    game.terminate(removalNotification.getCause());
                })
                .concurrencyLevel(4)
                .recordStats()
                .build();
    }

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/game/setTopic/")
    public void setTopic(String jsonMessage) {
        log.info("Request to set topic - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String topic = (String) map.get("topic");

        if (StringUtils.isBlank(topic)) {
            log.warn("Topic is blank for game code " + gameCode);
            return;
        }

        Game game = games.getIfPresent(gameCode);
        if (game != null) {
            game.setTopic(topic);
        }
    }

    @MessageMapping("/game/storySubmitted/")
    public void storySubmitted(String jsonMessage) {
        log.info("Story submitted - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String player = (String) map.get("player");
        String storyHint = (String) map.get("storyHint");
        String storyType = (String) map.get("storyType");
        String likeDislike = (String) map.get("likeDislike");

        if (StringUtils.isBlank(storyHint)) {
            log.warn("Story hint is blank for game code " + gameCode);
            return;
        }

        Game game = games.getIfPresent(gameCode);
        if (game != null) {
            game.submitPlayerStory(player, storyHint, storyType, likeDislike);
        }
    }

    @MessageMapping("/game/guessSubmitted/")
    public void guessSubmitted(String jsonMessage) {
        log.info("Guess submitted - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String player = (String) map.get("player");
        String storyType = (String) map.get("storyType");
        String likeDislike = (String) map.get("likeDislike");
        String guesser = (String) map.get("guesser");

        Game game = games.getIfPresent(gameCode);
        if (game != null) {
            game.submitGuess(player, guesser, storyType, likeDislike);
        }
    }

    @MessageMapping("/game/start/")
    public void startGame(String jsonMessage) {
        log.info("Game started - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");

        Game game = games.getIfPresent(gameCode);
        if (game != null) {
            game.start();
        }
    }

    @MessageMapping("/game/nextRound/")
    public void nextRound(String jsonMessage) {
        log.info("Next round - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");

        Game game = games.getIfPresent(gameCode);
        if (game != null) {
            game.nextRound();
        }
    }

    @MessageMapping("/game/join/")
    public void joinGame(String jsonMessage) {
        log.info("Player joining game - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        gameCode = gameCode.trim();
        String playerName = (String) map.get("playerName");
        playerName = playerName.trim();

        if (StringUtils.isBlank(gameCode)) {
            log.warn("Game code is blank.");
            return;
        }

        if(StringUtils.isBlank(playerName)) {
            log.warn("Player name is blank for game code " + gameCode);
            return;
        }

        joinGame(gameCode, playerName);
    }

    @MessageMapping("/game/leave/")
    public void leave(String jsonMessage) {
        log.info("Player leaving game - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String playerName = (String) map.get("playerName");

        leaveGame(gameCode, playerName);
    }

    private Game joinGame(String gameCode, String playerName) {
        Player player = new Player();
        player.name(playerName);

        Game game;
        if (games.getIfPresent(gameCode) != null) {
            game = games.getIfPresent(gameCode);
            game.playerJoined(player);
        }
        else {
            game = new Game(messagingTemplate, gameCode, player);
            games.put(gameCode, game);
        }

        return game;
    }

    private void leaveGame (String gameCode, String playerName) {
        if (games.getIfPresent(gameCode) != null) {
            Player player = new Player();
            player.name(playerName);
            Game game = games.getIfPresent(gameCode);
            game.playerLeft(player);
        }
    }
}
