package navigation;
import models.TitleDesc;
import utils.TextDeco;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;
import java.io.IOException;
import java.io.UncheckedIOException;

public class Menu extends TitleDesc {
    private static Terminal terminal = null;
    private final Choice[] choices;
    private int choice_index;

    public Menu(String title, String desc, Choice[] choices, int choice_index) {
        super(title, desc);
        this.choices = choices;

        // Vérifie la validité de l'indice du choix
        if (choice_index != -1 && (choice_index < 0 || choice_index >= this.choices.length))
            throw new IllegalArgumentException(
            "L'indice du choix (" + choice_index + ") doit être à -1 ou un indice valide d'un choix (de 0 à " + (this.choices.length - 1) + ")");

        // Si l'indice pointe un choix désactivé, -1
        if (choice_index != -1 && !this.choices[choice_index].get_state())
            this.choice_index = -1;
        else
            this.choice_index = choice_index;
    }

    /**
     * Initialise le terminal de l'application
     * @return `true` si le terminal a correctement été initialisé, sinon `false`
     */
    public static boolean init_terminal() {
        try {
            Menu.terminal = TerminalBuilder.builder().system(true).build();
            return true;
        }
        catch (Exception ex) {
            IO.println("Le terminal n'a pas pu être initialisé : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Passe en mode sélection du choix du menu
     * La navigation s'effectue avec les flèches du clavier
     * La sélection du choix se fait avec la touche Entrer
     * @return Le title du choix sélectionné, null si aucun n'est sélectionné
     */
    public String run() {
        if (Menu.terminal == null) {
            IO.println("Le terminal doit être initialisé en appelant Menu.init_terminal().");
            System.exit(1);
        }

        Attributes saved = Menu.terminal.enterRawMode();
        NonBlockingReader reader = Menu.terminal.reader();

        try {
            this.print();
            boolean running = true;

            while (running) {
                // Sleep 16 ms
                int input = reader.read(1000 / 60);

                // Aucune touche
                if (input == NonBlockingReader.READ_EXPIRED)
                    continue;

                // Input fermé
                if (input == NonBlockingReader.EOF)
                    break;

                // CTRL + C : exit le programme
                if (input == 3) {
                    Menu.terminal.setAttributes(saved);
                    System.exit(0);
                }

                // Touche Entrer
                if (input == '\r' || input == '\n')
                    running = false;

                // Touche flèche / Echap
                else if (input == 27) {
                    int c1 = reader.read(50);

                    // Si touche flèche (multiplateforme)
                    if (c1 == '[' || c1 == 'O') {
                        int c2 = reader.read(50);

                        // Flèche haut
                        if (c2 == 'A') {
                            this.select_previous_choice();
                            this.print();
                        }
                        // Flèche bas
                        else if (c2 == 'B') {
                            this.select_next_choice();
                            this.print();
                        }
                    }
                }
            }
        }
        catch (IOException ex) {
            // Ignore l'exception
            throw new UncheckedIOException(ex);
        }
        finally {
            // Restaure le terminal
            Menu.terminal.setAttributes(saved);
        }

        Choice choice = this.get_selected_choice();
        if (choice != null)
            return choice.get_title();

        return null;
    }

    /**
     * Affiche le menu et ses différents choix
     */
    public void print() {
        // Clear la console
        IO.println("\u001B[H\u001B[2J");
        System.out.flush();

        // Title et desc du menu
        IO.println(TextDeco.BLUE + this.get_title());
        if (this.get_desc() != null)
            IO.println(TextDeco.AQUA + this.get_desc());

        IO.print("\n");

        for (int i = 0; i < this.choices.length; i++) {
            Choice choice = this.choices[i];

            boolean is_selected = this.choice_index == i;
            boolean is_locked = !choice.get_state();

            // Title du choix, surligné si sélectionné, gris si bloqué
            if (is_selected)
                IO.println(TextDeco.HIGHLIGHT_BLUE + TextDeco.BLACK + " > " + choice.get_title() + " " + TextDeco.RESET);
            else if (is_locked)
                IO.println(TextDeco.GREY + "   " + choice.get_title() + " " + TextDeco.RESET);
            else
                IO.println(TextDeco.WHITE + "   " + choice.get_title() + " " + TextDeco.RESET);
        }

        // Desc du choix sélectionné
        Choice selected_choice = this.get_selected_choice();
        if (selected_choice != null && selected_choice.get_desc() != null)
            IO.println("\n" + TextDeco.AQUA + selected_choice.get_desc() + TextDeco.RESET + "\n");
        else
            IO.print("\n");
    }

    /**
     * Récupère le choix sélectionné
     * @return `null` si l'indice du choix est à -1, sinon l'objet `Choice` sélectionné
     */
    private Choice get_selected_choice() {
        if (this.choice_index == -1)
            return null;

        return this.choices[this.choice_index];
    }

    /**
     * Sélectionne le prochain choix disponible
     */
    public void select_next_choice() {
        // Mémorisation de l'ancien index
        int remember = this.choice_index;

        // Si aucun choix sélectionné, sélectionne le premier
        if (this.choice_index == -1) {
            this.choice_index = 0;
            return;
        }

        // Séléctonne le prochain choix disponible
        int tries = 0;
        do {
            // Si tous les choix ont été essayés, on restaure l'index à l'ancien
            if (tries == this.choices.length) {
                this.choice_index = remember;
                break;
            }

            // Modification de l'indice
            this.choice_index++;
            this.choice_index %= this.choices.length;
            tries++;
        }
        while (!this.choices[this.choice_index].get_state());
    }

    /**
     * Sélectionne le choix précédent le plus proche
     */
    public void select_previous_choice() {
        // Mémorisation de l'ancien index
        int remember = this.choice_index;

        // Si aucun choix sélectionné, sélectionne le dernier
        if (this.choice_index == -1) {
            this.choice_index = this.choices.length - 1;
            return;
        }

        // Séléctonne le prochain choix disponible
        int tries = 0;
        do {
            // Si tous les choix ont été essayés, on restaure l'index à l'ancien
            if (tries == this.choices.length) {
                this.choice_index = remember;
                break;
            }

            // Modification de l'indice
            this.choice_index--;
            if (this.choice_index < 0)
                this.choice_index += this.choices.length; // % ne marche pas sur nombres négatifs

            tries++;
        }
        while (!this.choices[this.choice_index].get_state());
    }
}
