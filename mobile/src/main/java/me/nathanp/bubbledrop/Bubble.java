package me.nathanp.bubbledrop;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;
import java.util.List;

@IgnoreExtraProperties
public class Bubble {

    @Exclude
    public static final int TEXT_BUBBLE = 1;
    @Exclude
    public static final int AUDIO_BUBBLE = 2;
    @Exclude
    public static final int PICTURE_BUBBLE = 3;

    int bubbleType;
    String filenameOrText;
    Date date;
    List<String> flags;
    String uid;
    String key;

    // Required for firebase to create bubbles
    public Bubble() {}
    public Bubble(int type, String filename, Date date, String key) {
        this.bubbleType = type;
        this.filenameOrText = filename;
        this.date = date;
        this.key = key;
    }

    // returns a string indicating how long ago this post was made
    protected String elapsedTimeString() {
        long diff = new Date().getTime() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        int daysInt = Math.round(days);
        int hoursInt = Math.round(hours);
        int minutesInt = Math.round(minutes);
        if (daysInt == 1) {
            return "1 day";
        } else if (daysInt > 1) {
            return Integer.toString(daysInt) + " days";
        } else if (hoursInt == 1) {
            return "1 hour";
        } else if (hoursInt > 1) {
            return Integer.toString(hoursInt) + " hours";
        } else if (minutesInt == 1) {
            return "1 minute";
        } else if (minutesInt > 1) {
            return Integer.toString(minutesInt) + " minutes";
        } else {
            return "less than a minute";
        }
    }
}
