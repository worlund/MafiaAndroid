/* jslint node: true */
"use strict";

var express = require('express');
var router = express.Router();
var model = require("./model.js");
var bcrypt = require('bcrypt');
/*
router.get('/roomlist', function (req, res) {
  var rooms = model.getRooms();
  var roomNames = [];
  for (var i = 0; i < rooms.length; i++) {
    roomNames.push(rooms[i]);
  }
  res.json({list:roomNames});
});
*/
/*
router.get('/tradesList/:trades', function (req, res) {
  model.findTrades(req.params.trades,res);
});

router.get('/orderList/:order', function (req, res) {
  model.findOrders(req.params.order, res);
});
*/

router.get('/rooms', function(req,res) {
  var list = model.getRooms();
  console.log("List of rooms: ");
  console.log(list);
  res.json({list: list});
});

router.post('/matchLogin', function (req, res) {
  console.log("@matchlogin");
  console.log(req.body);
  var name = req.body.name;
  console.log("Checking for user: " + name); 
  var pw = req.body.pw;
  model.tryLogin(req.body.name, pw, res);
});

router.post('/registerUser', function (req, res) {
  var name = req.body.name;
  console.log("Trying to register user: " + name); 
  var pw = req.body.pw;
  console.log("(PASSWORD DONT LOOK) " + pw);
  model.registerUser(req.body.name, pw, res);
});

router.post('/createRoom', function (req, res){
  var pw = req.body.pw;
  if(pw === '' || typeof pw === 'undefined'){
    console.log("No password @createRoom");    
  }
  else{
    console.log("password is set @createRoom");
  }
  console.log('Creating room with these : ' +req.body.room + ', ' +req.body.pw +', '+req.body.cap + ', ' + req.body.private);
  var result = model.addRoomGlobal(req.body.room, req.body.pw , req.body.cap, req.body.private);
  res.json({success: result});
});

router.post('/joinRoom', function (req,res) {
  console.log('Join room');
  var result = model.joinRoom(req.body.name, req.body.room);
  res.json({result: result});
});

router.post('/joinRoomWPassword', function (req,res) {
  console.log('Joining room by pw with these : ' +req.body.name + ', ' +req.body.pw +', '+req.body.room);
  var result = model.joinRoomPW(req.body.name, req.body.pw, req.body.room);
  res.json({result: result});
});

router.post('/joinRoomByPW', function (req,res) { // Should receive {name: , pw: }
  console.log("@joinRoomByPW");
  console.log(req.body);
  console.log(req.body.pw);
  var rooms = model.findRoomsByPW(req.body.pw);
  console.log('rooms ' +rooms);
  if(rooms.length === 0){
    res.json({result: 'NoRoomFound'});
  }else if(rooms.length > 1){
    var titles = model.getTheseRooms(rooms);
    res.json({result: 'ManyRoomsFound', rooms: titles});
  }else if(rooms.length === 1){
    var result = model.joinRoomPW(req.body.name, rooms[0].pw, rooms[0].name);
    res.json({result: 'OneFound', innerRes: result, room: rooms[0].name});
  }
});

router.post('/removeUser', function(req,res) {
  var user = req.body.name;
  var result = model.removeUser(user);
  res.json({success: result});
});

router.post('/startGame', function(req,res) {
  model.addGameID();
});

router.get('/roleInfo', function(req, res){
  console.log('@controller roleInfo');
  model.getRoleInfo(res);
}); 

module.exports = router;
