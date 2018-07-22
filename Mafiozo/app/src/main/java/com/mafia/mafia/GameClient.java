package com.mafia.mafia;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by Robert on 2017-03-14.
 */

public class GameClient {
    private io.socket.client.Socket socket;
    private String ip;
    private String port;
    private String roomName;
    private String userName;
    private GameActivity activity;


    public GameClient(String ip, String port, String roomName, String userName, GameActivity activity) {
        this.ip = ip;
        this.port = port;
        this.roomName = roomName;
        this.userName = userName;
        this.activity = activity;
        connect();
    }

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                //InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
                String socketAddress = "http://" + ip + ":" + port;
                System.out.println("SOCKET ="+socket);
                try {
                    socket = IO.socket(socketAddress);
                    System.out.println("SOCKET SUCCESS? ="+socket);
                    if(socket== null) {
                        System.out.println("WTF");
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                System.out.println("SOCKET ="+socket);

                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        emitter("join");
                        emitter("getRole");
                        emitter("gameReady");
                    }

                }).on("event", new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                    }

                }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                    }

                });
                socket.connect();
                startListen();

            }
        }).start();
    }

    public void emitter(String tag) {
        System.out.println(tag+": " + userName + ", " + roomName);
        JSONObject obj = new JSONObject();
        try {
            obj.put("userName", userName);
            obj.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit(tag, obj);
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

    public void sendRoleAction(int rid, String userChosen){
        JSONObject obj = new JSONObject();
        System.out.println("SENDING ROLE ACTION: " + userName + ", " + roomName + ", " + rid + ", " + userChosen);
        try {
            obj.put("roomName", roomName);
            obj.put("userName", userName);
            obj.put("userChosen", userChosen);
            obj.put("rid", rid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("sendRoleAction", obj);
    }

    public void accuse(String userChosen){
        JSONObject obj = new JSONObject();
        System.out.println("ACCUSE: " + userName + ", " + roomName + ", " + userChosen);
        try {
            obj.put("roomName", roomName);
            obj.put("userName", userName);
            obj.put("userChosen", userChosen);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("accuse", obj);
    }

    public void voteToKill(String userChosen){
        JSONObject obj = new JSONObject();
        System.out.println("VOTE TO KILL: " + userName + ", " + roomName + ", " + userChosen);
        try {
            obj.put("roomName", roomName);
            obj.put("userName", userName);
            obj.put("userChosen", userChosen);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("voteToKill", obj);
    }

    public void leaveSocketAndRoom(){
        JSONObject obj = new JSONObject();
        System.out.println("leaving socket: " + userName + ", " + roomName);
        try {
            obj.put("userName", userName);
            obj.put("gameName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("leaveGame", obj);
        socket.disconnect();
    }

    public void startListen() {

        // Receiving an object
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

        socket.on("globalMessage", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    //System.out.println("@Client Leave: userName = " + obj.get("userName") + obj.get("roomName"));
                    // ADD MSG TO CHAT
                    final String finalMsg = obj.get("message").toString();
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.addMessage(finalMsg);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

        socket.on("playerDied", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    final String userFinal = obj.get("user").toString();
                    final String killer = obj.get("killer").toString();
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.playerDied(userFinal, killer);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("role", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    System.out.println("@GameClient role: "+obj.get("roleName") + ", " +obj.get("instruct") + ", " + obj.get("info") + ", " + obj.get("rid"));
                    final String roleNameString = obj.get("roleName").toString();
                    final String instruct = obj.get("instruct").toString();
                    //final String finalMsg3 = obj.get("info").toString();
                    final String rid = obj.get("rid").toString();
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.role(rid, roleNameString, instruct);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("startInfo", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];
                // Should receive userName and roomName
                try {
                    System.out.println("@GameClient startInfo: "+obj.get("isDay") + ", " +obj.get("dayNumber") + ", " + obj.get("alivePlayers"));
                    final boolean isDayF = Boolean.parseBoolean(obj.get("isDay").toString());
                    final int dayNumberF = Integer.parseInt(obj.get("dayNumber").toString());
                    String tempArrayString = obj.get("alivePlayers").toString();
                    String[] tempS = tempArrayString.split(","); // Borde ge array?
                    final ArrayList<String> aliveF = new ArrayList<String>();
                    for(String s : tempS){
                        aliveF.add(s);
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.initStartInfo(isDayF, dayNumberF, aliveF);
                                }
                            });
                        }
                    }.start();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("startGame", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("@startGame GameClient");;
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
        socket.on("updateSkip", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String amount = obj.get("result").toString();
                    System.out.println("@updateSkip amount = " + amount);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.voteButtonChange(amount);
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        socket.on("updateProlong", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String amount = obj.get("result").toString();
                    System.out.println("@updateProlong amount = " + amount);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.prolongedButtonChange(amount);

                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("copInfo", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String message = obj.get("result").toString();
                    System.out.println("@copInfo amount = " + message);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.copInfo(message);
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("gameEnded", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String message = obj.get("message").toString();
                    final String tempArrayString = obj.get("result").toString();
                    System.out.println("@gameEnded msg = " + message);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.gameEnding(message, tempArrayString);
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("accuseStarted", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String userNameField = obj.get("userName").toString();
                    final String userChosenField = obj.get("userChosen").toString();
                    System.out.println("@accuseStarted msg = " + userNameField +", "+userChosenField);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.accuseStarted(userNameField, userChosenField);
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("accuseMade", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String userNameField = obj.get("userName").toString();
                    System.out.println("@accuseStarted msg = " + userNameField);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.accuseConfirmed(userNameField);
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("accuseNotMade", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("@accuseNotMade GameClient");;
                new Thread() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.accuseNotMade();
                            }
                        });
                    }
                }.start();
            }
        });
        socket.on("skipDay", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String result = obj.get("result").toString();
                    System.out.println("@skipday result = " + result);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.voteButtonChange(result);
                                    activity.skipDay();
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("prolongDay", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String result = obj.get("result").toString();
                    System.out.println("@prolongDay result = " + result);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.prolongedButtonChange(result);
                                    activity.prolongDayTime();
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("goDay", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("@GoDay GameClient");;
                new Thread() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.startDay();
                            }
                        });
                    }
                }.start();
            }
        });
        socket.on("goNight", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = (JSONObject) args[0];

                try{
                    final String result = obj.get("message").toString();
                    System.out.println("@goNight result = " + result);
                    // Should receive userName and roomName
                    new Thread() {
                        @Override
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.startNightString(result);
                                }
                            });
                        }
                    }.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        socket.on("secondVoting", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("@GoDay GameClient");;
                new Thread() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.secondVoting();
                            }
                        });
                    }
                }.start();
            }
        });
        socket.on("startVote", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("@startVote GameClient");;
                new Thread() {
                    @Override
                    public void run() {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.startVote();
                            }
                        });
                    }
                }.start();
            }
        });
    }
}
