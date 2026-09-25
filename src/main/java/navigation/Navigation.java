package navigation;
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
        boolean ask = true;
        while (ask) {
            String id = Input.get_string("Email : ");
            String password = Input.get_password("Mot de passe : ");

            // TODO connexion à la DB
            boolean login_res = false; // Simulation temporaire

            if (login_res)
                return true;

            ask = Input.get_bool(TextDeco.RED + "Identifiants incorrects. " + TextDeco.RESET + "Réessayer? (o/n) : ", 'o', 'n');
        }
        return false;
    }
}
