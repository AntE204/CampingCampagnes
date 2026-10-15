package utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

public class TimeUtils {
    /**
     * Vérifie le format de la date aaa-mm-dd hh:mm
     * @param date_time La date formattée
     * @return Si la date est correctement formattée
     */
    public static boolean is_date_time_valid(String date_time) {
        return date_time.matches("^\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01]) (?:[01]\\d|2[0-3]):[0-5]\\d$");
    }

    /**
     * Récupère la date du début de la semaine actuelle
     * @return La date du début de la semaine actuelle
     */
    public static LocalDateTime date_now() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .with(LocalTime.MIN);
    }

    /**
     * Récupère la date de la fin de la semaine actuelle
     * @return La date de la fin de la semaine actuelle
     */
    public static LocalDateTime date_next_week() {
        return LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .with(LocalTime.MIN);
    }
}
