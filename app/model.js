/* jslint node: true */
"use strict";

/**
 * A module that contains the main system object!
 * @module roomSystem
 */

/*
$name1 = "pettea";
$name2 = "worlund";
$name = $name2;
$pPW = "pRi27J2A";
$rPW = "yCrFMc5j";
$pw = $rPW;
*/

var roomList = [];
var gameList = [];
var Sequelize = require('sequelize');
var bcrypt = require('bcrypt');
var io;
var onlinePlayers = [];

var sequelize;
var users;
var game;
var session;
var gameSession;
var playerSession;
var roles;

exports.init = function(ioParam){
  io = ioParam;
  sequelize = new Sequelize('worlund', 'worlund_admin', 'yCrFMc5j', {
    host: 'mysql-vt2016.csc.kth.se',
    dialect: 'mysql'
  });

  users = sequelize.define('users', {
    uid: {type: Sequelize.INTEGER, primaryKey: true, autoIncrement: true},
    name: Sequelize.STRING,
    pw: Sequelize.CHAR
  }, {
    timestamps: false,
    charset: 'utf8',
    collate: 'utf8_unicode_ci'
  });

  game = sequelize.define('game', {
    gid: {type: Sequelize.INTEGER, primaryKey: true, autoIncrement: true},
    roomName: Sequelize.STRING, 
    amountOfPlayers: Sequelize.INTEGER
  }, {
    timestamps: false,
    charset: 'utf8',
    collate: 'utf8_unicode_ci'
  });


  session = sequelize.define('session', {
    sid: {type: Sequelize.INTEGER, primaryKey: true, autoIncrement: true},
    turn: Sequelize.INTEGER
  }, {
    timestamps: false
  });
  session.belongsTo(game, {foreignKey: 'gid', targetKey: 'gid'});


  playerSession = sequelize.define('playerSession', {
    isAlive: Sequelize.BOOLEAN
  }, {
    timestamps: false
  });
  playerSession.belongsTo(session, {foreignKey: 'sid', primaryKey: true, targetKey: 'sid'});
  playerSession.belongsTo(users, {foreignKey: 'uid', primaryKey: true, targetKey: 'uid'});

  roles = sequelize.define('roles', {
    rid: {type: Sequelize.INTEGER, primaryKey: true},
    roleName: Sequelize.STRING,
    instruct: Sequelize.TEXT,
    info: Sequelize.TEXT
  }, {
    timestamps: false,
    charset: 'utf8',
    collate: 'utf8_unicode_ci'
  });

  gameSession = sequelize.define('gameSession', {
  
  }, {
    timestamps: false
  });
  gameSession.belongsTo(game, {foreignKey: 'gid', primaryKey: true, targetKey: 'gid'});
  gameSession.belongsTo(users, {foreignKey: 'uid', primaryKey: true, targetKey: 'uid'});
  gameSession.belongsTo(roles, {foreignKey: 'rid', targetKey: 'rid'});

}


/**
 * Creates a room with the given name.
 * @param {String} name - The name of the room.
 */
