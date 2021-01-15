var player = "";
var game = "";

window.onload = function () {
    let cookie = document.cookie;
    let bannerLabel = document.getElementById("banner");
    let nameValue = cookie.split("=");
    let cookieValue = nameValue[1];

    let values = cookieValue.split("-");
    player = values[0];
    game = values[1];

    bannerLabel.innerText = player + " has joined game " + game;
    console.log("huh player = " + player + " game = " + game);
}