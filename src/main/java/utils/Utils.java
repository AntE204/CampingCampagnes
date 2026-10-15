package utils;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import models.Config;
import org.json.JSONArray;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class Utils {
    /**
     * Fonction qui va générer un planning en PDF à partir des paramètres, contenant les activités sur une période donnée
     * @param dateDebut LocalDateTime qui représente la date de début de la période pour laquelle on génére le planning
     * @param dateFin LocalDateTime qui représente la date de fin de la période pour laquelle on génére le planning
     * @param idAnimateur int qui va nous permettre de récuperer les activités de l'animateur concerné avec son nom et prénom pour mettre dans le PDF
     * @return Le contenu binaire du PDF
     */
    public static byte[] genererPDF(LocalDateTime dateDebut, LocalDateTime dateFin, int idAnimateur) {
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

        String sqlNom = "SELECT nom, prenom FROM utilisateur WHERE id = ?";

        try {
            // Récupération de l'animateur
            String nomA;
            String prenomA;
            Connection pdo = DB.getPdo();

            try (PreparedStatement recupNom = pdo.prepareStatement(sqlNom)) {
                recupNom.setInt(1, idAnimateur);

                try (ResultSet resultat = recupNom.executeQuery()) {
                    if (!resultat.next()) {
                        throw new SQLException("Aucun animateur trouvé pour l'id " + idAnimateur);
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
                req.setTimestamp(2, java.sql.Timestamp.valueOf(dateDebut));
                req.setTimestamp(3, java.sql.Timestamp.valueOf(dateFin));

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
            throw new RuntimeException("Erreur lors de la génération du planning PDF", e);
        }
    }

    /**
     * Fonction pour envoyer un mail, contenant un pdf généré à partir des paramètres
     * @param
     * @param dateDebut LocalDateTime qui représente la date de début de la période sur laquelle on veut générer le planning
     * @param dateFin   LocalDateTime qui reprèsente la date de fin de la période sur laquelle on veut générer le planning
     * @param idAnimateur int qui indique l'id de l'animateur pour lequel on veut générer le planning
     * @return Le texte de l'erreur ou du succès de l'envoi
     */
    public static String envoiMail(Config config, byte[] pdf, LocalDateTime dateDebut, LocalDateTime dateFin, int idAnimateur) {
        ClientOptions options = ClientOptions.builder()
                .apiKey(config.get("MAILJET_API_KEY"))
                .apiSecretKey(config.get("MAILJET_API_SECRET"))
                .build();

        String sql = "SELECT nom, prenom, mail FROM utilisateur WHERE id = ?";

        try {
            Connection pdo = DB.getPdo();
            PreparedStatement recupNom = pdo.prepareStatement(sql);
            recupNom.setInt(1, idAnimateur);

            ResultSet resultat = recupNom.executeQuery();
            if (!resultat.next()) {
                return "Aucun animateur trouvé par l'ID " + idAnimateur + ".";
            }

            String nomA = resultat.getString("nom");
            String prenomA = resultat.getString("prenom");
            String mailA = resultat.getString("mail");

            MailjetClient client = new MailjetClient(options);

            // Encodage du PDF
            String encodedPdf = Base64.getEncoder().encodeToString(pdf);

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
                                                    .put("Email", config.get("MAILJET_SENDER_EMAIL"))
                                                    .put("Name", config.get("MAILJET_SENDER_NAME")))
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
                return "Envoi du planning à " + mailA + " (" + response.getStatus() + ")";
                // System.out.println("Statut : " + response.getStatus());
                // System.out.println("Données : " + response.getData());
            }
            catch (Exception ex) {
                return "Erreur lors de l'envoi du mail : " + ex.getMessage();
            }
        } catch (SQLException ex) {
            return "Erreur SQL lors de la récupération de l'animateur" + ex.getMessage();
        }
    }
}