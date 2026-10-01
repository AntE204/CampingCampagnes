package utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mindrot.jbcrypt.BCrypt;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

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

        String HOST = (String) jo.get("HOST");
        String PORT = (String) jo.get("PORT");
        String DB_NAME = (String) jo.get("DB_NAME");
        String LOGIN = (String) jo.get("USER");
        String PASSWORD = (String) jo.get("PASSWORD");

        String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME;

        try {
            DB.pdo = DriverManager.getConnection(URL, LOGIN, PASSWORD);
            return true;
        } catch (SQLException exc) {
            IO.println("Erreur lors de l'initialisation de la base de données : " + exc.getMessage());
            return false;
        }
    }

    public static boolean passwordStrong(String pwd) {
        return pwd.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{12,}$");
    }

    public static boolean registerAccount(String mail, String tel, String nom, String prenom, String rue, String ville, String codePostal, String mdpClair) {
        String insert = "INSERT INTO animateur (mail, telephone, nom, prenom, rue, ville, codePostal, motDePasse) VALUES (?,?,?,?,?,?,?,?)";
        String check = "SELECT id FROM animateur WHERE mail LIKE ?";
        try {
            PreparedStatement checkdouble = pdo.prepareStatement(check);
            checkdouble.setString(1, mail);
            ResultSet res = checkdouble.executeQuery();
            if (res.next()) {
                System.out.println("Un utilisateur avec cette adresse mail existe déja dans la base!");
                return false;
            } else {
                if (passwordStrong(mdpClair)) {
                    String mspHash = BCrypt.hashpw(mdpClair, BCrypt.gensalt());
                    System.out.println("L'utilisateur a bien été crée");
                    PreparedStatement prep = pdo.prepareStatement(insert);
                    prep.setString(1, mail);
                    prep.setString(2, tel);
                    prep.setString(3, nom);
                    prep.setString(4, prenom);
                    prep.setString(5, rue);
                    prep.setString(6, ville);
                    prep.setString(7, codePostal);
                    prep.setString(8, mspHash);
                    prep.executeUpdate();
                    return true;
                } else {
                    System.out.println("Le mot de passe n'est pas assez fort! (12 caractères minimum, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial)");
                    return false;
                }


            }

        } catch (SQLException e) {
            System.out.println("ERREUR");
            System.out.println(e.getMessage());
            return false;
        }

    }

    public static boolean connexion(String mail, String mdpClair) {
        String checkpwd = "SELECT motDePasse FROM animateur WHERE mail=?";
//        String connexion = "SELECT id FROM animateur WHERE mail=? AND motDePasse=?";
        try {
            PreparedStatement check = pdo.prepareStatement(checkpwd);
            check.setString(1, mail);
            ResultSet pwd = check.executeQuery();
            if (pwd.next()) {
                String pwdHash = pwd.getString("motDePasse");
                boolean authenticate = BCrypt.checkpw(mdpClair, pwdHash);
                if (authenticate) {
                    System.out.println("Vous êtes connecté");
                    return true;
                } else {
                    System.out.println("mot de passe et/ou email incorrect !");
                    return false;
                }
            } else {
                System.out.println("mot de passe et/ou email incorrect !");
                return false;
            }


        } catch (SQLException e) {
            System.out.println("ERREUR");
            System.out.println(e.getMessage());
            return false;
        }
    }
    public static boolean checkAnimLimit(int idAnimateur, String date) {
        String checkLimit = "SELECT Count(*) as number FROM anime WHERE idAnimateur=? AND date=?";
        try {
            PreparedStatement stmt = pdo.prepareStatement(checkLimit);
            stmt.setInt(1, idAnimateur);
            stmt.setString(2, date);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                int nbAnim = res.getInt("number");
                if (nbAnim >= 7) {
//                    System.out.println("L'animateur a atteint ou dépassé la limite d'activités par jour ! ");
                    return false;
                } else {
                    return true;
                }
            } else {
                System.out.println("L'animateur n'a pas été trouvé!");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("ERREUR");
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean createAnimation(String libelle, int dureeEnMinutes){
        String newAnime= "INSERT INTO animation (libelle, duree) VALUES (?, ?)";
        try {
            PreparedStatement Animation = pdo.prepareStatement(newAnime);
            Animation.setString(1, libelle);
            Animation.setInt(2, dureeEnMinutes);
            Animation.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("ERREUR");
            System.out.println(e.getMessage());
            return false;
        }
    }

}
