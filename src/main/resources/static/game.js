var currentPlayer = "";
var gameCode = "";
var stompClient = null;
var playerIsHost = false;
var currentTopicSetter = null;
var messagesTextArea;

window.onload = function () {

    messagesTextArea = document.getElementById("gameMessagesTextArea");
    messagesTextArea.value = "";

    let joinButton = document.getElementById("joinButton");
    let leaveButton = document.getElementById("leaveButton");
    joinButton.addEventListener ("click", function() {
        connect();
        // don't worry about disconnect(). It automatically disconnects when the page is closed.
        joinButton.disabled = true;
        leaveButton.disabled = false;
    });

    leaveButton.addEventListener ("click", function() {
        joinButton.disabled = false;
        leaveButton.disabled = true;

        let jsonMessage = {};
        jsonMessage["gameCode"] = gameCode;
        jsonMessage["playerName"] = currentPlayer;
        stompClient.send("/game/leave/", {}, JSON.stringify(jsonMessage));

        document.getElementById("gameCode").value="";
        document.getElementById("playerName").value="";

        stompClient.disconnect(function() {
            appendTextMessage("Disconnected.");
        });
    });
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
    else {
        appendText = eventDesc;
    }

    return appendText;
}

function addAdminControls() {
    let adminTable = document.getElementById("adminTable");
    let rowLength = adminTable.rows.length;

    for (let i = rowLength-1; i >=0 ; i--) {
        adminTable.deleteRow(i);
    }

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

    messagesTextArea.value = messagesTextArea.value + "\n" + appendText;
    messagesTextArea.scrollTop = messagesTextArea.scrollHeight;
}

function appendTextMessage(message) {
    if (messagesTextArea.value === "") {
        messagesTextArea.value = message;
    }
    else {
        messagesTextArea.value = messagesTextArea.value + "\n" + message;
    }
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
    console.log("handle new round " + text);
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
    console.log("deleting story table rows");
    let storyTable = document.getElementById("storyTable");
    let rowLength = storyTable.rows.length;

    for (let i = rowLength-1; i >=2 ; i--) {
        storyTable.deleteRow(i);
        console.log("deleted row " + i);
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

    currentTopicSetter = topicSetter;

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

        let storySubmitButton = document.createElement('button');
        storySubmitButton.innerText = "Submit Story";
        storySubmitButton.addEventListener ("click", function() {
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

            stompClient.send("/game/storySubmitted/", {}, JSON.stringify(jsonMessage));
        });

        cell6.appendChild(storySubmitButton);
    }
}

function handleStorySubmitted(command) {
    let text = command.body;
    let json = JSON.parse(text);
    let playerName = json["playerName"];
    let storyHint = json["storyHint"];

    if (currentTopicSetter === currentPlayer) {
        let storyTable = document.getElementById("storyTable");
        let rowIndex = storyTable.rows.length;
        let newRow = storyTable.insertRow(rowIndex);
        let cell1 = newRow.insertCell(0);
        let cell2 = newRow.insertCell(1);
        let cell3 = newRow.insertCell(2);
        let cell4 = newRow.insertCell(3);
        let cell5 = newRow.insertCell(4);
        let cell6 = newRow.insertCell(5);

        cell1.innerText = playerName;
        cell2.innerText = " ";
        cell3.innerText = storyHint;
        addPairOfRadioButtons(cell4, playerName + "-storyType", "True", "Imaginary");
        addPairOfRadioButtons(cell5, playerName + "-likeDislike", "Like", "Dislike");

        let guessSubmitButton = document.createElement('button');
        guessSubmitButton.innerText = "Submit Guess";
        guessSubmitButton.addEventListener ("click", function() {
            let storyType = document.querySelector("input[name=" + CSS.escape(playerName) + "-storyType]:checked").value;
            let likeDislike = document.querySelector("input[name=" + CSS.escape(playerName)+ "-likeDislike]:checked").value;

            let jsonMessage = {};
            jsonMessage["gameCode"] = gameCode;
            jsonMessage["player"] = playerName;
            jsonMessage["storyType"] = storyType;
            jsonMessage["likeDislike"] = likeDislike;
            jsonMessage["guesser"] = currentPlayer;

            cell4.innerText = storyType;
            cell4.className = "defaultTd";
            cell5.innerText = likeDislike;
            cell5.className = "defaultTd";
            cell6.innerText = "Guessed";

            stompClient.send("/game/guessSubmitted/", {}, JSON.stringify(jsonMessage));
        });

        cell6.appendChild(guessSubmitButton);
    }
}

function connect() {
    let socket = new SockJS('/remagine-websocket');
    stompClient = Stomp.over(socket);
    appendTextMessage("Connecting...");
    stompClient.connect({}, function (frame) {
        appendTextMessage("Connected.");

        gameCode = document.getElementById("gameCode").value;
        currentPlayer = document.getElementById("playerName").value;

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

        stompClient.subscribe('/game/newRound/' + gameCode, function (command) {
            handleNewRound(command);
        });

        stompClient.subscribe('/game/topicSet/' + gameCode, function (command) {
            handleTopicSet(command);
        });

        stompClient.subscribe('/game/storySubmitted/' + gameCode, function (command) {
            handleStorySubmitted(command);
        });

        let jsonMessage = {};
        jsonMessage["gameCode"] = gameCode;
        jsonMessage["playerName"] = currentPlayer;
        stompClient.send("/game/join/", {}, JSON.stringify(jsonMessage));

        fetchGameHistory();
    }, function (message) {
        // this is for unintended disconnects
        appendTextMessage("Connection Lost. Message = " + message);
    });

}
function fetchGameHistory() {
    let jsonMessage = {};
    jsonMessage["gameCode"] = gameCode;
    jsonMessage["player"] = currentPlayer;
    stompClient.send("/game/fetchGameHistory/", {}, JSON.stringify(jsonMessage));
}