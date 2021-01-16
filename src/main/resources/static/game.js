var currentPlayer = "";
var gameCode = "";
var stompClient = null;
var playerIsHost = false;

window.onload = function () {
    let cookie = document.cookie;
    let nameValue = cookie.split("=");
    let cookieValue = nameValue[1];

    let values = cookieValue.split("-");
    currentPlayer = values[0];
    gameCode = values[1];

    connect();
    // don't worry about disconnect(). It automatically disconnects when the page is closed.
}

function getTextForMessage(json) {
    let appendText = json;
    let type = json["type"];
    let eventDesc = json["description"];

    if (type === "HOST_JOINED") {
        if (eventDesc === currentPlayer) {
            appendText = "You are the host of this game.";
            playerIsHost = true;
        }
        else {
            appendText = eventDesc + " is the host of this game.";
        }
    }
    else if (type === "PLAYER_JOINED") {
        appendText = eventDesc + " joined the game.";
    }
    else if (type === "GAME_CREATED") {
        appendText = "Game created with unique code '" + eventDesc + "'";
    }
    else if (type === "NEW_ROUND") {
        appendText = "New round - " + eventDesc;
    }
    return appendText;
}

function appendMessage(command) {
    let text = command.body;
    let json = JSON.parse(text);
    let appendText = getTextForMessage(json);

    let messagesTextArea = document.getElementById("gameMessagesTextArea");
    messagesTextArea.value = messagesTextArea.value + "\n" + appendText;
    messagesTextArea.scrollTop = messagesTextArea.scrollHeight;
}

function updateScore(command) {
    let text = command.body;

    console.log("Got game score " + text);

    let json = JSON.parse(text);
    let player = json["player"];
    let score = json["score"];
    let scoreboardTable = document.getElementById("scoreboardTable");
    let playerRow = document.getElementById("row-" + player);

    if (playerRow == null) {
        let rowLength = scoreboardTable.rows.length;
        playerRow = scoreboardTable.insertRow(rowLength);
        playerRow.id = "row-" + player;
        playerRow.insertCell(0);
        playerRow.insertCell(1);
    }
    let playerCell = playerRow.cells[0];
    let scoreCell = playerRow.cells[1];
    playerCell.innerText = player;
    scoreCell.innerText = score;
}

function handleNewRound(command) {
    let storyTable = document.getElementById("storyTable");
    let rowLength = storyTable.rows.length;
    for (let i = 2; i < rowLength; i++) {
        storyTable.deleteRow(i);
    }

    let text = command.body;
    let json = JSON.parse(text);
    let topicSetter = json["topicSetter"];
    let topic = json["topic"];
    let roundNumber = json["roundNumber"];

    let rowIndex = storyTable.rows.length;
    let newRow = null;

    newRow = storyTable.insertRow(rowIndex);
    let cell1 = newRow.insertCell(0);
    let cell2 = newRow.insertCell(1);
    cell1.innerText = topicSetter;

    if (topicSetter === currentPlayer) {
        let topicInput = document.createElement('input');
        cell2.appendChild(topicInput);
        return ;
    }
    else {
        cell2.innerText = "TBD";
    }
}

function connect() {
    let socket = new SockJS('/remagine-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/game/messages/' + gameCode, function (command) {
            // do something with game messages
            appendMessage(command);
        });

        stompClient.subscribe('/game/messages/' + gameCode + '/' + currentPlayer, function (command) {
            // these messages are for the individual user
            appendMessage(command);
        });

        stompClient.subscribe('/game/score/' + gameCode, function (command) {
            updateScore(command);
        });

        stompClient.subscribe('/game/newRound/' + gameCode, function (commmand) {
            handleNewRound(commmand);
        });

        fetchGameHistory();
    });
}
function fetchGameHistory() {
    console.log("fetch game history");
    let jsonMessage = {};
    jsonMessage["gameCode"] = gameCode;
    jsonMessage["player"] = currentPlayer;
    stompClient.send("/game/fetchGameHistory/", {}, JSON.stringify(jsonMessage));
}