function Room(name, pw, cap, priv) {
    this.name = name;
    //this.messages = [];
    this.connectedPlayers = [];
    this.connPlayers = 0;
    this.readyPlayersArray = [];
    this.readyPlayers = 0;
    this.roles = [];

    this.priv = priv;

    this.pw;
    this.hasPassword;
    this.pwString;

    this.playersMax;
    this.hasCap;

    this.roomTitle;

    if(pw === 'undefined' || pw === '' || pw === null){
      this.hasPassword=false;
      this.pwString = '   ';
    }else{
      this.hasPassword=true;
      this.pw = pw;
      this.pwString = '(*)';
    }
    if(cap === '0' || cap === null){
      this.hasCap = false;
      this.playersMax = '?';
      console.log('zero');
    }else{
      console.log('not zero = ' + cap);
      this.hasCap = true;
      this.playersMax = cap;
    }
    this.roomTitle = this.name + ' ' + this.pwString + ' ' + this.connPlayers + '/' + this.playersMax;
    console.log('@Creating room: ' + this.name + ', ' + this.playersMax + ', ' +cap + ', ' +this.hasCap);

    this.addPlayer = function(name){
      this.connectedPlayers.push(name);
      this.connPlayers++;
    }

    this.getRoomData = function(){
      return JSON.parse('{"cPlayers":"' + this.connectedPlayers+'", "rPlayers":"'+ this.readyPlayersArray+'", "playersMax":"'+ this.playersMax+'", "cPlayersNum":"'+ this.connPlayers+'", "rPlayersNum":"'+ this.readyPlayers+'"}');
    }

    this.updatedData = function(readyUser, isReady){
      return JSON.parse('{"cPlayersNum":"'+ this.connPlayers+'", "rPlayersNum":"'+ this.readyPlayers+ '", "readyUser":"'+ readyUser + '", "ready":"'+isReady+'"}');
    }

    this.playerReady = function(name){
      this.readyPlayers++;
      this.readyPlayersArray.push(name);
      console.log('readyPlayers = playersMax : ' + this.readyPlayers + ' = ' + this.playersMax + ', connectedPlayers = ' +this.connectedPlayers );
      if(this.playersMax === '?'){
        if(this.readyPlayers >= 3 && this.readyPlayers === this.connPlayers){
          return true;
        }
      }
      else if(this.readyPlayers === parseInt(this.playersMax)){
        return true;
      }
      return false;
    }

    this.playerUnready = function(name){
      this.readyPlayers--;
      removeArrayElement(this.readyPlayersArray, name);
    }

    this.setRoomTitle = function(){
      this.roomTitle = this.name + ' ' + this.pwString + ' ' + this.connPlayers + '/' + this.playersMax;
    }

    this.removeUser = function(user){
      removeArrayElement(this.connectedPlayers, user);
      if(removeArrayElement(this.readyPlayersArray, user) === 'True'){
        this.readyPlayers--;
      }
      this.connPlayers--;
      this.roomTitle = this.name + ' ' + this.pwString + ' ' + this.connPlayers + '/' + this.playersMax;
      if(this.connectedPlayers.length === 0){
        removeRoom(this.name); // Should remove room from roomList and memory  
      }
    } 
}

