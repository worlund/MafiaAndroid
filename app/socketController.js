/* jslint node: true */
"use strict";

var model = require('./model.js');

module.exports = function (socket, io) {

  // user joins room
  socket.on('join', function (req) {
    console.log("@Socket.join : user = " +  req.userName + ", room = " +req.roomName);
    var userName = req.userName;
    var roomName = req.roomName;
    socket.join(roomName);
    socket.join('API_'+userName); // För att skicka private messages
    console.log('A user joined ' + roomName);
    io.to(roomName).emit('join', req);
  });

  socket.on('initData', function (req) {
    var userName = req.userName;
    var roomName = req.roomName;
    var room = model.findRoomGlobal(roomName);
    var result = room.getRoomData();
    io.to('API_'+userName).emit('connInfo', result); //Connection info  
    var result2 = room.updatedData('', true);
    for(var i = 0; i < room.connectedPlayers.length; i++){
      if(room.connectedPlayers[i] !== userName){
        io.to('API_'+room.connectedPlayers[i]).emit('update', result2);
      }
    }
  });


  socket.on('message', function (req) {
    console.log('@Socket msg msg = ' + req.message);
    var roomName = req.roomName;
    var userName = req.userName;
    var message = req.message;
    console.log("@Socket.message RoomName = "+roomName + ", userName = " + userName + ", message = "+message);
    io.to(roomName).emit('message', req);
  });

  // Only leaves room, not socket
  socket.on('leaveRoom', function(req){
    console.log('@Socket Leavesroom');
    var userName = req.userName;
    var roomName = req.roomName;
    var room = model.findRoomGlobal(roomName);
    room.removeUser(userName);
    console.log('A user left ' + roomName);    
    var result2 = room.updatedData('', true);
    io.to(roomName).emit('update', result2);
    io.to(roomName).emit('leave', req);   
  });

  socket.on('leaveGame', function(req){
    console.log('@Socket Leavesroom');
    var userName = req.userName;
    var gameName = req.gameName;
    var game = model.findGameGlobal(gameName);
    game.removeUser(userName);
    console.log('A user left ' + gameName);     
  });

  socket.on('ready', function(req){
    console.log('@ready');
    var userName = req.userName;
    var roomName = req.roomName;
    var room = model.findRoomGlobal(roomName);
    var result = room.playerReady(userName);
    console.log(result);
    if(result){
      if(room.connPlayers !== room.connectedPlayers.length){
        console.log('Error'); // should always be true here
      }else{
        var players = room.connectedPlayers.slice();
        var game = model.addGame(roomName, players, 0, false, []); // dayNumber, isday, deadPlayers  
        var result = game.generateRoles();
        if(result){
          io.to(roomName).emit('allReady', req); // req is redundant, change intent to GameActivity  
        }        
      }
      
    }else{
      io.to(roomName).emit('update', room.updatedData(userName, true));
    }
  });

  socket.on('gameReady', function(req){
    console.log('@gameReady');
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName);
    game.sendStartInfo(userName);
  });
  

  socket.on('unready', function(req){
    console.log('@unready');
    var userName = req.userName;
    var roomName = req.roomName;
    var room = model.findRoomGlobal(roomName);
    room.playerUnready(userName);
    io.to(roomName).emit('update', room.updatedData(userName, false));
  });

  socket.on('getRole', function(req){
    console.log('@getRole');
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    var result = game.getRole(userName);
    console.log('getRole was successful = ' + result);
  });

  socket.on('sendRoleAction', function(req){
    console.log('@sendRoleAction socketController');
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName);
    var userName = req.userName
    var result = game.readyForDay(userName); // SENDING GO DAY FOR LAST PERSON BEFORE PUSH ROLE ACTION
    console.log('successfully ready up: ' + result + ' for ' + userName);
    if(result){
      var userChosen = req.userChosen;
      var rid = req.rid;
      game.pushRoleAction(rid, userChosen, userName);
      console.log('aliveNightRoles = '+game.aliveNightRoles);
      console.log('current player Night count = '+game.rPlayersNight.length);
      if(game.rPlayersNight.length === game.aliveNightRoles){ // CHANGING TO DAY
        console.log('Going to Day from server rPlayersNight.length = '+game.rPlayersNight.length);   
        game.rPlayersNight = [];
        game.rVoteSkip = [];
        game.rVoteProlong = [];
        game.dayNumber++;
        game.checkKills();
        game.updateRoleNight();
        game.goDayReset();
        io.to(game.name).emit('goDay', {result: game.rPlayersNight.length}); 
      }
    }
  });

  socket.on('accuse', function(req){
    console.log('@accuse');
    var userName = req.userName;
    var roomName = req.roomName;
    var userChosen = req.userChosen;
    var game = model.findGameGlobal(roomName); 
    var result = game.accuse(userName, userChosen);
  });

  socket.on('voteToKill', function(req){
    console.log('@voteToKill');
    var userName = req.userName;
    var roomName = req.roomName;
    var userChosen = req.userChosen;
    var game = model.findGameGlobal(roomName); 
    // See if userName already has voted
    if(!game.hasVoted(userName)){
      var result = game.voteToKill(userName, userChosen);
    }
  });

  socket.on('confirmAccuse', function(req){
    console.log('@confirmAccuse');
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    if(game.waitingForConfirm){
      var result = game.confirmAccuse(userName);
    }
  });

  socket.on('noConfirmAccuse', function(req){
    console.log('@noConfirmAccuse');
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    if(game.waitingForConfirm){
      var result = game.noConfirmAccuse(userName);
    }
  });

  socket.on('startVote', function(req){
    console.log('@startVote');
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    io.to(game.name).emit('startVote', ''); 
  });

  

  socket.on('readyForSkip', function(req) {
    console.log("@readyforskip");
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    game.readyForSkip(userName);
  });  

  socket.on('unreadyForSkip', function(req) {
    console.log("@Unreadyforskip");
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    game.unreadyForSkip(userName);
  });

  socket.on('prolongDay', function(req) {
    console.log("@prolongDay");
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    game.readyForProlong(userName);
  });

  socket.on('unprolongDay', function(req) {
    console.log("@unprolongDay");
    var userName = req.userName;
    var roomName = req.roomName;
    var game = model.findGameGlobal(roomName); 
    game.unreadyForProlong(userName);
  });

/*
  // user leaves room and socket
  socket.on('leave', function (req) {
    console.log(req);
    var userName = req.userName;
    var roomName = req.roomName;
    var room = model.findRoom(roomName);
    room.removeUser(user);
    console.log('A user left ' + name);
    io.to(name).emit('leave', user);
    // LEAVE SOCKEt
  });
*/
};
