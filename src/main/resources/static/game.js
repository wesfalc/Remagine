var player = "";
var gameCode = "";
var stompClient = null;
var playerIsHost = false;

window.onload = function () {
    let cookie = document.cookie;
    let nameValue = cookie.split("=");
    let cookieValue = nameValue[1];

    let values = cookieValue.split("-");
    player = values[0];
    gameCode = values[1];

    connect();
    // don't worry about disconnect(). It automatically disconnects when the page is closed.
}

function connect() {
    let socket = new SockJS('/remagine-websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/game/messages/' + gameCode, function (command) {
            // do something with game messages
        });

        stompClient.subscribe('/game/messages/' + gameCode + '/' + player, function (command) {
            // these messages are for the individual user
            let text = command.body;
            let json = JSON.parse(text);
            let appendText = text;

            let type = json["type"];
            let eventDesc = json["description"];

            if (type === "HOST_JOINED") {
                if (eventDesc === player) {
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
            let bannerLabel = document.getElementById("banner");
            bannerLabel.innerText = bannerLabel.innerText + "\n" + appendText;
        });
        fetchGameHistory();
    });
}
function fetchGameHistory() {
    console.log("fetch game history");
    let jsonMessage = {};
    jsonMessage["gameCode"] = gameCode;
    jsonMessage["player"] = player;
    stompClient.send("/game/fetchGameHistory/", {}, JSON.stringify(jsonMessage));
}