function Game(name, players, dayNumber, isDay, deadPlayers){
  this.name = name;
  this.dayNumber = dayNumber;
  this.isDay = isDay;
  this.roles = [];
  this.players = players;
  console.log('Game created! players.length = ' + this.players.length);
  
  this.amountOfPlayers = players.length;
  this.connPlayers = 0;

  this.alivePlayers = [];
  for(var i = 0; i < this.players.length; i++){
    this.alivePlayers[i] = this.players[i]; 
  }
  this.aliveRoles = [];
  this.aliveNightRoles;

  this.initDeadPlayers = deadPlayers; // only used for init
  this.resultList = [];

  this.accusesLeft = 2;
  this.waitingForConfirm = false;
  this.accuseTimer;
  this.accuseCounter = 0;
  this.voted = [];
  this.votesCounter = [];
  this.secondVoting = false;

  this.rPlayersNight = [];
  this.rVoteSkip = [];
  this.rVoteProlong = [];

  this.tempKilledPlayers = [];
  this.tempSavedPlayers = [];

  this.sendStartInfo = function(userName){
    var result = JSON.parse('{"isDay":"' + this.isDay+'", "dayNumber":"'+ this.dayNumber+'", "alivePlayers":"'+ this.alivePlayers+'"}')
    io.to('API_'+userName).emit('startInfo', result);
    this.connPlayers++;
    console.log('@SendStartInfo user = ' + userName + ', ' + this.connPlayers);
    if(this.connPlayers === this.amountOfPlayers){
      console.log('TIME TO START GAME');
      io.to(this.name).emit('startGame', '');
    }
  }

  this.uberPrint = function(){
    console.log('-------------SHOW ME THE ARRAYS--------------');
    for(var i = 0; i < this.players.length; i++){
      console.log('players['+i+'] = ' + this.players[i]);
    }
    for(var i = 0; i < this.alivePlayers.length; i++){
      console.log('alivePlayers['+i+'] = ' + this.alivePlayers[i]);
    }
    for(var i = 0; i < this.roles.length; i++){
      console.log('roles['+i+'] = ' + this.roles[i]);
    }
    for(var i = 0; i < this.aliveRoles.length; i++){
      console.log('aliveRoles['+i+'] = ' + this.aliveRoles[i]);
    }
    console.log('-------------SHOW ME THE ARRAYS--------------');
  }

  this.generateRoles = function(){
    this.roles = getRoles(this.amountOfPlayers);
    for(var i = 0; i < this.initDeadPlayers.length; i++){
      removeDeadPlayer(this.initDeadPlayers[i]);
    }
    shuffle(this.roles);  
    this.aliveRoles = this.roles.slice();
    this.uberPrint();
    this.updateRoleNight();
    console.log('Roles: ' + this.roles.length);
    console.log(this.roles);
    return true;
  }

  this.getRole = function(user){
    if(this.roles.length !== this.players.length){
      console.log('Roles and players differ in length, shouldnt happen');
      console.log(this.roles.length + ", " + this.players.length);
    }
    var result = 'False';
    for(var i = 0; i < this.players.length; i++){
      if(this.players[i] === user){
        var roleName = this.roles[i];
        sendRoleJSON(roleName, this.players[i]);   
        result = 'True';
        break;
      }   
    }
    return result;
  }

  this.removeDeadPlayer = function(user){
    console.log('@removeDeadPlayer Printing user to remove with role. players.length = ' + this.players.length + ', ');
    for(var i = 0; i < this.alivePlayers.length; i++){
      if(user === this.alivePlayers[i]){
        console.log('\t\t FIND  ME : ' + this.alivePlayers[i] + ': ' +this.aliveRoles[i]);
        this.alivePlayers.splice(i,1);
        this.aliveRoles.splice(i, 1);   
        break; 
      }
    }
    console.log('After removeDeadPlayer = ' + this.players.length);
  }

  this.killPlayer = function(user, killer){
    console.log('@killPlayer user = ' +user + ', killer = ' + killer);
    this.removeDeadPlayer(user);
    io.to(this.name).emit('playerDied', {user: user, killer: killer}); 
    var end = this.checkIfGameOver();
    if(end !== 'NotOver'){
      console.log('RESULT TABLE: ');
      var firstLine = 'Name   Role   Status';
      console.log(firstLine);
      console.log('Print: '+this.connPlayers +' = this.connPlayers , this.players.length = ' + this.players.length);
      for(var i = 0; i < this.connPlayers; i++){
        var res = this.players[i];
        res = res + ' ' + this.roles[i];
        var endedAlive = false;
        for(var j = 0; j < this.alivePlayers.length; j++){
          if(this.players[i] === this.alivePlayers[j]){
            endedAlive = true;
            break;
          }
        }
        if(endedAlive){
          res = res + ' Alive';
        }
        else{
          res = res + ' Dead';
        }
        console.log(res);
        this.resultList.push(res);
      }
      this.uberPrint();
      io.to(this.name).emit('gameEnded', {message: end, result: this.resultList}); 
    }
  }

  // Måste callas varje gång nån dör, håller koll på hur många roller som måste calla under natten
  this.updateRoleNight = function(){
    console.log('@updateRoleNight Listing all alive roles: ');
    var tempCounter = 0;
    for(var i = 0; i < this.aliveRoles.length; i++){
      console.log('\t ' + this.aliveRoles[i]);
      if(this.aliveRoles[i] === 'Mafia' || this.aliveRoles[i] === 'Cop' || this.aliveRoles[i] === 'Medic' || this.aliveRoles[i] === 'Crazy Cop') {// Night roles
        tempCounter++;
      }
    }
    console.log('\tSETTING aliveNightRoles = '+ tempCounter);
    this.aliveNightRoles = tempCounter;
  }

  this.checkIfGameOver = function(){
    // Check if any side has won
    var end = 'NotOver';
    var mafiaIsAlive = false;

    for(var i = 0; i < this.aliveRoles.length; i++){
      if(this.aliveRoles[i] === 'Mafia'){
        mafiaIsAlive = true;
      }
    }
    if(!mafiaIsAlive){
      end = 'The Town Wins!';
    }
    else if(this.aliveRoles.length <= 2){ // är 2 rätt?
      end = 'The Mafia Wins!';
    }
    console.log('@checkIfGameOver end = '+ end);
    return end;
  }

  this.copCheck = function(userChosen, userName, isCrazy){
    // Kolla userChosen role
    var result;
    for(var i = 0; i < this.players.length; i++){
      if(userChosen === this.players[i]){
        result = this.roles[i];
        break;
      }
    }
    var string;
    var guilty = userChosen + ' seems suspicious';
    var innocent = userChosen + ' seems trustworthy';
    if(isCrazy){
      if(result === 'Mafia'){
        string = innocent;
      }else{
        string = guilty;
      }
    }else{ // normal cop
      if(result === 'Mafia'){
        string = guilty;
      }else{
        string = innocent;
      }
    }
    console.log('returning data to Cop');
    console.log('@copCheck: isCrazy = ' + isCrazy + ', message = ' + string);
    io.to('API_'+userName).emit('copInfo', {result: string}); 
  }

  this.goDayReset = function(){
    this.voted = [];
    this.votesCounter = [];
    this.accusesLeft = 2;
    this.secondVoting = false;
    for(var i = 0; i < this.alivePlayers.length + 1; i++){ // För att få satt för default option också
      this.votesCounter[i] = 0;
    }
  }

  // Bör anropas när det byter från natt till dag, alt precis innan
  this.checkKills = function(){
    var madeKill = false;
    //Remove duplicates
    for(var i = 0; i < this.tempKilledPlayers.length; i++){
      var userChosen = this.tempKilledPlayers[i];
      var isSaved = false;
      for(var j = 0; j < this.tempSavedPlayers.length; j++){
        if(this.tempSavedPlayers[i] === userChosen){
          isSaved = true;
          break;
        }
      }
      if(!isSaved){
        this.killPlayer(userChosen, 'Mafia');
        madeKill = true;
      }
    }
    if(!madeKill){
      io.to(this.name).emit('globalMessage',{message:'Noone was killed during the night'});
    }
    this.tempKilledPlayers = [];
    this.tempSavedPlayers = [];
  }

  this.addToBeKilled = function(userChosen){
    if(this.tempKilledPlayers.indexOf(userChosen) === -1){
      this.tempKilledPlayers.push(userChosen);
    }
  }

  // Handles role actions : Makes action and then pushes either directly or after night
  // Push up the action that are gonna occur when night time is over
  this.pushRoleAction = function(rid, userChosen, userName){
    console.log('@model pushRoleAction rid = ' + rid);
    switch(rid){
      case 1: // Mafia
        // Lägg till system för voting av vem döda för multiple mafias
        this.addToBeKilled(userChosen);
        break;
      case 2: // Cop
        this.copCheck(userChosen, userName, false);
        break;
      case 3: // Medic
        this.tempSavedPlayers.push(userChosen);
        break;
      case 5: // Sheriff
        this.killPlayer(userChosen, 'Sheriff-'+userName);
        break;
      case 6: // Crazy Cop
        this.copCheck(userChosen, userName, true);
        break;
    }
  }

  this.removeUser = function(userName) {
      removeArrayElement(this.players, userName);
      this.connPlayers--;
      this.roomTitle = this.name + ' ' + this.pwString + ' ' + this.connPlayers + '/' + this.playersMax;
      if(this.players.length === 0){
        removeGame(this.name); // Should remove room from roomList and memory  
      }
  }


  // 2 accuses per day, needs to be confirmed to be valid
  this.accuse = function(userName, userChosen){
    this.accuseCounter = 0;
    this.waitingForConfirm = true;
    for(var i = 0; i < this.alivePlayers.length; i++){
      io.to('API_'+this.alivePlayers[i]).emit('accuseStarted', {userName: userName, userChosen: userChosen});
    }
    // Start timeout until all votes count as a no
    // Ta bort om det krånglar atm
    clearTimeout(this.accuseTimer);
    this.accuseTimer = setTimeout(this.cancelAccuse , 20000);
  }  

  // 2 accuses per day, needs to be confirmed to be valid
  this.confirmAccuse = function(userName){
    this.waitingForConfirm = false;
    clearTimeout(this.accuseTimer);
    console.log('Should only work for one user : ' + userName);
    io.to(this.name).emit('accuseMade', {userName: userName});
  }  

  this.noConfirmAccuse = function(userName){
      this.accuseCounter++;
      console.log('@noConfirmAccuse accuseCounter = ' +this.accuseCounter +', alivePlayers = ' +this.alivePlayers.length);
      if(this.accuseCounter === this.alivePlayers-2){ // All but two can vote, one who votes and one who get voted on
          clearTimeout(this.accuseTimer);
          this.cancelAccuse();
      }
  }

  this.cancelAccuse = function(){
    this.waitingForConfirm = false;
    io.to(this.name).emit('accuseNotMade', '');
  }

  this.voteToKill = function(userName, userChosen){
    // Find index of userChosen to increase their voteNumber
    console.log('TIME TO VOTE');

    var index = this.alivePlayers.length + 1; // Satt default för no choice
    for(var i = 0; i < this.alivePlayers.length; i++){
        if(this.alivePlayers[i] === userChosen){
          index = i;
          break;
        }
    }
    this.votesCounter[index] = this.votesCounter[index] + 1;
    this.voted.push(userName);
    if(this.voted.length === this.alivePlayers.length){
      // Vote is done, tally votes
      this.makeVerdict();
    }
  }
  
  this.hasVoted = function(userName){
    if(this.voted.indexOf(userName) !== -1){
      // Already voted
      return true;
    }
    return false;
  }

  this.makeVerdict = function(){
    // Counts votes, checks for majority
    var majorityNum = Math.floor(this.alivePlayers.length / 2) + 1; // Is default round down?
    console.log('@makeVerdict majorityNum = ' + majorityNum);
    var voteSucceeded = false;
    var playerToKill = '';
    console.log('votesCounter.length = ' + this.votesCounter.length);
    for(var i = 0; i < this.votesCounter.length ;i++){
      console.log('\t votesCounter[i] = ' + this.votesCounter[i]);
      if(this.votesCounter[i] >= majorityNum){
        if(i === this.alivePlayers.length+1){
          // Should be used in voteTallying as a minority outcome
          console.log('No majority found, No vote was majority');
          break;
        }else{
          // Save vote
          voteSucceeded = true; 
          playerToKill = this.alivePlayers[i];
          console.log(this.alivePlayers[i] + ' has majority votes with ' + this.votesCounter[i]);
          break;
        }
      }
    }
    // Checks if voteSucceeded
    if(voteSucceeded){ // Kills player
      console.log('@makeVerdict voteSucceeded');
      this.killPlayer(playerToKill, 'Town');
      this.updateRoleNight();
      io.to(this.name).emit('goNight', {message: 'After the successful vote, night comes'});
    }else{ // Vote fail through not voting / nothing being majority
      console.log('@makeVerdict voteFailed');
      if(this.secondVoting){
        io.to(this.name).emit('goNight', {message: 'No successful vote was made, night comes'});
      }else{
        this.allowSecondVoting();
      }
    }
  }

  this.allowSecondVoting = function(){
    this.voted = [];
    this.votesCounter = [];
    this.accusesLeft = 1;
    this.secondVoting = true;
    io.to(this.name).emit('secondVoting','');
  }

  this.readyAdder = function(array, user, add){
    var result = false;
    if(add){
      if(array.indexOf(user) !== -1){
        console.log('Player already ready');
        result = false;
      }else{
        array.push(user);
        result = true;
      }
    }
    else{ // Remove element
      if(removeArrayElement(array, user) === 'True'){
        // Removed user successfully
        result = true;
      }else{
        console.log('User not removed');
        result = false;
      }
    }
    return result;
  }

  this.readyForDay = function(user){
    var result = this.readyAdder(this.rPlayersNight, user, true);
    console.log('ready for day : ' + user);
    return result;
  }

  this.readyForSkip = function(user){
    var result = this.readyAdder(this.rVoteSkip, user, true);
    console.log('ready for skip : ' + user);
    if(this.rVoteSkip.length === this.alivePlayers.length){
      console.log('Skipping Day from server');
      io.to(this.name).emit('skipDay', {result: this.rVoteSkip.length}); 
    }else{
      console.log('new updated number skip = ' + result);
      io.to(this.name).emit('updateSkip', {result: this.rVoteSkip.length}); 
    }
  }

  this.readyForProlong = function(user){
    var result = this.readyAdder(this.rVoteProlong, user, true);
    console.log('ready for prolong : ' + user);
    if(this.rVoteProlong.length === this.alivePlayers.length){
      console.log('Prolonging Day from server');
      io.to(this.name).emit('prolongDay', {result: this.rVoteProlong.length}); 
      this.rVoteProlong = [];
    }else{
      console.log('new updated number prolong = ' + this.rVoteProlong.length);
      io.to(this.name).emit('updateProlong', {result: this.rVoteProlong.length}); 
    }
  }

  this.unreadyForSkip = function(user){
    var result = this.readyAdder(this.rVoteSkip, user, false);
    console.log('unready for skip : ' + user);
    console.log('new updated number prolong = ' + result);
    io.to(this.name).emit('updateSkip', {result: this.rVoteSkip.length}); 
  }

  this.unreadyForProlong = function(user){
    var result = this.readyAdder(this.rVoteProlong, user, false);
    console.log('unready for prolong : ' + user);
    console.log('new updated number prolong = ' + result);
    io.to(this.name).emit('updateProlong', {result: this.rVoteProlong.length}); 
  }

} // END OF GAME


