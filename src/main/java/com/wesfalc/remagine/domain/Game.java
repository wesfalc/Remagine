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

    private enum GuessType {
        TRUE_STORY_GUESSED_RIGHT,
        TRUE_STORY_GUESSED_WRONG,
        IMAGINARY_STORY_GUESSED_RIGHT,
        IMAGINARY_STORY_GUESSED_WRONG,
        LIKE_DISLIKE_GUESSED_RIGHT,
        LIKE_DISLIKE_GUESSED_WRONG
    }

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

    public void playerLeft(Player player) {
        players.remove(player);
        playerMap.remove(player.name());
        addNewEvent(new Event(Event.Type.PLAYER_LEFT, player.name()));
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

    private void sendStorySubmittedMessage(Story story) {
        Story storyToSend = new Story(); // don't send all the details

        storyToSend.playerName(story.playerName());
        storyToSend.storyHint(story.storyHint());

        messagingTemplate.convertAndSend("/game/storySubmitted/" + code, gson.toJson(storyToSend));
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
        story.playerName(player.name());

        currentRound.addStory(player.name(), story);

        addNewEvent(new Event(Event.Type.STORY_SUBMITTED, playerName));
        sendStorySubmittedMessage(story);
    }

    public void submitGuess(String player, String guesser, String storyType, String likeDislike) {

        addNewEvent(new Event(Event.Type.STORY_GUESSED, guesser + " guessed that the story by "
                + player + " is " + storyType.toLowerCase() + " and they " + likeDislike.toLowerCase() + " it."));

        Story existingStory = currentRound.getStory(player);

        Story guessedStory = initGuessedStory(guesser, storyType, likeDislike);


        addNewEvent(new Event(Event.Type.STORY_REVEALED, "The story by "
                + player +" is actually " + getTrueOrImaginary(existingStory.trueStory())
                + " and they " + getLikeOrDislike(existingStory.liked()) + " it."));

        scoreTheGuess(existingStory, guessedStory);
        //addNewEvent();
    }

    private void scoreTheGuess(Story existingStory, Story guessedStory) {
        int guesserScore;
        int playerScore;

        String playerName = existingStory.playerName();
        String guesser = guessedStory.playerName();

        if (existingStory.trueStory()) {
            if(guessedStory.trueStory()) {
                // guesser guessed right
                playerScore = getScoreFor(GuessType.TRUE_STORY_GUESSED_RIGHT);
                updateScore(playerName, playerScore);
                guesserScore = 1;
                updateScore(guesser, guesserScore);
            }
            else {
               // guesser guessed wrong
                playerScore = getScoreFor(GuessType.TRUE_STORY_GUESSED_WRONG);
                updateScore(playerName, playerScore);
                guesserScore = 0;
                updateScore(guesser, guesserScore);
            }
        }
        else {
            // imaginary story
            if (!guessedStory.trueStory()) {
                // guesser guessed right
                playerScore = getScoreFor(GuessType.IMAGINARY_STORY_GUESSED_RIGHT);
                updateScore(playerName, playerScore);
                guesserScore = 1;
                updateScore(guesser, guesserScore);
            }
            else {
                // guesser guessed wrong
                playerScore = getScoreFor(GuessType.IMAGINARY_STORY_GUESSED_WRONG);
                updateScore(playerName, playerScore);
                guesserScore = 0;
                updateScore(guesser, guesserScore);
            }
        }

        if (existingStory.liked() == guessedStory.liked()) {
            playerScore = getScoreFor(GuessType.LIKE_DISLIKE_GUESSED_RIGHT);
            updateScore(playerName, playerScore);
            guesserScore = 1;
            updateScore(guesser, guesserScore);
        }
        else {
            playerScore = getScoreFor(GuessType.LIKE_DISLIKE_GUESSED_WRONG);
            updateScore(playerName, playerScore);
            guesserScore = 0;
            updateScore(guesser, guesserScore);
        }
    }

    private void updateScore(String playerName, int playerScore) {
        Player player = playerMap.get(playerName);
        int newScore = player.score() + playerScore;
        player.score(newScore);

        String pointsString = " points.";
        if (playerScore == 1)
        {
            pointsString = " point.";
        }

        addNewEvent(new Event(Event.Type.SCORE_UPDATED, playerName + " got " + playerScore + pointsString));

        Score scoreMessage = new Score();
        scoreMessage.player(playerName);
        scoreMessage.score(newScore);

        sendScore(code, gson.toJson(scoreMessage));
    }

    private void sendScore(String gameCode, String scoreJson) {
        messagingTemplate.convertAndSend("/game/score/" + code, scoreJson);
    }

    private int getScoreFor(GuessType guessType) {
        switch (guessType) {
            case TRUE_STORY_GUESSED_RIGHT:
                return 4;

            case TRUE_STORY_GUESSED_WRONG:
                return 5;

            case IMAGINARY_STORY_GUESSED_RIGHT:
            case LIKE_DISLIKE_GUESSED_WRONG:
                return 3;

            case IMAGINARY_STORY_GUESSED_WRONG:
                return 6;

            case LIKE_DISLIKE_GUESSED_RIGHT:
                return 1;

        }
        return 0;
    }

    private Story initGuessedStory(String guesser, String storyType, String likeDislike) {
        Story story = new Story();
        story.playerName(guesser);
        if("like".equals(likeDislike.toLowerCase())) {
            story.liked(true);
        }
        else {
            story.liked(false);
        }

        if("true".equals(storyType.toLowerCase())) {
            story.trueStory(true);
        }
        else {
            story.trueStory(false);
        }

        return story;
    }

    private String getLikeOrDislike(boolean likeDislike) {
        if(likeDislike) {
            return "like";
        }
        return "dislike";
    }

    private String getTrueOrImaginary(boolean trueOrImaginary) {
        if(trueOrImaginary) {
            return "true";
        }
        return "imaginary";
    }
}
