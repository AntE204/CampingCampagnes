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
}