/*
 roles = sequelize.define('roles', {
    rid: {type: Sequelize.INTEGER, primaryKey: true},
    roleName: Sequelize.STRING,
    instruct: Sequelize.STRING,
    info: Sequelize.STRING
  }, {
    timestamps: false,
    charset: 'utf8',
    collate: 'utf8_unicode_ci'
  });
  */
function sendRoleJSON(roleName, user){
  roles
  .findOne({
    where: {
      roleName: roleName
    }, 
  })
  .then(function(result) {
    console.log('ROLES TO SEND:');
    //console.log(result);
    io.to('API_'+user).emit('role', result); 
  });
       
}

exports.getRoleInfo = function(res) {
  console.log('@GETROLEINFO');
  roles
  .findAll()
  .then(function(result) {
    //console.log(result);
    res.json({list: result});
  });
}

function getRoles(num){
  var roles = [];
  if(num < 3){
    console.log('invalid');
  }else{
    for(var i = 0; i < Math.ceil(num/6) ;i++){
      console.log('Adding Mafia');
      roles.push('Mafia');  
    }
    roles.push('Cop');
    for(var i = 0; i < Math.floor(num/7) ;i++){
      var secondCop = Math.floor((Math.random() * 3) + 1);
      console.log('Adding Second Cop');
      if(secondCop === 1){
        roles.push('Crazy Cop');
      }
      else{
        roles.push('Cop');
      }   
    }
    roles.push('Medic');
    for(var i = 0; i < Math.floor(num/9) ;i++){
      console.log('Adding Sheriff');
      roles.push('Sheriff')
    }
    var rolesLeft = num - roles.length;
    for(var i = 0; i < rolesLeft; i++){
      console.log('Adding Townfolk');
      roles.push('Townfolk');  
    }
  }
  return roles;
}

