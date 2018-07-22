package com.mafia.mafia;

/**
 * Created by Robert on 2017-03-07.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Class to validate usernames and passwords.
public class LoginValidator {
    private Pattern pattern;
    private Pattern pwPattern;
    private Matcher matcher;

    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9]{3,15}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$";

    //Initialize pattern with set regex.
    public LoginValidator() {
        pattern = pattern.compile(USERNAME_PATTERN);
        pwPattern = pwPattern.compile(PASSWORD_PATTERN);
    }

    //Validate given username to set pattern regex
    //return true if match, false if not.
    public boolean validate(String username) {
        matcher = pattern.matcher(username);
        return matcher.matches();
    }

    public boolean validatePassword(String password) {
        matcher = pwPattern.matcher(password);
        return matcher.matches();
    }
}

