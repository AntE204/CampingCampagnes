import navigation.Menu;
import navigation.Navigation;

public class Main {
    public static void main(String[] args) {
        // Init le terminal
        if (!Menu.init_terminal())
            System.exit(1);

        // Menu : Se connecter / Quitter
        if (Navigation.menu_login().equals("Quitter"))
            return;

        // Demande nom et mdp, quitte si ne parvient pas à se conencter au compte
        if (Navigation.ask_login())
            return;
    }
}