/**
 * Shuffles array in place. ES6 version
 * @param {Array} a items The array containing the items.
 */
function shuffle(a) {
    for (let i = a.length; i; i--) {
        let j = Math.floor(Math.random() * i);
        [a[i - 1], a[j]] = [a[j], a[i - 1]];
    }
}

function removeArrayElement(array, element){
  console.log('@RemoveArrayElement model.js with ' + element);
  var result = 'False';
  for(var i = 0; i < array.length; i++){
    if(element === array[i]){
      array.splice(i, 1);   
      result = 'True';
      break; 
    }
  }
  return result;
}

exports.addGame = function(name, players, dayNumber, isDay, deadPlayers){ // gameName
  var newGame = new Game(name, players, dayNumber, isDay, deadPlayers);
  gameList.push(newGame);
  return newGame;
}

/**
 * Creates a room with the given name.
 * @param {String} name - The name of the room.
 */
exports.addRoomGlobal = function (name, pw, cap, priv, res) {
  addRoom(name, pw, cap, priv);
  return 'True';

};

function addRoom(name, pw, cap, priv) {
  var newName = name;
  var nameTaken = false;
  for(var j = 1; j < 20; j++){
    for(var i = 0; i < roomList.length; i++){
      if(roomList.name === newName){
        nameTaken = true;
        break;
      }
    }
    if(nameTaken){
      newName = name + '(' +j+')';
    }
    else{
      break;
    }
    nameTaken= false;
  }

  // If more than 20 have same, its gonna break
  var newRoom = new Room(newName, pw, cap, priv);
  roomList.push(newRoom);
};

