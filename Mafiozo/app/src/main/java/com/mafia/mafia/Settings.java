package com.mafia.mafia;

/**
 * Created by Petter on 2017-03-12.
 */

public class Settings {

    public static String ip = "192.168.1.253";//253";//"192.168.1.72";
    public static String port = "8989";

    private static String userName;

    public static String getUserName(){
        return userName;
    }

    public static void setUserName(String value){
        userName = value;
    }

    public static void setConnectionInfo(String ips, String ports) {
        ip = ips;
        port = ports;
        System.out.println("IP = "+ip+" PORT = "+port);
    }

}
