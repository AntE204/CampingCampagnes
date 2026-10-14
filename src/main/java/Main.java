import models.Config;
import navigation.Menu;
import navigation.Navigation;
import utils.DB;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        // Récupère la config
        Config config = new Config("./config.json");
        if (!config.is_valid()) {
            config.print_error();
            exit(1);
        }

        // Connexion à la DB
        if (!DB.init(config))
            exit(1);

        // Init le terminal
        if (!Menu.init_terminal())
            exit(1);
        System.out.println(DB.checkAnimationPeriod(9, LocalDateTime.parse("2027-11-09T00:00:00"), LocalDateTime.parse("2027-11-17T00:00:00")));
        // DB.envoiMail(LocalDateTime.parse("2026-10-12T00:00:00"), LocalDateTime.parse("2026-10-18T00:00:00"), 9);

        // Menu : Se connecter / Quitter
        if (Navigation.menu_login().equals("Quitter"))
            exit(0);

        // Demande à créer un compte directeur si aucun n'existent
        if (DB.noDirectorAccount())
            if (!Navigation.register_account(true, "directeur", null))
                exit(1);

        // Demande nom et mdp, quitte si ne parvient pas à se connecter au compte
        if (!Navigation.ask_login())
            exit(0);

        boolean stay = true;
        while (stay) {
            String main_choice = Navigation.menu_main(DB.firstName);
            stay = !main_choice.equals("Quitter");

            if (main_choice.equals("Créer un compte"))
                Navigation.register_account(false, null, null);

            else if (main_choice.equals("Modifier un compte"))
                Navigation.edit_an_account(null, true);

            else if (main_choice.equals("Supprimer un compte")) {
                if (Navigation.delete_account())
                    System.exit(0);
            }

            else if (main_choice.equals("Créer une animation"))
                Navigation.create_animation();

            else if (main_choice.equals("Attribuer des animateurs"))
                Navigation.set_animators();

            else if (main_choice.equals("Envoyer les plannings"))
                Navigation.send_plannings(config);

            else if (main_choice.equals("Modifier mon compte"))
                Navigation.edit_my_account();

            else if (main_choice.equals("Recevoir mon planning"))
                Navigation.receive_my_planning(config);
        }

        exit(0);
    }

    private static void exit(int code) {
        DB.close();
        System.exit(code);
    }
}