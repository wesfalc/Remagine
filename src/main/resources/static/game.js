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
            addAdminControls();
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
    else if (type === "GAME_STARTED") {
        appendText = eventDesc;
    }
    else if (type === "NEW_ROUND") {
        appendText = "New round - Round " + eventDesc;
    }
    else if (type === "NEW_TOPIC_SETTER") {
        appendText = "Topic to be set by " + eventDesc;
    }
    else if (type === "TOPIC_SET") {
        appendText = "Topic set to " + eventDesc;
    }
    else if (type === "STORY_SUBMITTED") {
        appendText = eventDesc + " is ready with a story.";
    }

    return appendText;
}

function addAdminControls() {
    let adminTable = document.getElementById("adminTable");

    let newRow;

    newRow = adminTable.insertRow(0);
    let cell1 = newRow.insertCell(0);
    cell1.innerText = "Admin Controls";
    cell1.colSpan = 2;

    newRow = adminTable.insertRow(1);
    cell1 = newRow.insertCell(0);
    cell2 = newRow.insertCell(1);

    let startGameButton = document.createElement('button');
    startGameButton.innerText = "Start Game";
    startGameButton.addEventListener ("click", function() {
        let jsonMessage = {};
        jsonMessage["gameCode"] = gameCode;
        stompClient.send("/game/start/", {}, JSON.stringify(jsonMessage));
        startGameButton.disabled = true;
    });

    cell1.appendChild(startGameButton);

    let nextRoundButton = document.createElement('button');
    nextRoundButton.innerText = "Next Round";
    nextRoundButton.addEventListener("click", function () {
       let jsonMessage = {};
        jsonMessage["gameCode"] = gameCode;
        stompClient.send("/game/nextRound/", {}, JSON.stringify(jsonMessage));
    });

    cell2.appendChild(nextRoundButton);
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
    let text = command.body;
    let json = JSON.parse(text);
    let topicSetter = json["topicSetter"];
    let roundNumber = json["roundNumber"];

    let storyTable = deleteStoryTableRows();

    let rowIndex = storyTable.rows.length;
    let newRow = null;

    newRow = storyTable.insertRow(rowIndex);
    let cell1 = newRow.insertCell(0);
    let cell2 = newRow.insertCell(1);
    cell1.innerText = topicSetter;

    if (topicSetter === currentPlayer) {
        let topicInput = document.createElement('input');
        cell2.appendChild(topicInput);

        let topicSubmitButton = document.createElement('button');
        topicSubmitButton.innerText = "Submit Topic";
        topicSubmitButton.addEventListener ("click", function() {
            let jsonMessage = {};
            jsonMessage["gameCode"] = gameCode;
            jsonMessage["topicSetter"] = topicSetter;
            jsonMessage["topic"] = topicInput.value;
            jsonMessage["round"] = roundNumber;
            storyTable.deleteRow(rowIndex);
            stompClient.send("/game/setTopic/", {}, JSON.stringify(jsonMessage));

        });
        let cell3 = newRow.insertCell(2);
        cell3.appendChild(topicSubmitButton);

        return ;
    }
    else {
        cell2.innerText = "TBD";
    }
}

function deleteStoryTableRows() {
    let storyTable = document.getElementById("storyTable");
    let rowLength = storyTable.rows.length;

    for (let i = 2; i < rowLength; i++) {
        storyTable.deleteRow(i);
    }
    return storyTable;
}

function addPairOfRadioButtons(cell, radioButtonCommonName,
                               firstButtonValue, secondButtonValue) {
    let firstRadioButton = document.createElement('input');
    firstRadioButton.type = "radio";
    firstRadioButton.name = radioButtonCommonName;
    firstRadioButton.checked = true;
    firstRadioButton.value = firstButtonValue;

    let firstLabel = document.createElement('label');
    firstLabel.innerText = firstButtonValue;

    let br1 = document.createElement('br');

    let secondRadioButton = document.createElement('input');
    secondRadioButton.type = "radio";
    secondRadioButton.name = radioButtonCommonName;
    secondRadioButton.checked = false;
    secondRadioButton.value = secondButtonValue;

    let secondLabel = document.createElement('label');
    secondLabel.innerText = secondButtonValue;

    cell.appendChild(firstRadioButton);
    cell.appendChild(firstLabel);
    cell.appendChild(br1);
    cell.appendChild(secondRadioButton);
    cell.appendChild(secondLabel);
    cell.className = "tdAlignLeft";
}

function handleTopicSet(command) {
    let text = command.body;
    let json = JSON.parse(text);

    let topicSetter = json["topicSetter"];
    let roundNumber = json["roundNumber"];
    let topic = json["topic"];

    let storyTable = deleteStoryTableRows();

    let rowIndex = storyTable.rows.length;
    let newRow = null;

    newRow = storyTable.insertRow(rowIndex);
    let cell1 = newRow.insertCell(0);
    let cell2 = newRow.insertCell(1);
    cell1.innerText = topicSetter;
    cell2.innerText = topic;

    rowIndex = storyTable.rows.length;

    if (topicSetter != currentPlayer) {
        newRow = storyTable.insertRow(rowIndex);
        cell1 = newRow.insertCell(0);
        cell2 = newRow.insertCell(1);
        let cell3 = newRow.insertCell(2);
        let cell4 = newRow.insertCell(3);
        let cell5 = newRow.insertCell(4);
        let cell6 = newRow.insertCell(5);

        cell1.innerText = currentPlayer;
        cell2.innerText = "";

        let storyHintInput = document.createElement('input');
        cell3.appendChild(storyHintInput);

        addPairOfRadioButtons(cell4, "storyType", "True", "Imaginary");

        addPairOfRadioButtons(cell5, "likeDislike", "Like", "Dislike");

        let hintSubmitButton = document.createElement('button');
        hintSubmitButton.innerText = "Submit Hint";
        hintSubmitButton.addEventListener ("click", function() {
            let storyType = document.querySelector('input[name="storyType"]:checked').value;
            let likeDislike = document.querySelector('input[name="likeDislike"]:checked').value;

            let storyHint = storyHintInput.value;

            let jsonMessage = {};
            jsonMessage["gameCode"] = gameCode;
            jsonMessage["player"] = currentPlayer;
            jsonMessage["storyHint"] = storyHint;
            jsonMessage["storyType"] = storyType;
            jsonMessage["likeDislike"] = likeDislike;
            jsonMessage["round"] = roundNumber;

            cell3.innerText = storyHint;
            cell4.innerText = storyType;
            cell4.className = "defaultTd";
            cell5.innerText = likeDislike;
            cell5.className = "defaultTd";
            cell6.innerText = "Ready";

            stompClient.send("/game/hintSubmitted/", {}, JSON.stringify(jsonMessage));
        });

        cell6.appendChild(hintSubmitButton);
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

        stompClient.subscribe('/game/topicSet/' + gameCode, function (commmand) {
            handleTopicSet(commmand);
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