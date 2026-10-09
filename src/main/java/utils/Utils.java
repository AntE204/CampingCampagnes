package utils;

import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import org.json.JSONArray;
import org.json.JSONObject;
public class Utils {
    public static byte[] genererPDF (LocalDateTime dateDebut, LocalDateTime dateFin, int idAnimateur){
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document= new Document(PageSize.A4);
        String sql="SELECT * FROM anime INNER JOIN animation ON animation.id=anime.idAnimation WHERE idAnimateur=? AND date BETWEEN ? AND ? ORDER BY date ASC";
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font fontCorps = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Paragraph titre = new Paragraph("Planning des Animations", fontTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            titre.setSpacingAfter(20);
            document.add(titre);
            Paragraph periode = new Paragraph("Période du " + dateDebut.format(dateFormatter) + " au " + dateFin.format(dateFormatter), fontCorps);
            periode.setSpacingAfter(20);
            document.add(periode);

            PreparedStatement req = pdo.prepareStatement(sql);
            req.setInt(1, idAnimateur);
            req.setTimestamp(2, Timestamp.valueOf(dateDebut));
            req.setTimestamp(3, Timestamp.valueOf(dateFin));
            try (ResultSet rs = req.executeQuery()) {

                // On vérifie s'il y a des lignes, sinon on affiche un message
                if (!rs.isBeforeFirst()) {
                    document.add(new Paragraph("Aucune animation prévue pour cette période.", fontCorps));
                } else {
                    // Création d'un tableau à 3 colonnes (Date, Heure Début, Activité)
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{3f, 2f, 3f, 4f}); // Largeurs relatives des colonnes

                    // En-têtes du tableau
                    String[] headers = {"Date", "Heure", "Durée","Activité"};
                    for (String header : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                        cell.setBackgroundColor(new Color(0, 128, 128)); // Couleur Teal
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(8);
                        table.addCell(cell);
                    }

                    // Parcours des résultats MySQL
                    while (rs.next()) {

                        String nom = rs.getString("libelle");
                        // Conversion de la date SQL en LocalDateTime Java
                        Timestamp timestamp = rs.getTimestamp("date");
                        LocalDateTime dateDebutFormat = timestamp.toLocalDateTime();
                        int duree = rs.getInt("duree");

                        // Ajout des cellules au tableau PDF
                        table.addCell(new PdfPCell(new Phrase(dateDebutFormat.format(dateFormatter), fontCorps)));
                        table.addCell(new PdfPCell(new Phrase(dateDebutFormat.format(timeFormatter), fontCorps)));
                        table.addCell(new PdfPCell(new Phrase(String.valueOf(duree), fontCorps)));
                        table.addCell(new PdfPCell(new Phrase(nom, fontCorps)));
                    }

                    document.add(table);
                }
            }
            document.close();
        } catch (SQLException e) {

        }
        return out.toByteArray();
    }

    public static void envoiMail(LocalDateTime dateDebut, LocalDateTime dateFin, int idAnimateur){
        ClientOptions options = ClientOptions.builder()
                .apiKey("")
                .apiSecretKey("")
                .build();


        MailjetClient client = new MailjetClient(options);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateDebut= LocalDateTime.parse("2026-10-08 00:00:00", formatter);
        LocalDateTime dateFin= LocalDateTime.parse("2026-10-15 00:00:00", formatter);
        byte[] fileContent = DB.genererPDF(dateDebut, dateFin, 7);
        String encodedPdf = Base64.getEncoder().encodeToString(fileContent);
        JSONObject attachment = new JSONObject()
                .put("ContentType", "application/pdf")
                .put("Filename", "planning.pdf")
                .put("Base64Content", encodedPdf);
        MailjetRequest request = new MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, new JSONArray()
                        .put(new JSONObject()
                                .put(Emailv31.Message.FROM, new JSONObject()
                                        .put("Email", "tiago.guedesl2pda@gmail.com")
                                        .put("Name", "Tiago Guedes"))
                                .put(Emailv31.Message.TO, new JSONArray()
                                        .put(new JSONObject()
                                                .put("Email", "titi.jeej@gmail.com")
                                                .put("Name", "Destinataire")))
                                .put(Emailv31.Message.SUBJECT, "Sujet de test Java")
                                .put(Emailv31.Message.TEXTPART, "Bonjour, test en texte brut.")
                                .put(Emailv31.Message.HTMLPART, "<h3>Bonjour</h3><p>Ceci est dgdgdgdg un test en HTML.</p><button onclick='alert(1)'>Bonjour</button><h1>TEST</h1>")
                                .put(Emailv31.Message.ATTACHMENTS, new JSONArray().put(attachment))));

        try {
            MailjetResponse response = client.post(request);
            System.out.println("Statut : " + response.getStatus());
            System.out.println("Données : " + response.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
