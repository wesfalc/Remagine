package com.wesfalc.remagine.controller;

import com.wesfalc.remagine.domain.Game;
import com.wesfalc.remagine.domain.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

@Slf4j
@Controller
public class GameController {

    private HashMap<String, Game> games = new HashMap<>();

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    private void sendGameMessage(String gameCode, String message) {
        messagingTemplate.convertAndSend("/game/messages/" + gameCode, message);
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

        String playerJoinedMessage = username + " has joined the game.";
        sendGameMessage(gameCode, playerJoinedMessage);
    }

    private Game joinGame(String gameCode, String username) {
        Player player = new Player();
        player.name(username);

        Game game;
        if (games.containsKey(gameCode)) {
            game = games.get(gameCode);
        }
        else {
            game = new Game();
            game.code(gameCode);
            game.host(player);
            games.put(gameCode, game);
        }
        game.addPlayer(player);

        return game;
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

}
