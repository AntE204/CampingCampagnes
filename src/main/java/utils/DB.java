package utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    private static Connection pdo = null;

    public static boolean init() {
        JSONObject jo = null;

        try {
            Object obj = new JSONParser().parse(new FileReader("./config.json"));
            jo = (JSONObject) obj;
        } catch (FileNotFoundException exc) {
            IO.println("Erreur lors de l'ouverture du fichier config.json : " + exc.getMessage());
            return false;
        } catch (IOException exc) {
            IO.println("Erreur avec le fichier de config.json : " + exc.getMessage());
            return false;
        } catch (ParseException exc) {
            IO.println("Erreur avec le format du config.json : " + exc.getMessage());
            return false;
        }

        String URL = (String) jo.get("URL");
        String LOGIN = (String) jo.get("USER");
        String PASSWORD = (String) jo.get("PASSWORD");

        try {
            DB.pdo = DriverManager.getConnection(URL, LOGIN, PASSWORD);
            return true;
        } catch (SQLException exc) {
            IO.println("Erreur lors de l'initialisation de la base de données : " + exc.getMessage());
            return false;
        }
    }

}
