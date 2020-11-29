package me.nathanp.bubbledrop;

import java.util.ArrayList;
import java.util.List;

public class User {
    String username;
    String email;
    List<String> bubbles;

    public User(){} //Needed for firebase serialization
    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
