package models;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Config {
    private String error = null;
    private JSONObject json_obj = null;

    public Config(String path) {
        try {
            Object obj = new JSONParser().parse(new FileReader(path));
            this.json_obj = (JSONObject)obj;
        }
        catch (FileNotFoundException exc) {
            this.error = "Erreur lors de l'ouverture du fichier " + path + " : " + exc.getMessage();
        }
        catch (IOException exc) {
            this.error = "Erreur avec le fichier de " + path + " : " + exc.getMessage();
        }
        catch (ParseException exc) {
            this.error = "Erreur de syntaxe dans " + path + ".";
        }
    }

    public boolean is_valid() {
        return this.error == null;
    }

    public void print_error() {
        IO.println(this.error);
    }

    public String get(String key) {
        if (this.json_obj == null)
            return null;

        return (String)this.json_obj.get(key);
    }
}
