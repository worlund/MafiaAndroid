package com.mafia.mafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Robert on 2017-03-13.
 */

public class StringValidator {
    private Pattern roomNamePattern;
    private Pattern noWhiteSpace;
    private Matcher roomNameMatcher;
    private Matcher noWhiteSpaceMatcher;

    private static final String NO_WHITESPACE = "^\\S+$";
    private static final String ROOMNAME_PATTERN = "^[a-zA-Z0-9]{1,10}$";
    //"^(?!API_)\S+[A-Za-z0-9]$"

    //Initialize pattern with set regex.
    public StringValidator() {
        roomNamePattern = roomNamePattern.compile(ROOMNAME_PATTERN);
        noWhiteSpace = noWhiteSpace.compile(NO_WHITESPACE);
    }

    //Validate given username to set pattern regex
    //return true if match, false if not.
    public boolean validateRoomName(String roomName) {
        roomNameMatcher = roomNamePattern.matcher(roomName);
        noWhiteSpaceMatcher = noWhiteSpace.matcher(roomName);
        System.out.println("@validator = "+roomName);
        if(!noWhiteSpaceMatcher.matches()) {
            System.out.println("ContainsWHITESPACE");
            return false;
        } else if(roomNameMatcher.matches()) {
            System.out.println("Matches!");
            return true;
        }
        return false;
    }
}
