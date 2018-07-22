package com.mafia.mafia;

/**
 * Created by Petter on 2017-03-12.
 */

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Client {
    private io.socket.client.Socket socket;
    private String ip;
    private String port;
    private String roomName;
    private String userName;
    private Activity act;
    private LobbyActivity activity;
    private GameActivity gameActivity;

    public Client(String ip, String port, String roomName, String userName, LobbyActivity activity){
        this.ip=ip;
        this.port=port;
        this.roomName = roomName;
        this.userName = userName;
        this.activity = activity;
        this.act = activity;
        connect();
    }

    public Client(Client prevClient, GameActivity activity){
        this.socket = prevClient.socket;
        this.ip = prevClient.ip;
        this.port = prevClient.port;
        this.roomName = prevClient.roomName;
        this.userName = prevClient.userName;
        this.gameActivity = activity;
        this.act = activity;
    }

    /*
    socketOutput = socket.getOutputStream();
                    socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

     */

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                //InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                String socketAddress = "http://" + ip + ":"+port;
                try {
                    socket = IO.socket(socketAddress);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }


                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        join();
                    }

                }).on("event", new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {}

                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {}

                });
                socket.connect();
                startListen();

            }
        }).start();
    }

    public void sendMessage(String message){
        JSONObject obj = new JSONObject();
        System.out.println("SENDING: " + userName + ", " + roomName);
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
            obj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("message", obj);
    }

    public void removeFromRoom(){
        JSONObject obj = new JSONObject();
        System.out.println("removing from room: " + userName + ", " + roomName);
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("leaveRoom", obj);
    }

    public void leaveSocketAndRoom(){
        JSONObject obj = new JSONObject();
        System.out.println("leaving socket: " + userName + ", " + roomName);
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("leaveRoom", obj);
        socket.disconnect();
        System.out.println("Socket from Lobby is disconnected");
    }

    public void join(){
        // Sending an object
        System.out.println("JOINING: " + userName + ", " + roomName);
        JSONObject obj = new JSONObject();
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("join", obj);
        socket.emit("initData", obj);
    }

    public void ready(){
        // Sending an object
        System.out.println("ready: " + userName + ", " + roomName);
        JSONObject obj = new JSONObject();
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("ready", obj);
    }

    public void unready(){
        // Sending an object
        System.out.println("unready: " + userName + ", " + roomName);
        JSONObject obj = new JSONObject();
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("unready", obj);
    }

    public void startListen(){

        // Receiving an object
        socket.on("join", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject)args[0];
                // Should receive userName and roomName
                try {
                    System.out.println("@Client Join: userName = " + obj.get("userName") + ", "+obj.get("roomName"));
                    final String finalMsg = obj.get("userName").toString();
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.joinUser(finalMsg);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                String msg = "";
                String user = "";
                try {
                    msg = obj.get("message").toString();
                    user = obj.get("userName").toString();
                    System.out.println(obj.get("userName"));
                    System.out.println(obj.get("roomName"));
                    System.out.println(obj.get("message"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // ADD MSG TO CHAT
                final String finalMsg = msg;
                final String finalUser = user;
                new Thread() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.addMessage(finalUser, finalMsg);
                            }
                        });
                    }
                }.start();
            }
        });
        socket.on("leave", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    System.out.println("@Client Leave: userName = " + obj.get("userName") + obj.get("roomName"));
                    // ADD MSG TO CHAT
                    final String finalMsg = obj.get("userName").toString();
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.leaveUser(finalMsg);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("update", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    System.out.println("@Client update: connPlayers = " + obj.get("cPlayersNum") + ", readyPlayers = "+ obj.get("rPlayersNum"));
                    final String finalMsg = obj.get("cPlayersNum").toString();
                    final String finalMsg2 = obj.get("rPlayersNum").toString();
                    final String readyUser = obj.get("readyUser").toString();
                    final boolean ready = Boolean.parseBoolean(obj.get("ready").toString());

                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.updateFields(finalMsg, finalMsg2, readyUser, ready);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("connInfo", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    System.out.println("@Client connInfo: cPlayers = " + obj.get("cPlayers") + ", rPlayers = "+ obj.get("rPlayers") + ", playersMax = " +obj.get("playersMax"));
                    final String finalMsg = obj.get("cPlayers").toString();
                    final String finalMsg2 = obj.get("rPlayers").toString();
                    final String finalMsg3 = obj.get("playersMax").toString();
                    final String cPlayersNum = obj.get("cPlayersNum").toString();
                    final String rPlayersNum = obj.get("rPlayersNum").toString();;

                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.setConnInfo(finalMsg, finalMsg2, finalMsg3, cPlayersNum, rPlayersNum);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("allReady", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                new Thread() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.startGame();
                            }
                        });
                    }
                }.start();
            }
        });
    }

}