// From online users, used in login
exports.removeUser = function(name){
  return removeArrayElement(onlinePlayers, name);
}

exports.findRoomsByPW = function(pw){
  var tempRooms = [];
  console.log(pw);
  for(var i = 0; i < roomList.length; i++){
    console.log('@findRoomsByPW : ' + roomList[i].name + '('+roomList[i].pw+')');
    if(roomList[i].pw === pw){
      console.log('adding: ' + roomList[i].name);
      tempRooms.push(roomList[i]);
    }
  }
  return tempRooms;
}

exports.joinRoomPW = function(name, pw, roomName){
  var room = findRoom(roomName);
  console.log(room);
  var success = 'False';
  if(room.hasPassword){ // Should always be true if we are here
    if(!room.hasCap || room.connPlayers < room.playersMax){
      if(pw === room.pw){
        room.addPlayer(name);
        room.setRoomTitle();  
        success = 'True';
      }
    }else{
      console.log("Conn players = "+room.connPlayers);
    }
  }
  return success;
}


exports.joinRoom = function(name, roomName){
  var room = findRoom(roomName);
  var resultString = '';
  if(room.hasPassword){
    resultString = 'RequiresPW';
  }else{
    if(room.hasCap && room.connPlayers >= room.playersMax){
      resultString = 'RoomFull';
    }else{
      room.addPlayer(name);
      room.setRoomTitle();  
      resultString = 'Success';
    }  
  }
  return resultString;
}

