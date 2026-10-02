package navigation;
import utils.DB;
import utils.Input;
import utils.TextDeco;

public class Navigation {
    /**
     * Menu "Se connecter / Quitter"
     * @return Le title du choix sélectionné
     */
    public static String menu_login() {
        Choice[] choices = {
            new Choice("Se connecter", "Si vous ne possédez pas de compte, demandez à la responsable Anne Himation d'en créer un pour vous."),
            new Choice("Quitter", null)
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

            boolean login_res = DB.connexion(email, password);

            if (login_res)
                return true;

            ask = Input.get_bool(TextDeco.RED + "Identifiants incorrects. " + TextDeco.RESET + "Réessayer? (o/n) : ", 'o', 'n');
        }
        return false;
    }

    /**
     * Demande les informations d'un nouveau compte et l'ajoute en base de données
     * @param force_director Saute la question du type de compte pour créer un compte directeur
     */
    public static boolean register_account(boolean force_director) {
        IO.println("Aucun compte directeur n'existe dans la base de données. Veuillez en créer un.");

        String email;
        while (true) {
            email = Input.get_string("Email : ");
            if (DB.verifMail(email))
                break;

            IO.println("L'email n'est pas valide !");
        }

        String password;
        while (true) {
            password = Input.get_password("Mot de passe : ");
            if (DB.passwordStrong(password))
                break;

            IO.println("Le mot de passe doit :\n-Comporter 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial\n- Compoter au moins 12 caractères");
        }

        while (!Input.get_password("Confirmer le mot de passe : ").equals(password))
            IO.println("Le mot de passe ne correspond pas !");

        String first_name = Input.get_string("Prénom : ");
        String last_name = Input.get_string("Nom : ");
        String street = Input.get_string("Rue : ");
        String town = Input.get_string("Ville : ");
        String zip_code = Input.get_string("Code postal : ");
        String phone = Input.get_string("Numéro de téléphone (facultatif) : ");
        boolean is_director = force_director || Input.get_bool("Compte directeur ? (o/n) : ", 'o', 'n');

        boolean res = DB.registerAccount(email, phone, last_name, first_name, street, town, zip_code, password, is_director);
        IO.println(res ? "Le compte " + email + " a été ajouté dans la base de données !" : "Le compte n'a pas pu être ajouté dans la base de données.");
        return res;
    }
}
