package com.wesfalc.remagine.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Controller
public class GameController {

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/userJoined", method = {RequestMethod.POST})

    public void userJoined(HttpServletRequest request, HttpServletResponse response, @RequestParam("username") String username) throws IOException {
        log.info("userJoined = " + username);
        request.getSession().setAttribute("username", username);
        response.setStatus(302);
        response.sendRedirect("/randomText.html");
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
