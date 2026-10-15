package utils;

import dbres.*;
import models.Animation;
import models.Animator;
import models.Config;
import org.mindrot.jbcrypt.BCrypt;
import org.openpdf.text.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DB {
    private static Connection pdo = null;
    public static Integer loggedUser = null;
    public static boolean isDirecteur = false;
    public static String firstName = null;
    public static String myEmail = null;

    /**
     * Fonction pour initialiser la base de données
     * @param config Le fichier de configuration de l'appli
     * @return Si la base a bien été initialisée
     */
    public static boolean init(Config config) {
        String HOST = config.get("HOST");
        String PORT = config.get("PORT");
        String DB_NAME = config.get("DB_NAME");
        String LOGIN = config.get("USER");
        String PASSWORD = config.get("PASSWORD");

        String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME;


        try {
            DB.pdo = DriverManager.getConnection(URL, LOGIN, PASSWORD);
            return true;
        } catch (SQLException exc) {
            IO.println("Erreur lors de l'initialisation de la base de données : " + exc.getMessage());
            return false;
        }
    }

    /**
     * Ferme la connexion à la base de données
     */
    public static void close() {
        try {
            if (DB.pdo != null)
                DB.pdo.close();
        }
        catch (Exception e) {

        }
    }

    /**
     * Fonction qui va vérifier la force du mot de passe (12 caractères, minimum 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial)
     * @param pwd Le mot de passe de l'utilisateur
     * @return Si le mot de passe est assez fort
     */
    public static boolean passwordStrong(String pwd) {
        return pwd.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.!?_])(?=\\S+$).{12,}$");
    }

    /**
     * Ajoute un utilisateur dans la base de données
     * @param mail L'email de l'utilisateur
     * @param tel Le téléphone de l'utilisateur
     * @param nom Le nom de l'utilisateur
     * @param prenom Le prénom de l'utilisateur
     * @param rue La rue de l'utilisateur
     * @param ville La ville de l'utilisateur
     * @param codePostal Le code postal de l'utilisateur
     * @param mdpClair Le mot de passe en clair de l'utilisateur
     * @return Si l'utilisateur a bien été enregistré
     */
    public static DBRes registerAccount(String mail, String tel, String nom, String prenom, String rue, String ville, String codePostal, String mdpClair, boolean isDirector) {
        String insert = "INSERT INTO utilisateur (mail, telephone, nom, prenom, rue, ville, codePostal, motDePasse, permission) VALUES (?,?,?,?,?,?,?,?,?)";
        String check = "SELECT id FROM utilisateur WHERE mail LIKE ?";
        try {
            PreparedStatement checkdouble = pdo.prepareStatement(check);
            checkdouble.setString(1, mail);
            ResultSet res = checkdouble.executeQuery();
            if (res.next()) {
                return new DBRes(false, "Cet email est déjà utilisé !");
            } else {
                if(verifMail(mail)){
                    if (passwordStrong(mdpClair)) {
                        String mdpHash = BCrypt.hashpw(mdpClair, BCrypt.gensalt());
                        PreparedStatement prep = pdo.prepareStatement(insert);
                        prep.setString(1, mail);
                        prep.setString(2, tel);
                        prep.setString(3, nom);
                        prep.setString(4, prenom);
                        prep.setString(5, rue);
                        prep.setString(6, ville);
                        prep.setString(7, codePostal);
                        prep.setString(8, mdpHash);
                        prep.setString(9, isDirector ? "directeur" : "animateur");
                        prep.executeUpdate();
                        return new DBRes(true);
                    }else {
                        return new DBRes(false, "Le mot de passe n'est pas assez fort! (12 caractères minimum, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial)");
                    }
                }else{
                    return new DBRes(false, "L'email n'est pas valide!");
                }
            }
        } catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la création du compte : " + e.getMessage());
        }

    }

    /**
     * Modifie les informations d'un compte utilisateur
     * @param oldEmail L'ancien email de l'utilisateur
     * @param tel Le téléphone de l'utilisateur
     * @param nom Le nom de l'utilisateur
     * @param prenom Le prénom de l'utilisateur
     * @param rue La rue de l'utilisateur
     * @param ville La ville de l'utilisateur
     * @param codePostal Le code postal de l'utilisateur
     * @param mdpClair Le mot de passe en clair de l'utilisateur
     * @return Si l'utilisateur a bien été modifié
     */
    public static DBRes editAccount(String oldEmail, String mail, String tel, String nom, String prenom, String rue, String ville, String codePostal, String mdpClair, boolean isDirector) {
        String sql = "UPDATE utilisateur SET mail = ?, telephone = ?, nom = ?, prenom = ?, rue = ?, ville = ?, codePostal = ?, motDePasse = ?, permission = ? WHERE mail LIKE ?";

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setString(1, mail);
            req.setString(2, tel);
            req.setString(3, nom);
            req.setString(4, prenom);
            req.setString(5, rue);
            req.setString(6, ville);
            req.setString(7, codePostal);
            req.setString(8, BCrypt.hashpw(mdpClair, BCrypt.gensalt()));
            req.setString(9, isDirector ? "directeur" : "animateur");
            req.setString(10, oldEmail);

            boolean res = req.executeUpdate() == 1;

            if (res) {
                boolean is_self = oldEmail.equals(DB.myEmail);
                if (is_self) {
                    DB.myEmail = mail;
                    DB.firstName = prenom;
                    DB.isDirecteur = isDirector;
                }

                return new DBRes(true);
            }
            else
                return new DBRes(false, "L'email ne correspond à aucun utilisateur.");
        }
        catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la modification d'un compte : " + e.getMessage());
        }
    }

    /**
     * Supprime un compte
     * @param email L'email du compte à supprimer
     * @return Si l'utilsiateur a correctement été supprimé
     */
    public static DBDeleteAccount deleteAccount(String email) {
        String sql_select = "SELECT id FROM utilisateur WHERE mail LIKE ?";
        String sql_delete = "DELETE FROM utilisateur WHERE mail LIKE ? AND utilisateur.id NOT IN (SELECT idAnimateur FROM anime)";

        try {
            // Vérifie si l'utilisateur supprimé est l'utilisateur connecté
            PreparedStatement req_select = pdo.prepareStatement(sql_select);
            req_select.setString(1, email);
            ResultSet res = req_select.executeQuery();
            boolean is_self = res.next() && res.getInt("id") == DB.loggedUser;

            // Supprime l'utilisateur
            PreparedStatement req_delete = pdo.prepareStatement(sql_delete);
            req_delete.setString(1, email);
            boolean deleted = req_delete.executeUpdate() == 1;

            return new DBDeleteAccount(deleted, deleted ? "L'utilisateur a bien été supprimé." : "L'utilisateur n'a pas pu être supprimé car il a déjà animé une activité.", is_self);
        }
        catch (SQLException e) {
            return new DBDeleteAccount(false, "Erreur lors de la suppression de l'utilisateur : " + e.getMessage(), false);
        }
    }

    /**
     * Vérifie si au moins un utilisateur directeur existe dans la base de données
     * @return Si un utilisateur directeur existe dans la base de données
     */
    public static boolean noDirectorAccount() {
        String sql = "SELECT id FROM utilisateur WHERE permission = ?";
        try {
            PreparedStatement check = pdo.prepareStatement(sql);
            check.setString(1, "directeur");
            ResultSet res = check.executeQuery();
            return !res.next();
        }
        catch (SQLException e) {
            IO.println("Erreur lors de la vérification de la présence d'un compte directeur : " + e.getMessage());
            return false;
        }
    }

    /**
     * Fonction pour se connecter à l'application
     * @param mail Le mail de l'utilisateur
     * @param mdpClair Le mot de passe en clair de l'utilisateur
     * @return Si l'utilisateur a bien pu se connecter
     */
    public static DBRes connexion(String mail, String mdpClair) {
        String checkpwd = "SELECT id, mail, prenom, motDePasse, permission FROM utilisateur WHERE mail=?";
        try {
            PreparedStatement check = pdo.prepareStatement(checkpwd);
            check.setString(1, mail);
            ResultSet res = check.executeQuery();
            if (res.next()) {
                String pwdHash = res.getString("motDePasse");
                boolean authenticate = BCrypt.checkpw(mdpClair, pwdHash);
                if (authenticate) {
                    DB.loggedUser = res.getInt("id");
                    DB.isDirecteur = res.getString("permission").equals("directeur");
                    DB.firstName = res.getString("prenom");
                    DB.myEmail = res.getString("mail");
                    return new DBRes(true);
                } else {
                    return new DBRes(false, "Identifiants incorrects !");
                }
            } else {
                return new DBRes(false, "Identifiants incorrects !");
            }


        } catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la connexion : " + e.getMessage());
        }
    }

    /**
     * Fonction pour créer une animation
     * @param libelle Le nom de l'activite que l'on veut créer
     * @param dureeEnMinutes La durée de l'activité en minutes
     * @return Si l'animation a bien été crée
     */
    public static DBRes createAnimation(String libelle, int dureeEnMinutes){
        String newAnime= "INSERT INTO animation (libelle, duree) VALUES (?, ?)";
        try {
            PreparedStatement Animation = pdo.prepareStatement(newAnime);
            Animation.setString(1, libelle);
            Animation.setInt(2, dureeEnMinutes);
            Animation.executeUpdate();
            return new DBRes(true);
        } catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la création d'une animation : " + e.getMessage());
        }
    }

    /**
     * Vérifie si une animation existe, insensible à la casse
     * @param name Le nom de l'animation à vérifier
     * @return Si l'animation existe
     */
    public static DBRes animationExists(String name) {
        String sql = "SELECT libelle FROM animation WHERE libelle LIKE ?";

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setString(1, name);
            ResultSet res = req.executeQuery();

            if (res.next())
                return new DBRes(false, "Il existe déjà une animation avec cet intitulé.");
            else
                return new DBRes(true);
        }
        catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la vérification de l'existence d'une animation : " + e.getMessage());
        }
    }

    /**
     * Récupère toutes les animations
     * @return Une liste de toues les animations
     */
    public static DBAllAnimations getAllAnimations() {
        String sql = "SELECT * FROM animation";
        List<Animation> animations = new ArrayList<>();

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            ResultSet res = req.executeQuery();

            while (res.next()) {
                Animation anim = new Animation(
                    res.getInt("id"),
                    res.getString("libelle"),
                    res.getInt("duree")
                );
                animations.add(anim);
            }

            return new DBAllAnimations(true, null, animations);
        }
        catch (SQLException e) {
            return new DBAllAnimations(false, "Erreur lors de la récupération de toutes les animations : " + e.getMessage(), null);
        }
    }

    /**
     * Récupère une animation par son nom
     * @param name Le nom de l'animation
     * @return L'animation récupérée
     */
    public static DBSingleAnimation getAnimationByName(String name) {
        String sql = "SELECT * FROM animation WHERE libelle LIKE ?";

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setString(1, name);
            ResultSet res = req.executeQuery();

            if (res.next()) {
                Animation anim = new Animation(
                    res.getInt("id"),
                    res.getString("libelle"),
                    res.getInt("duree")
                );
                return new DBSingleAnimation(true, null, anim);
            }
            else
                return new DBSingleAnimation(false, "Aucune animation ne possède l'intitulé \"" + name + "\".", null);
        }
        catch (SQLException e) {
            return new DBSingleAnimation(false, "Erreur lors de la récupération d'une animation : " + e.getMessage(), null);
        }
    }

    /**
     * Ajoute un animateur à une animation
     * @param animationName Le nom de l'animation
     * @param animatorEmail L'email de l'animateur
     * @param date_time La date et l'heure au format yyyy-MM-dd hh:mm:ss
     * @return Si l'animateur a pu être ajouté, en prennant en compte les limites (pas 2 en même temps, pas + de 7 par jour)
     */
    public static DBRes addAnimatorToAnimation(String animationName, String animatorEmail, String date_time) {
        String getDurationSql = "SELECT id, duree FROM animation WHERE libelle LIKE ?";
        String checkOverlapSql = "SELECT 1 FROM anime a " +
                "INNER JOIN animation an ON a.idAnimation = an.id " +
                "WHERE a.idAnimateur = (SELECT id FROM utilisateur WHERE mail LIKE ?) " +
                "AND ? < DATE_ADD(a.date, INTERVAL an.duree MINUTE) " +
                "AND a.date < DATE_ADD(?, INTERVAL ? MINUTE)";
        String checkLimitSql = "SELECT COUNT(*) AS counter FROM anime " +
                "WHERE idAnimateur = (SELECT id FROM utilisateur WHERE mail LIKE ?) " +
                "AND date >= DATE(?) AND date < DATE_ADD(DATE(?), INTERVAL 1 DAY)";
        String insertSql = "INSERT INTO anime (idAnimateur, idAnimation, date) VALUES ((SELECT id FROM utilisateur WHERE mail LIKE ?), ?, ?)";

        try {
            String startDateTime = date_time + ":00";

            // Récup infos de l'animation
            PreparedStatement getAnim = pdo.prepareStatement(getDurationSql);
            getAnim.setString(1, animationName);
            ResultSet resAnim = getAnim.executeQuery();
            if (!resAnim.next()) {
                return new DBRes(false, "L'animation '" + animationName + "' n'existe pas.");
            }
            int newAnimId = resAnim.getInt("id");
            int newAnimDuration = resAnim.getInt("duree");

            // Chevauchement d'horaires
            PreparedStatement checkOverlap = pdo.prepareStatement(checkOverlapSql);
            checkOverlap.setString(1, animatorEmail);
            checkOverlap.setString(2, startDateTime);
            checkOverlap.setString(3, startDateTime);
            checkOverlap.setInt(4, newAnimDuration);
            ResultSet resOverlap = checkOverlap.executeQuery();
            if (resOverlap.next()) {
                return new DBRes(false, animatorEmail + " anime déjà une autre activité sur ce créneau horaire.");
            }

            // Limite de 7 anims par jour
            PreparedStatement checkLimit = pdo.prepareStatement(checkLimitSql);
            checkLimit.setString(1, animatorEmail);
            checkLimit.setString(2, startDateTime);
            checkLimit.setString(3, startDateTime);
            ResultSet resLimit = checkLimit.executeQuery();
            if (resLimit.next() && resLimit.getInt("counter") >= 7) {
                return new DBRes(false, animatorEmail + " anime déjà 7 activités ou plus ce jour-là.");
            }

            // Attribution de l'animation
            PreparedStatement req = pdo.prepareStatement(insertSql);
            req.setString(1, animatorEmail);
            req.setInt(2, newAnimId);
            req.setString(3, startDateTime);

            if (req.executeUpdate() == 1) {
                return new DBRes(true);
            } else {
                return new DBRes(false, animatorEmail + " n'a pas pu être ajouté.");
            }

        } catch (SQLException e) {
            return new DBRes(false, "Erreur lors de l'attribution de l'animateur : " + e.getMessage());
        }
    }

    /**
     * Fonction permettant de retirer un animateur d'une animation
     * @param animationName String qui représente le libelle d'une animation
     * @param animatorEmail String qui représente le mail de l'animateur
     * @return true si la suppression s'est effectuée, false avec un message d'erreur sinon
     */
    public static DBRes removeAnimatorFromAnimation(String animationName, String animatorEmail) {
        String sql = "DELETE FROM anime WHERE idAnimateur = (SELECT id FROM utilisateur WHERE mail LIKE ?) AND idAnimation = (SELECT id FROM animation WHERE libelle LIKE ?)";

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setString(1, animatorEmail);
            req.setString(2, animationName);
            boolean deleted = req.executeUpdate() > 0;

            if (deleted)
                return new DBRes(true);
            else
                return new DBRes(false, "L'animateur " + animatorEmail + " n'a pas pu être supprimé de l'animation " + animationName);
        }
        catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la suppression d'un animateur à une animation " + e.getMessage());
        }
    }

    /**
     * Récupère tous les animateurs
     * @return Une liste de tous les animateurs, directeurs compris
     */
    public static DBAllAnimators getAllAnimators() {
        String sql = "SELECT * FROM utilisateur";
        List<Animator> animators = new ArrayList<>();

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            ResultSet res = req.executeQuery();

            while (res.next()) {
                Animator animator = new Animator(
                    res.getInt("id"),
                    res.getString("mail"),
                    res.getString("prenom"),
                    res.getString("nom"),
                    res.getString("rue"),
                    res.getString("ville"),
                    res.getString("codePostal"),
                    res.getString("telephone"),
                    res.getString("permission").equals("directeur")
                );

                animators.add(animator);
            }

            return new DBAllAnimators(true, null, animators);
        }
        catch (SQLException e) {
            return new DBAllAnimators(false, "Erreur lors de la récupération de tous les animateurs : " + e.getMessage(), null);
        }
    }

    /**
     * Récupère tous les animateurs qui animent dans une période
     * @param dateDebut Le début de la période
     * @param dateFin La fin de la période
     * @return La liste des animateurs
     */
    public static DBAllAnimators getAnimatorsWhoAnimate(LocalDateTime dateDebut, LocalDateTime dateFin) {
        String sql = "SELECT DISTINCT u.* FROM utilisateur u INNER JOIN anime a ON u.id = a.idAnimateur WHERE date BETWEEN ? AND ?";
        List<Animator> animators = new ArrayList<>();

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setTimestamp(1, Timestamp.valueOf(dateDebut));
            req.setTimestamp(2, Timestamp.valueOf(dateFin));
            ResultSet res = req.executeQuery();

            while (res.next()) {
                Animator animator = new Animator(
                    res.getInt("id"),
                    res.getString("mail"),
                    res.getString("prenom"),
                    res.getString("nom"),
                    res.getString("rue"),
                    res.getString("ville"),
                    res.getString("codePostal"),
                    res.getString("telephone"),
                    res.getString("permission").equals("directeur")
                );

                animators.add(animator);
            }

            return new DBAllAnimators(true, null, animators);
        }
        catch (SQLException e) {
            return new DBAllAnimators(false, "Erreur lors de la récupération des animateurs qui anniment : " + e.getMessage(), null);
        }
    }

    /**
     * Vérifie si une adresse mail est valide (***@***.***)
     * @param mail Le mail de l'utilisateur
     * @return Si l'email est valide (une @, un point après l'@)
     */
    public static boolean verifMail(String mail){
        return mail.matches("^((?!\\.)[\\w\\-_.]*[^.])(@\\w+)(\\.\\w+(\\.\\w+)?[^.\\W])$");
    }

    /**
     * Vérifie si un email est déjà utilisé par un utilisateur inscrit
     * @param mail L'email à tester
     * @return Si l'email est utilisé
     */
    public static DBRes emailUsed(String mail) {
        String sql = "SELECT id FROM utilisateur WHERE mail LIKE ?";

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setString(1, mail);
            ResultSet res = req.executeQuery();

            if (res.next())
                return new DBRes(false, "Cet email est déjà utilisé !");
            else
                return new DBRes(true);
        }
        catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la vérification de la présence de l'email : " + e.getMessage());
        }
    }

    public static DBSingleAnimator getMyInfo(){
        String recup = "SELECT * FROM utilisateur WHERE id=?";
        try {
            PreparedStatement check = pdo.prepareStatement(recup);
            check.setInt(1, DB.loggedUser);
            ResultSet res = check.executeQuery();

            if(res.next()){
                Animator animateur = new Animator(
                    DB.loggedUser,
                    res.getString("mail"),
                    res.getString("prenom"),
                    res.getString("nom"),
                    res.getString("rue"),
                    res.getString("ville"),
                    res.getString("codePostal"),
                    res.getString("tel"),
                    DB.isDirecteur
                );
                return new DBSingleAnimator(true, null, animateur);
            }else{
                return new DBSingleAnimator(false, "Les informations de votre compte n'ont pas pu être récupérées.", null);
            }
        } catch (SQLException e) {
            return new DBSingleAnimator(false, "Erreur lors de la récupération de vos informations "+e.getMessage(), null);
        }
    }

    public static Connection getPdo() {
        return DB.pdo;
    }

}
