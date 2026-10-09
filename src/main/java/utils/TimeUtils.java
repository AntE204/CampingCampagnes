package utils;

public class TimeUtils {
    public static boolean is_date_time_valid(String date_time) {
        return date_time.matches("^\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01]) (?:[01]\\d|2[0-3]):[0-5]\\d$");
    }
}