function getTheseRoomsP(rooms){ // Room list prints
  var titleList = [];
  for(var i = 0; i < rooms.length; i++){
    if(rooms[i].priv === false){
      titleList.push(rooms[i].roomTitle);
    }  
  }
  return titleList;
}

exports.getTheseRooms = function(rooms){
  return getTheseRoomsP(rooms);
}

/**
 * Returns all the Rooms.
 */
exports.getRooms = function() {
  return getTheseRoomsP(roomList);
  //return roomList;
};

/**
 * Removes the room object with the matching name.
 * @param {String} name - The name of the room.
 */
exports.removeRoomGlobal = function(name){
  removeRoom(name);
};

function removeRoom(name){
  var res = false;
  for (var i = 0; i < roomList.length; i++) {
    var room = roomList[i];
    if (room.name === name) {
      roomList.splice(i, 1);
      room = null;
      res = true;
      break;
    }
  }
  return res;
}

function removeGame(name) {
  var res = false;
  for (var i = 0; i < gameList.length; i++) {
    var game = gameList[i];
    if (game.name === name) {
      gameList.splice(i, 1);
      game = null;
      res = true;
      break;
    }
  }
  return res;
}

exports.findGameGlobal = function(name){
  for (var i = 0; i < gameList.length; i++) {
    if (gameList[i].name === name) {
      return gameList[i];
    }
  }
}

/**
 * Return the room object with the matching name.
 * @param {String} name - The name of the room.
 */
exports.findRoomGlobal = function(name) {
  return findRoom(name);
};

function findRoom(name){
  for (var i = 0; i < roomList.length; i++) {
    if (roomList[i].name === name) {
      //console.log(roomList[i]);
      return roomList[i];
    }
  }
}




exports.addGameID = function(amountOfPlayers){

}


// Login


exports.tryLogin = function(name, pw, res){ 

  if(onlinePlayers.indexOf(name) !== -1){
    res.json({loginSuccess: "LoggedInElsewhere"});
  }
  else{
    users
    .findOne({
      where: {
        name: name
      }, 
    })
    .then(function(result) {
        if(result === null){
          res.json({loginSuccess: "False"});
        }else{
          var hash = result.pw;
          console.log("HASH = " + hash);
          bcrypt.compare(pw, result.pw, function(err, resu) {
            if(resu === true){ // true?
              console.log("WE MAKE IT? : " + resu);
              onlinePlayers.push(name);

              res.json({loginSuccess: "True"});
            }else{
              console.log("COMPARE FAILED");
              res.json({loginSuccess: "False"});
            }
          })
        }
      });
  }
  
}

exports.registerUser = function(name, pw, res){
  
  users
  .findOne({
    where: {
      name: name
    }, 
  })
  .then(function(result) {
      if(result === null){
        console.log("NO USER FOUND WITH THIS NAME?");
        bcrypt.hash(pw, 10, function(err, hash) {
          users
          .create({ name: name, pw: hash})
          .then(function(result){
            res.json({success: "True"}); 
          })        
        })
      } else{
        console.log("User already exists");
        res.json({success: "False"});
      }
    });
}



/*
exports.getRoomMessages = function(roomName, res) {
  var result = findRoom(roomName).messages;
  res.json({list: result});
};

function socketUpdateMessages(roomName){
    var result = findRoom(roomName).messages;
    io.to(roomName).emit('update', result);
};
*/
