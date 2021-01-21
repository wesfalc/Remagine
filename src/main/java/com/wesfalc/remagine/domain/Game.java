package com.wesfalc.remagine.domain;

import com.google.common.cache.RemovalCause;
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
    private List<Player> activePlayers = new ArrayList<>();
    private Map<String, Player> activePlayerMap = new HashMap<>();
    private Map<String, Player> inactivePlayerMap = new HashMap<>();
    private Player host;
    private String code;
    private boolean started = false;
    private boolean finished = false;

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

    Gson gson = new Gson();

    private SimpMessageSendingOperations messagingTemplate;

    public Game(SimpMessageSendingOperations messagingTemplate, String code, Player host) {
        this.messagingTemplate = messagingTemplate;
        this.code = code;
        host(host);
        addPlayer(host);
        addNewEvent(new Event(Event.Type.GAME_CREATED, code));
    }

    public void addPlayer(Player player) {
        if (activePlayerMap.containsKey(player.name())) {
            addNewEvent(new Event(Event.Type.PLAYER_ALREADY_IN_GAME, player.name()));
            return;
        }

        if(inactivePlayerMap.containsKey(player.name())) {
            Player existingPlayer = inactivePlayerMap.get(player.name());
            activePlayers.add(existingPlayer);
            activePlayerMap.put(existingPlayer.name(), existingPlayer);
            addNewEvent(new Event(Event.Type.PLAYER_REJOINED, player.name()));
            setHostIfNull();
            return;
        }

        activePlayers.add(player);
        activePlayerMap.put(player.name(), player);
        addNewEvent(new Event(Event.Type.PLAYER_JOINED, player.name()));
        setHostIfNull();
    }

    public void playerLeft(Player player) {
        Player existingPlayer = activePlayerMap.get(player.name());
        if(existingPlayer == null) {
            return;
        }

        inactivePlayerMap.put(existingPlayer.name(), existingPlayer);
        activePlayers.remove(existingPlayer);
        activePlayerMap.remove(player.name());
        addNewEvent(new Event(Event.Type.PLAYER_LEFT, player.name()));
        
        if (existingPlayer == host) {
            host = null;
            setHostIfNull();
        }
    }

    private void setHostIfNull() {
        if (host == null) {
            if (activePlayers.size() > 0) {
                host = activePlayers.get(0);
                addNewEvent(new Event(Event.Type.HOST_CHANGED, host.name()));
            }
        }
    }

    private void host(Player host) {
        this.host = host;
        addNewEvent(new Event(Event.Type.HOST_JOINED, host.name()));
    }

    public void start() {
        addNewEvent(new Event(Event.Type.GAME_STARTED, "Game started."));
        Collections.shuffle(activePlayers);
        sendInitialScores();
        nextRound();
    }

    private void addNewEvent(Event event) {
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

    private void sendStoryRevealedMessage(Story story) {
        messagingTemplate.convertAndSend("/game/storyRevealed/" + code, gson.toJson(story));
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
        if (playerIndex >= activePlayers.size()) {
            playerIndex = 0;
        }
        return activePlayers.get(playerIndex);
    }

    public void submitPlayerStory(String playerName, String storyHint, String storyType, String likeDislike) {
        Player player = activePlayerMap.get(playerName);
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

        sendStoryRevealedMessage(existingStory);
        scoreTheGuess(existingStory, guessedStory);
    }

    private void scoreTheGuess(Story existingStory, Story guessedStory) {
        int playerTypeOfStoryPoints;
        int guesserTypeOfStoryPoints;

        int playerLikeDislikePoints;
        int guesserLikeDislikePoints;

        if (existingStory.trueStory()) {
            if(guessedStory.trueStory()) {
                // guesser guessed right
                playerTypeOfStoryPoints = getScoreFor(GuessType.TRUE_STORY_GUESSED_RIGHT);
                guesserTypeOfStoryPoints = 1;
            }
            else {
               // guesser guessed wrong
                playerTypeOfStoryPoints = getScoreFor(GuessType.TRUE_STORY_GUESSED_WRONG);
                guesserTypeOfStoryPoints = 0;
            }
        }
        else {
            // imaginary story
            if (!guessedStory.trueStory()) {
                // guesser guessed right
                playerTypeOfStoryPoints = getScoreFor(GuessType.IMAGINARY_STORY_GUESSED_RIGHT);
                guesserTypeOfStoryPoints = 1;
            }
            else {
                // guesser guessed wrong
                playerTypeOfStoryPoints = getScoreFor(GuessType.IMAGINARY_STORY_GUESSED_WRONG);
                guesserTypeOfStoryPoints = 0;
            }
        }

        if (existingStory.liked() == guessedStory.liked()) {
            playerLikeDislikePoints = getScoreFor(GuessType.LIKE_DISLIKE_GUESSED_RIGHT);
            guesserLikeDislikePoints = 1;
        }
        else {
            playerLikeDislikePoints = getScoreFor(GuessType.LIKE_DISLIKE_GUESSED_WRONG);
            guesserLikeDislikePoints = 0;
        }

        updateScore(existingStory.playerName(), playerTypeOfStoryPoints, playerLikeDislikePoints);
        updateScore(guessedStory.playerName(), guesserTypeOfStoryPoints, guesserLikeDislikePoints);
    }

    private void updateScore(String playerName, int storyTypePoints, int likeDislikePoints) {
        Player player = activePlayerMap.get(playerName);

        if (player == null) {
            log.info("updating score for inactive player " + playerName);
            player = inactivePlayerMap.get(playerName);
        }

        if (player == null) {
            log.warn("player " + playerName + " does not exist in game " + code + " . Cannot update score.");
            return;
        }

        int finalPlayerScore = storyTypePoints + likeDislikePoints;

        int newScore = player.score() + finalPlayerScore;
        player.score(newScore);

        String pointsString = " points.";
        if (finalPlayerScore == 1)
        {
            pointsString = " point.";
        }

        addNewEvent(new Event(Event.Type.SCORE_UPDATED, playerName + " got "
                + storyTypePoints + " + " + likeDislikePoints + " = "
                + finalPlayerScore + pointsString));

        Score scoreMessage = new Score();
        scoreMessage.player(playerName);
        scoreMessage.score(newScore);

        sendScore(code, gson.toJson(scoreMessage));
    }

    private void sendScore(String gameCode, String scoreJson) {
        messagingTemplate.convertAndSend("/game/score/" + code, scoreJson);
    }

    private void sendInitialScores() {
        for(Player player : activePlayers) {
            Score scoreMessage = new Score();
            scoreMessage.player(player.name());
            scoreMessage.score(player.score());
            sendScore(code, gson.toJson(scoreMessage));
        }
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

    public void terminate(RemovalCause cause) {
        finished = true;
        if (cause == RemovalCause.SIZE) {
            log.info("Too many games on server! Game " + code + " terminated.");
            addNewEvent(new Event(Event.Type.GAME_TERMINATED_DUE_TO_TOO_MANY_SIMULTANEOUS_GAMES, "Game terminated. " +
                    "Sorry! Too many games being played simultaneously on the server!"));
        }
        else if (cause == RemovalCause.EXPIRED) {
            log.info("Game " + code + " terminated due to inactivity.");
            addNewEvent(new Event(Event.Type.GAME_TERMINATED_DUE_TO_INACTIVITY, "Game terminated due to inactivity."));
        }
        else {
            log.info("Game terminated. Cause = " + cause);
            addNewEvent(new Event(Event.Type.GAME_TERMINATED, "Game terminated. Cause = " + cause));
        }
    }
}
