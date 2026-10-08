import navigation.Menu;
import navigation.Navigation;
import utils.DB;

public class Main {
    public static String test = null;

    public static void main(String[] args) {
        // Connexion à la DB
        if (!DB.init())
            exit(1);

        // Init le terminal
        if (!Menu.init_terminal())
            exit(1);

        // Menu : Se connecter / Quitter
        if (Navigation.menu_login().equals("Quitter"))
            exit(0);

        // Demande à créer un compte directeur si aucun n'existent
        if (DB.noDirectorAccount())
            if (!Navigation.register_account(true, null))
                exit(1);

        // Demande nom et mdp, quitte si ne parvient pas à se connecter au compte
        if (!Navigation.ask_login())
            exit(0);

        boolean stay = true;
        while (stay) {
            String main_choice = Navigation.menu_main(DB.firstName);
            stay = !main_choice.equals("Quitter");

            if (main_choice.equals("Créer un compte"))
                Navigation.register_account(false, null);

            else if (main_choice.equals("Modifier un compte"))
                Navigation.edit_an_account();

            else if (main_choice.equals("Supprimer un compte")) {
                if (Navigation.delete_account())
                    System.exit(0);
            }

            else if (main_choice.equals("Créer une animation"))
                Navigation.create_animation();

            else if (main_choice.equals("Attribuer des animateurs"))
                Navigation.set_animators();

            else if (main_choice.equals("Envoyer les plannings"))
                Navigation.send_plannings();

            else if (main_choice.equals("Modifier mon compte"))
                Navigation.edit_my_account();

            else if (main_choice.equals("Recevoir mon planning"))
                Navigation.receive_my_planning();
        }

        exit(0);
    }

    private static void exit(int code) {
        DB.close();
        System.exit(code);
    }
}