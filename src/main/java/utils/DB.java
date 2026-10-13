package utils;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import dbres.*;
import models.Animation;
import models.Animator;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mindrot.jbcrypt.BCrypt;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import javax.xml.transform.Result;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.spec.DSAGenParameterSpec;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DB {
    private static Connection pdo = null;
    public static Integer loggedUser = null;
    public static boolean isDirecteur = false;
    public static String firstName = null;
    public static String myEmail = null;

    /**
     * Fonction pour initialiser la base de données
     * @return Si la base a bien été initialisée
     */
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
        return pwd.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.])(?=\\S+$).{12,}$");
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
                        System.out.println("L'utilisateur a bien été crée");
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

            if (res)
                return new DBRes(true);
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
        String sql_delete = "DELETE FROM utilisateur WHERE mail LIKE ?";

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

            return new DBDeleteAccount(deleted, !deleted ? null : "L'utilisateur n'a pas pu être supprimé.", is_self);
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
     * Fonction pour checker qu'un animateur n'a pas dépassé la limite d'animations par jour
     * @param idAnimateur L'id de l'animateur duquel on va vérifier le nombre d'animations
     * @param date La date de l'animation
     * @return Si l'animateur n'a pas dépassé la limite et peut donc animer l'activité
     */
    public static DBRes checkAnimLimit(int idAnimateur, String date) {
        String checkLimit = "SELECT Count(*) as number FROM anime WHERE idAnimateur=? AND date=?";
        try {
            PreparedStatement stmt = pdo.prepareStatement(checkLimit);
            stmt.setInt(1, idAnimateur);
            stmt.setString(2, date);
            ResultSet res = stmt.executeQuery();
            if (res.next()) {
                int nbAnim = res.getInt("number");
                if (nbAnim >= 7) {
                    return new DBRes(false, "L'animateur a atteint ou dépassé la limite d'activités par jour ! ");
                } else {
                    return new DBRes(true);
                }
            } else {
                return new DBRes(false, "L'animateur n'a pas été trouvé !");
            }

        } catch (SQLException e) {
            return new DBRes(false, "Erreur lors de la vérification de la limite d'animations : " + e.getMessage());
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
     * @param date_time La date et l'heure au format dd/MM/yyyy hh:mm:ss
     * @return Si l'animateur a pu être ajouté, en prennant en compte les limites (pas 2 en même temps, pas + de 7 par jour)
     */
    public static DBRes addAnimatorToAnimation(String animationName, String animatorEmail, String date_time) {
        String sql = "INSERT INTO anime (idAnimateur, idAnimation, date) VALUES ((SELECT id FROM utilisateur WHERE mail LIKE ?), (SELECT id FROM animation WHERE libelle LIKE ?), ?)";

        try {
            PreparedStatement req = pdo.prepareStatement(sql);
            req.setString(1, animatorEmail);
            req.setString(2, animationName);;
            req.setString(3, date_time + ":00");
            boolean added = req.executeUpdate() == 1;
            // TODO vérifier si un animateur n'anime pas 2 fois en même temps
            // TODO vérifier si limite d'animation par jour atteinte

            if (added)
                return new DBRes(true);
            else
                return new DBRes(false, animatorEmail + " n'a pas pu être ajouté.");
        }
        catch (SQLException e) {
            return new DBRes(false, "Erreur lors de l'attribution d'un animateur à une naimation : " + e.getMessage());
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


    /**
     * Fonction qui va générer un planning en PDF à partir des paramètres, contenant les activités sur une période donnée
     * @param dateDebut LocalDateTime qui représente la date de début de la période pour laquelle on génére le planning
     * @param dateFin LocalDateTime qui représente la date de fin de la période pour laquelle on génére le planning
     * @param idAnimateur int qui va nous permettre de récuperer les activités de l'animateur concerné avec son nom et prénom pour mettre dans le PDF
     * @return
     */
    public static byte[] genererPDF(LocalDateTime dateDebut,
                                    LocalDateTime dateFin,
                                    int idAnimateur) {

        final DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern("HH:mm:ss");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        String sql =
                "SELECT animation.libelle, anime.date, animation.duree " +
                        "FROM anime " +
                        "INNER JOIN animation ON animation.id = anime.idAnimation " +
                        "WHERE anime.idAnimateur = ? " +
                        "AND anime.date BETWEEN ? AND ? " +
                        "ORDER BY anime.date ASC";

        String sqlNom =
                "SELECT nom, prenom FROM utilisateur WHERE id = ?";

        try {
            // Récupération de l'animateur
            String nomA;
            String prenomA;

            try (PreparedStatement recupNom = pdo.prepareStatement(sqlNom)) {
                recupNom.setInt(1, idAnimateur);

                try (ResultSet resultat = recupNom.executeQuery()) {
                    if (!resultat.next()) {
                        throw new SQLException(
                                "Aucun animateur trouvé pour l'id " + idAnimateur);
                    }

                    nomA = resultat.getString("nom");
                    prenomA = resultat.getString("prenom");
                }
            }

            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitre = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font fontHeader = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font fontCorps = FontFactory.getFont(
                    FontFactory.HELVETICA, 10, Color.BLACK);

            // Titre du PDF
            Paragraph titre = new Paragraph(
                    "Planning des Animations - " + prenomA + " " + nomA,
                    fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(20);
            document.add(titre);

            // Période
            Paragraph periode = new Paragraph(
                    "Période du " + dateDebut.format(dateFormatter)
                            + " au " + dateFin.format(dateFormatter),
                    fontCorps);
            periode.setSpacingAfter(20);
            document.add(periode);

            // Récupération des animations
            try (PreparedStatement req = pdo.prepareStatement(sql)) {
                req.setInt(1, idAnimateur);
                req.setTimestamp(2, Timestamp.valueOf(dateDebut));
                req.setTimestamp(3, Timestamp.valueOf(dateFin));

                try (ResultSet rs = req.executeQuery()) {

                    if (!rs.isBeforeFirst()) {
                        document.add(new Paragraph(
                                "Aucune animation prévue pour cette période.",
                                fontCorps));
                    } else {
                        PdfPTable table = new PdfPTable(4);
                        table.setWidthPercentage(100);
                        table.setWidths(new float[]{3f, 2f, 3f, 4f});

                        String[] headers = {
                                "Date", "Heure", "Durée", "Activité"
                        };

                        for (String header : headers) {
                            PdfPCell cell = new PdfPCell(
                                    new Phrase(header, fontHeader));
                            cell.setBackgroundColor(new Color(0, 128, 128));
                            cell.setHorizontalAlignment(
                                    Element.ALIGN_CENTER);
                            cell.setPadding(8);
                            table.addCell(cell);
                        }

                        while (rs.next()) {
                            String nom = rs.getString("libelle");
                            Timestamp timestamp = rs.getTimestamp("date");
                            LocalDateTime dateAnimation =
                                    timestamp.toLocalDateTime();
                            int duree = rs.getInt("duree");

                            table.addCell(new PdfPCell(new Phrase(
                                    dateAnimation.format(dateFormatter),
                                    fontCorps)));

                            table.addCell(new PdfPCell(new Phrase(
                                    dateAnimation.format(timeFormatter),
                                    fontCorps)));

                            table.addCell(new PdfPCell(new Phrase(
                                    duree + " min", fontCorps)));

                            table.addCell(new PdfPCell(new Phrase(
                                    nom, fontCorps)));
                        }

                        document.add(table);
                    }
                }
            }

            document.close();
            return out.toByteArray();

        } catch (SQLException | DocumentException e) {
            if (document.isOpen()) {
                document.close();
            }
            throw new RuntimeException(
                    "Erreur lors de la génération du planning PDF", e);
        }
    }

    /**
     * Fonction pour envoyer un mail, contenant un pdf généré à partir des paramètres
     * @param dateDebut LocalDateTime qui représente la date de début de la période sur laquelle on veut générer le planning
     * @param dateFin   LocalDateTime qui reprèsente la date de fin de la période sur laquelle on veut générer le planning
     * @param idAnimateur int qui indique l'id de l'animateur pour lequel on veut générer le planning
     */
    public static void envoiMail(LocalDateTime dateDebut,
                                 LocalDateTime dateFin,
                                 int idAnimateur) {

        ClientOptions options = ClientOptions.builder()
                .apiKey("")
                .apiSecretKey("")
                .build();

        String sql = "SELECT nom, prenom, mail FROM utilisateur WHERE id = ?";

        try (PreparedStatement recupNom = pdo.prepareStatement(sql)) {

            recupNom.setInt(1, idAnimateur);

            try (ResultSet resultat = recupNom.executeQuery()) {

                if (!resultat.next()) {
                    System.out.println("Aucun animateur trouvé.");
                    return;
                }

                String nomA = resultat.getString("nom");
                String prenomA = resultat.getString("prenom");
                String mailA = resultat.getString("mail");

                MailjetClient client = new MailjetClient(options);

                // Génération du PDF avec les paramètres reçus
                byte[] fileContent = genererPDF(
                        dateDebut, dateFin, idAnimateur);

                String encodedPdf = Base64.getEncoder()
                        .encodeToString(fileContent);

                // Nom du fichier personnalisé
                String nomFichier = ("planning_" + prenomA + "_" + nomA)
                        .replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";

                org.json.JSONObject attachment = new org.json.JSONObject()
                        .put("ContentType", "application/pdf")
                        .put("Filename", nomFichier)
                        .put("Base64Content", encodedPdf);

                MailjetRequest request = new MailjetRequest(Emailv31.resource)
                        .property(Emailv31.MESSAGES, new JSONArray()
                                .put(new org.json.JSONObject()
                                        .put(Emailv31.Message.FROM,
                                                new org.json.JSONObject()
                                                        .put("Email", "example@example.com")
                                                        .put("Name", "Example Example"))
                                        .put(Emailv31.Message.TO, new JSONArray()
                                                .put(new org.json.JSONObject()
                                                        .put("Email", mailA)
                                                        .put("Name", prenomA + " " + nomA)))
                                        .put(Emailv31.Message.SUBJECT,
                                                "Planning de " + prenomA + " " + nomA)
                                        .put(Emailv31.Message.TEXTPART,
                                                "Bonjour " + prenomA
                                                        + ", voici ton planning en pièce jointe.")
                                        .put(Emailv31.Message.HTMLPART,
                                                "<h3>Bonjour " + prenomA + "</h3>"
                                                        + "<p>Voici ton planning des animations en pièce jointe.</p>")
                                        .put(Emailv31.Message.ATTACHMENTS,
                                                new JSONArray().put(attachment))));

                try {
                    MailjetResponse response = client.post(request);

                    System.out.println("Statut : " + response.getStatus());
                    System.out.println("Données : " + response.getData());

                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi du mail :");
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de la récupération de l'animateur", e);
        }
    }
}
