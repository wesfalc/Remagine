package com.wesfalc.remagine.controller;

import com.google.gson.Gson;
import com.wesfalc.remagine.domain.Event;
import com.wesfalc.remagine.domain.Game;
import com.wesfalc.remagine.domain.Player;
import com.wesfalc.remagine.domain.Score;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class GameController {

    private HashMap<String, Game> games = new HashMap<>();
    Gson gson = new Gson();

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private void sendGameMessage(String gameCode, Object message) {
        messagingTemplate.convertAndSend("/game/messages/" + gameCode, message);
    }

    private void sendPlayerMessage(String gameCode, String player, Object message) {
        messagingTemplate.convertAndSend("/game/messages/" + gameCode+"/" + player, message);
    }

    private void sendScore(String gameCode, String scoreJson) {
        messagingTemplate.convertAndSend("/game/score/" + gameCode, scoreJson);
    }

    private void sendGameHistory(Game game, String player) {
        for(Event event : game.history()) {
            sendPlayerMessage(game.code(), player, gson.toJson(event));
        }
    }

    @MessageMapping("/game/fetchGameHistory/")
    public void fetchGameHistory(String jsonMessage) {
        log.info("Request to fetch game history for game " + jsonMessage);

        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String player = (String) map.get("player");

        Game game = games.get(gameCode);
        if (game != null) {
            sendGameHistory(game, player);
        }
    }

    @MessageMapping("/game/setTopic/")
    public void setTopic(String jsonMessage) {
        log.info("Request to set topic - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String topicSetter = (String) map.get("topicSetter");
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

        Game game = joinGame(gameCode, playerName);
    }

    @MessageMapping("/game/leave/")
    public void leave(String jsonMessage) {
        log.info("Player leaving game - " + jsonMessage);
        Map map = gson.fromJson(jsonMessage, Map.class);
        String gameCode = (String) map.get("gameCode");
        String playerName = (String) map.get("playerName");

        leaveGame(gameCode, playerName);
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/userJoined", method = {RequestMethod.POST})
    public void userJoined(HttpServletRequest request, HttpServletResponse response, @RequestParam("gamecode") String gameCode,
            @RequestParam("username") String username) throws IOException {
        log.info("gameCode = " + gameCode + " userJoined = " + username);
        Game game = joinGame(gameCode, username);

        request.getSession().setAttribute("gamecode", game.code());
        request.getSession().setAttribute("username", username);
        response.setStatus(302);
        Cookie cookie = new Cookie("remagine", username + "-" + gameCode);
        response.addCookie(cookie);
        response.sendRedirect("/home.html");

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

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/randomText", method = {RequestMethod.POST})
    public void randomText(HttpServletRequest request, HttpServletResponse response, @RequestParam("randomText") String text) throws IOException {
        String username = (String) request.getSession().getAttribute("username");

        if (StringUtils.isEmpty(username)) {
            log.info("user needs to register.");
            response.setStatus(302);
            response.sendRedirect("/");
        } else {
            log.info("username = " + username + " text = " + text);
            response.setStatus(302);
            response.sendRedirect("/randomText.html");
        }
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/test/{gameCode}/{player}/{score}")
    public void addTestScore(@PathVariable ("gameCode") String gameCode, @PathVariable ("player") String player, @PathVariable("score") int score) throws IOException {
        log.info("received test score " + score + " for player " + player + " for game " + gameCode);

        Score playerScore = new Score();
        playerScore.player(player);
        playerScore.score(score);

        sendScore(gameCode, gson.toJson(playerScore));
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/test/{gameCode}/nextRound")
    public void testStartNewRound(@PathVariable ("gameCode") String gameCode) throws IOException {
        Game game = games.get(gameCode);
        game.nextRound();
    }

}
