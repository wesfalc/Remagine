var player = "";
var gameCode = "";

window.onload = function () {
    let cookie = document.cookie;
    let bannerLabel = document.getElementById("banner");
    let nameValue = cookie.split("=");
    let cookieValue = nameValue[1];

    let values = cookieValue.split("-");
    player = values[0];
    gameCode = values[1];

    bannerLabel.innerText = player + " has joined game " + gameCode;

    connect();
    // don't worry about disconnect(). It automatically disconnects when the page is closed.
}

function connect() {
    let socket = new SockJS('/remagine-websocket');
    let stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/game/messages/' + gameCode, function (command) {
            let text = command.body;
            let bannerLabel = document.getElementById("banner");
            bannerLabel.innerText = bannerLabel.innerText + "\n" + text;
        });
    });
}
