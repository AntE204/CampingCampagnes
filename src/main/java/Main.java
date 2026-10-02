import navigation.Menu;
import navigation.Navigation;
import utils.DB;

public class Main {
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

        // Demande nom et mdp, quitte si ne parvient pas à se connecter au compte
        if (!Navigation.ask_login())
            exit(0);
    }

    private static void exit(int code) {
        DB.close();
        System.exit(code);
    }
}