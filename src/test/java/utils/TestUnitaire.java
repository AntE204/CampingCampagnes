package utils;

import dbres.DBRes;
import models.Config;
import org.junit.Assert;

import javax.print.attribute.standard.JobKOctetsSupported;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class TestUnitaire {
    private static String compte_email = null;

    private static String get_random_email() {
        return String.valueOf(Math.random()).replace('.', '0') + "@test.com";
    }

    @org.junit.jupiter.api.Test
    void passwordStrong() {
        String testPassword= "123Azerty@123";
        String test2= "123";
        Assert.assertTrue(DB.passwordStrong(testPassword));
        Assert.assertFalse(DB.passwordStrong(test2));
    }

    @org.junit.jupiter.api.Test
    void verifMail() {
        String mailTest="test@test.com";
        Assert.assertTrue(DB.verifMail(mailTest));
        String mailFail="testtest";
        Assert.assertFalse(DB.verifMail(mailFail));
    }

    @org.junit.jupiter.api.Test
    void db_compte() {
        // Ouverture config
        Config config = new Config("./config.json");
        Assert.assertTrue("Config invalide", config.is_valid());

        // Ouverture DB
        Assert.assertTrue("Init de DB échouée", DB.init(config));

        String email = TestUnitaire.get_random_email();
        TestUnitaire.compte_email = email;
        String mdp = "mdpTest!123.OK";

        // Vérif de l'email
        Assert.assertTrue("Email invalide", DB.verifMail(email));

        // vérif mot de passe
        Assert.assertTrue("Mot de passe trop faible", DB.passwordStrong(mdp));

        // Ajout du compte
        DBRes res_ajout = DB.registerAccount(email, "test tel", "test nom", "test prénom", "test rue", "test ville", "12345", mdp, false);
        Assert.assertTrue(res_ajout.error, res_ajout.ok);

        // Reqquêtes SQL pures
        String sql_select = "SELECT id FROM utilisateur WHERE mail LIKE ?";
        String sql_delete = "DELETE FROM utilisateur WHERE id = ?";

        try {
            Connection pdo = DB.getPdo();

            // Select
            PreparedStatement req_select = pdo.prepareStatement(sql_select);
            req_select.setString(1, email);
            ResultSet res_select = req_select.executeQuery();
            if (!res_select.next())
                Assert.fail("La sélection du compte (res_select.next()) ajouté a échoué");

            int id_compte = res_select.getInt("id");
            Assert.assertFalse("Compte pas ajouté", id_compte == 0);

            // Delete
            PreparedStatement req_delete = pdo.prepareStatement(sql_delete);
            req_delete.setInt(1, id_compte);
            int res_delete = req_delete.executeUpdate();
            Assert.assertEquals("Supression du compte échouée", res_delete, 1);

            DB.close();
        }
        catch (Exception ex) {
            Assert.fail("Exception lors du test du compte : " + ex.getMessage());
        }
    }
}