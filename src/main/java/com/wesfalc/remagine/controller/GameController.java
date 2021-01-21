package com.wesfalc.remagine.controller;

import com.google.gson.Gson;
import com.wesfalc.remagine.domain.Game;
import com.wesfalc.remagine.domain.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class GameController {

    private HashMap<String, Game> games = new HashMap<>();
    Gson gson = new Gson();

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @MessageMapping("/game/setTopic/")
    public void setTopic(String jsonMessage) {
        log.info("Request to set topic - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String topic = (String) map.get("topic");

        Game game = games.get(gameCode);
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

        Game game = games.get(gameCode);
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

        Game game = games.get(gameCode);
        if (game != null) {
            game.submitGuess(player, guesser, storyType, likeDislike);
        }
    }

    @MessageMapping("/game/start/")
    public void startGame(String jsonMessage) {
        log.info("Game started - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");

        Game game = games.get(gameCode);
        if (game != null) {
            game.start();
        }
    }

    @MessageMapping("/game/nextRound/")
    public void nextRound(String jsonMessage) {
        log.info("Next round - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");

        Game game = games.get(gameCode);
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
        if (games.containsKey(gameCode)) {
            game = games.get(gameCode);
            game.addPlayer(player);
        }
        else {
            game = new Game(messagingTemplate, gameCode, player);
            games.put(gameCode, game);
        }

        return game;
    }

    private void leaveGame (String gameCode, String playerName) {
        if (games.containsKey(gameCode)) {
            Player player = new Player();
            player.name(playerName);
            Game game = games.get(gameCode);
            game.playerLeft(player);
        }
    }
}
