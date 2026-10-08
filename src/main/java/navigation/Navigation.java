package navigation;
import dbres.DBDeleteAccount;
import dbres.DBRes;
import utils.DB;
import utils.Input;
import utils.TextDeco;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Navigation {
    private static Choice choice_exit = new Choice("Quitter", null);
    /**
     * Menu "Se connecter / Quitter"
     * @return Le title du choix sélectionné
     */
    public static String menu_login() {
        Choice[] choices = {
            new Choice("Se connecter", "Si vous ne possédez pas de compte, demandez à la responsable Anne Himation d'en créer un pour vous."),
            Navigation.choice_exit
        };
        Menu menu = new Menu("Connexion", null, choices, 0);
        return menu.run();
    }

    /**
     * Demande le nom et mdp du compte utilisateur
     * Propose de réessayer si identifiants invalides
     * @return Si la connexion au compte a fonctionné
     */
    public static boolean ask_login() {
        IO.println("Connectez-vous à votre compte.");

        boolean ask = true;
        while (ask) {
            String email = Input.get_string("Email : ");
            String password = Input.get_password("Mot de passe : ");

            DBRes res = DB.connexion(email, password);

            if (res.ok)
                return true;

            ask = Input.get_bool(TextDeco.RED + "Identifiants incorrects. " + TextDeco.RESET + "Réessayer? (o/n) : ", 'o', 'n');
        }
        return false;
    }

    /**
     * Permet de créer ou modifier un compte en demandant les informations
     * @param force_director Si le compte créé est le 1er compte de la base (forcément directeur)
     * @param old_email En cas de modification de compte, l'ancien email de l'utilisateur
     */
    public static boolean register_account(boolean force_director, String old_email) {
        if (force_director)
            IO.println("Aucun compte directeur n'existe dans la base de données. Veuillez en créer un.");

        // Email
        String email;
        while (true) {
            email = Input.get_string("Email : ");

            DBRes res = DB.emailUsed(email);
            if (!res.ok && old_email == null)
                IO.println(res.error);

            else if (DB.verifMail(email))
                break;

            else
                IO.println("L'email n'est pas valide !");
        }

        // MDP
        String password;
        while (true) {
            password = Input.get_password("Mot de passe : ");
            if (DB.passwordStrong(password))
                break;

            IO.println("Le mot de passe doit :\n-Comporter 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial\n- Compoter au moins 12 caractères");
        }

        while (!Input.get_password("Confirmer le mot de passe : ").equals(password))
            IO.println("Le mot de passe ne correspond pas !");

        // Autres infos
        String first_name = Input.get_string("Prénom : ");
        String last_name = Input.get_string("Nom : ");
        String street = Input.get_string("Rue : ");
        String town = Input.get_string("Ville : ");
        String zip_code = Input.get_string("Code postal : ");
        String phone = Input.get_string("Numéro de téléphone (facultatif) : ");
        boolean is_director = force_director || Input.get_bool("Compte directeur ? (o/n) : ", 'o', 'n');

        // Insert ou update
        DBRes res;
        if (old_email == null)
            res = DB.registerAccount(email, phone, last_name, first_name, street, town, zip_code, password, is_director);
        else
            res = DB.editAccount(old_email, email, phone, last_name, first_name, street, town, zip_code, password, is_director);

        // Popup en cas de succès
        if (res.ok) {
            String popup = "Le compte de " + first_name + " " + last_name + " a été " + (old_email == null ? "créé" : "modifié") + " !";
            if (force_director)
                popup += " Veuillez vous y connecter.";
            Menu.set_popup(popup);
        }
        else
            Menu.set_popup("Le compte n'a pas pu être ajouté dans la base de données : " + res.error);

        return res.ok;
    }

    /**
     * Menu principal, adapté en fonction des droits
     * @return Le title du choix sélectionné
     */
    public static String menu_main(String first_name) {
        Choice[] choices_directeur = {
            new Choice("Créer un compte", "Créez un compte directeur ou animateur."),
            new Choice("Modifier un compte", "Modifiez le compte d'un utilisateur."),
            new Choice("Supprimer un compte", "Supprimez le compte d'un utilisateur. Toutes les données du compte seront effacées."),
            new Choice("Créer une animation", "Créez une animation."),
            new Choice("Attribuer des animateurs", "Attribuez ou retirez des animateurs à une animation."),
            new Choice("Envoyer les plannings", "Envoyez les plannings hebdomadaires à chaque animateur."),
            Navigation.choice_exit
        };

        Choice[] choices_animateur = {
            new Choice("Modifier mon compte", "Modifiez les informations de votre compte."),
            new Choice("Recevoir mon planning", "Recevez le planning de vos animations de la semaine."),
            Navigation.choice_exit
        };

        Menu menu = new Menu("Bonjour " + first_name + ".", null, DB.isDirecteur ? choices_directeur : choices_animateur, 0);
        return menu.run();
    }

    /**
     * Demande les informations d'un compte à modifier
     */
    public static void edit_an_account() {
        String old_email = Input.get_string("Entrez l'email du compte à modifier : ");

        // Email pas utilisé
        if (!DB.verifMail(old_email)) {
            Menu.set_popup("Cet email n'est pas utilisé !");
            return;
        }

        Navigation.register_account(false, old_email);
    }

    /**
     * Supprime un compte
     * @return Si l'utilisateur supprimé correspond à l'utilisateur actuellement connecté
     */
    public static boolean delete_account() {
        String email = Input.get_string("Entrez l'email de l'utilisateur à supprimer : ");

        // Email pas utilisé
        if (!DB.verifMail(email)) {
            Menu.set_popup("Cet email n'est pas utilisé !");
            return false;
        }

        DBDeleteAccount res = DB.deleteAccount(email);
        if (!res.ok) {
            if (res.is_self)
                IO.println(res.error);
            else
                Menu.set_popup(res.error);
        }

        return res.is_self;
    }

    public static void create_animation() {
        String name = Input.get_string("Nom de l'animation : ");

        DBRes res_exists = DB.animationExists(name);
        if (!res_exists.ok) {
            Menu.set_popup(res_exists.error);
            return;
        }

        int length = Input.get_int("Durée de l'animation en minutes : ");
        DBRes res_create = DB.createAnimation(name, length);
        Menu.set_popup(res_create.ok ? "L'animation a été créée !" : res_create.error);
    }

    public static void set_animators() {
        // TODO
    }

    public static void send_plannings() {
        // TODO
    }

    public static void edit_my_account() {
        // TODO
    }

    public static void receive_my_planning() {
        // TODO
    }
}
