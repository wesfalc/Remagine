var currentPlayer = "";
var gameCode = "";
var stompClient = null;
var playerIsHost = false;
var currentTopicSetter = null;
var messagesTextArea;
var textForTrue      = "True\u00a0\u00a0\u00a0\u00a0\u00a0";
var textForImaginary = "Imaginary";
var textForLike    = "Like\u00a0\u00a0\u00a0";
var textForDislike = "Dislike";

window.onload = function () {

    messagesTextArea = document.getElementById("gameMessagesTextArea");
    messagesTextArea.value = "";

    let joinButton = document.getElementById("joinButton");
    let leaveButton = document.getElementById("leaveButton");
    joinButton.addEventListener ("click", function() {
        gameCode = document.getElementById("gameCode").value.trim();
        currentPlayer = document.getElementById("playerName").value.trim();

        if (gameCode == null || gameCode ==="" || currentPlayer == null || currentPlayer === "") {
            return;
        }

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
        removeAdminTableRows();
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
            addAdminControls(true);
        }
        else {
            appendText = eventDesc + " is the host of this game.";
        }
    }
    else if (type === "HOST_CHANGED") {
        if (eventDesc === currentPlayer) {
            appendText = "You are the new host of this game.";
            playerIsHost = true;
            addAdminControls(false);
        }
        else {
            appendText = eventDesc + " is the new host of this game.";
        }
    }
    else if (type === "HOST_RECONNECTED") {
        if (eventDesc === currentPlayer) {
            appendText = "You are the host of this game.";
            playerIsHost = true;
            addAdminControls(false);
        }
        else {
            appendText = eventDesc + " is the host of this game.";
        }
    }
    else if (type === "PLAYER_JOINED") {
        appendText = eventDesc + " joined the game.";
    }
    else if (type === "PLAYER_REJOINED") {
        appendText = eventDesc + " rejoined the game.";
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
    else if (type === "PLAYER_RECONNECTED") {
        appendText = eventDesc + " reconnected. ";
    }
    else if (type === "PLAYER_LEFT") {
        appendText = eventDesc + " left the game. ";
    }
    else {
        appendText = eventDesc;
    }

    return appendText;
}

function removeAdminTableRows() {
    let adminTable = document.getElementById("adminTable");
    let rowLength = adminTable.rows.length;

    for (let i = rowLength-1; i >=0 ; i--) {
        adminTable.deleteRow(i);
    }
    return adminTable;
}

function addAdminControls(startGame) {
    let adminTable = removeAdminTableRows();

    let newRow;

    newRow = adminTable.insertRow(0);
    let cell1 = newRow.insertCell(0);
    cell1.innerText = "Admin Controls";
    cell1.colSpan = 2;

    newRow = adminTable.insertRow(1);
    cell1 = newRow.insertCell(0);
    let cell2 = newRow.insertCell(1);

    if (startGame) {

        let startGameButton = document.createElement('button');
        startGameButton.innerText = "Start Game";
        startGameButton.className = "joinButton";
        startGameButton.addEventListener("click", function () {
            let jsonMessage = {};
            jsonMessage["gameCode"] = gameCode;
            stompClient.send("/game/start/", {}, JSON.stringify(jsonMessage));
            startGameButton.disabled = true;
        });

        cell1.appendChild(startGameButton);
    }

    let nextRoundButton = document.createElement('button');
    nextRoundButton.className = "joinButton";
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
        playerRow.className = "scoreboardTr";
        playerRow.insertCell(0);
        playerRow.insertCell(1);
    }
    let playerCell = playerRow.cells[0];
    playerCell.className = "scoreboardTd";
    let scoreCell = playerRow.cells[1];
    scoreCell.className = "scoreboardTd";
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
    newRow.className = "storyTr";
    let cell1 = newRow.insertCell(0);
    let cell2 = newRow.insertCell(1);
    cell1.innerText = topicSetter;

    if (topicSetter === currentPlayer) {
        let topicInput = document.createElement('input');
        topicInput.className = "standardInput";
        cell2.appendChild(topicInput);

        let topicSubmitButton = document.createElement('button');
        topicSubmitButton.className = "joinButton";
        topicSubmitButton.innerText = "Submit Topic";
        topicSubmitButton.addEventListener ("click", function() {
            let topic = topicInput.value;
            if(topic == null || topic === "") {
                return;
            }

            let jsonMessage = {};
            jsonMessage["gameCode"] = gameCode;
            jsonMessage["topicSetter"] = topicSetter;
            jsonMessage["topic"] = topic;
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

    for (let i = rowLength-1; i >=1 ; i--) {
        storyTable.deleteRow(i);
        console.log("deleted row " + i);
    }
    return storyTable;
}

function addPairOfRadioButtons(cell, radioButtonCommonName, firstButtonText,
                               firstButtonValue, secondButtonText, secondButtonValue) {
    let firstRadioButton = document.createElement('input');
    firstRadioButton.type = "radio";
    firstRadioButton.name = radioButtonCommonName;
    firstRadioButton.checked = true;
    firstRadioButton.value = firstButtonValue;

    let firstLabel = document.createElement('label');
    firstLabel.innerText = firstButtonText;

    let br1 = document.createElement('br');

    let secondRadioButton = document.createElement('input');
    secondRadioButton.type = "radio";
    secondRadioButton.name = radioButtonCommonName;
    secondRadioButton.checked = false;
    secondRadioButton.value = secondButtonValue;

    let secondLabel = document.createElement('label');
    secondLabel.innerText = secondButtonText;

    cell.appendChild(firstRadioButton);
    cell.appendChild(firstLabel);
    cell.appendChild(br1);
    cell.appendChild(secondRadioButton);
    cell.appendChild(secondLabel);
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
    newRow.className = "storyTr";
    let cell1 = newRow.insertCell(0);
    let cell2 = newRow.insertCell(1);
    cell1.innerText = topicSetter;
    cell2.innerText = topic;

    rowIndex = storyTable.rows.length;

    if (topicSetter != currentPlayer) {
        newRow = storyTable.insertRow(rowIndex);
        newRow.className = "storyTr";
        cell1 = newRow.insertCell(0);
        cell2 = newRow.insertCell(1);
        let cell3 = newRow.insertCell(2);
        let cell4 = newRow.insertCell(3);
        let cell5 = newRow.insertCell(4);
        let cell6 = newRow.insertCell(5);

        cell1.innerText = currentPlayer;
        cell2.innerText = "";

        let storyHintInput = document.createElement('input');
        storyHintInput.className = "standardInput";
        cell3.appendChild(storyHintInput);

        addPairOfRadioButtons(cell4, "storyType",
            textForTrue, "True", textForImaginary, "Imaginary");


        addPairOfRadioButtons(cell5, "likeDislike",
            textForLike, "Like", textForDislike, "Dislike");


        let storySubmitButton = document.createElement('button');
        storySubmitButton.className = "joinButton";
        storySubmitButton.innerText = "Submit Story";
        storySubmitButton.addEventListener ("click", function() {
            let storyType = document.querySelector('input[name="storyType"]:checked').value;
            let likeDislike = document.querySelector('input[name="likeDislike"]:checked').value;

            let storyHint = storyHintInput.value;

            if (storyHint == null || storyHint ==="") {
                return;
            }

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

    let cell1, cell2, cell3, cell4, cell5, cell6;

    if (currentTopicSetter === currentPlayer || playerName != currentPlayer) {
        let storyTable = document.getElementById("storyTable");
        let rowIndex = storyTable.rows.length;
        let newRow = storyTable.insertRow(rowIndex);
        newRow.className = "storyTr";
        newRow.id = "storyOf" + playerName;
        cell1 = newRow.insertCell(0);
        cell2 = newRow.insertCell(1);
        cell3 = newRow.insertCell(2);
        cell4 = newRow.insertCell(3);
        cell5 = newRow.insertCell(4);
        cell6 = newRow.insertCell(5);
    }

    if (currentTopicSetter === currentPlayer) {
        cell1.innerText = playerName;
        cell2.innerText = " ";
        cell3.innerText = storyHint;
        addPairOfRadioButtons(cell4, playerName + "-storyType",
                  textForTrue, "True", textForImaginary, "Imaginary");

        addPairOfRadioButtons(cell5, playerName + "-likeDislike",
            textForLike, "Like", textForDislike, "Dislike");

        let guessSubmitButton = document.createElement('button');
        guessSubmitButton.className = "joinButton";
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
    else if (playerName != currentPlayer) {
        cell1.innerText = playerName;
        cell2.innerText = " ";
        cell3.innerText = storyHint;
        cell4.innerText = "?";
        cell5.innerText = "?";
        cell6.innerText = "Ready";
    }
}

function handleStoryRevealed(command) {
    let text = command.body;
    let json = JSON.parse(text);
    let playerName = json["playerName"];
    let storyHint = json["storyHint"];
    let trueStory = json["trueStory"];
    let liked = json["liked"];

    let rowId= "storyOf" + playerName;
    let row = document.getElementById(rowId);

    if(row != null) {
        row.cells[3].innerText = getTrueOrImaginary(trueStory);
        row.cells[4].innerText = getLikeOrDislike(liked);
    }
}

function getLikeOrDislike(likeDislike) {
    if(likeDislike) {
        return "Like";
    }
    return "Dislike";
}

function getTrueOrImaginary(trueOrImaginary) {
    if(trueOrImaginary) {
        return "True";
    }
    return "Imaginary";
}

function connect() {
    let socket = new SockJS('/remagine-websocket');
    stompClient = Stomp.over(socket);
    appendTextMessage("Connecting...");
    stompClient.connect({}, function (frame) {
        appendTextMessage("Connected.");

        // common messages
        stompClient.subscribe('/game/messages/' + gameCode, function (command) {
            // do something with game messages
            appendMessage(command);
        });

        // for the individual user (intended for message replay)
        stompClient.subscribe('/game/messages/' + gameCode + '/' + currentPlayer, function (command) {
            appendMessage(command);
        });

        // common messages
        stompClient.subscribe('/game/score/' + gameCode, function (command) {
            updateScore(command);
        });

        // for the individual user (intended for message replay)
        stompClient.subscribe('/game/score/' + gameCode + '/' + currentPlayer, function (command) {
            updateScore(command);
        });

        // common messages
        stompClient.subscribe('/game/newRound/' + gameCode, function (command) {
            handleNewRound(command);
        });

        // for the individual user (intended for message replay)
        stompClient.subscribe('/game/newRound/' + gameCode + '/' + currentPlayer, function (command) {
            handleNewRound(command);
        });

        // common messages
        stompClient.subscribe('/game/topicSet/' + gameCode, function (command) {
            handleTopicSet(command);
        });

        // for the individual user (intended for message replay)
        stompClient.subscribe('/game/topicSet/' + gameCode + '/' + currentPlayer, function (command) {
            handleTopicSet(command);
        });

        // common messages
        stompClient.subscribe('/game/storySubmitted/' + gameCode, function (command) {
            handleStorySubmitted(command);
        });

        // for the individual user (intended for message replay)
        stompClient.subscribe('/game/storySubmitted/' + gameCode + '/' + currentPlayer, function (command) {
            handleStorySubmitted(command);
        });

        // common messages
        stompClient.subscribe('/game/storyRevealed/' + gameCode, function (command) {
            handleStoryRevealed(command);
        });

        // for the individual user (intended for message replay)
        stompClient.subscribe('/game/storyRevealed/' + gameCode + '/' + currentPlayer, function (command) {
            handleStoryRevealed(command);
        });

        let jsonMessage = {};
        jsonMessage["gameCode"] = gameCode;
        jsonMessage["playerName"] = currentPlayer;
        stompClient.send("/game/join/", {}, JSON.stringify(jsonMessage));

    }, function (message) {
        // this is for unintended disconnects
        appendTextMessage("Connection Lost. Message = " + message);
        setTimeout(function() {
                connect();
            }, 5000);
    });

}