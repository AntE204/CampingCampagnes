package navigation;
import models.TitleDesc;

public class Menu extends TitleDesc {
    private Choice[] choices;
    private int choice_index;

    public Menu(String title, String desc, Choice[] choices, int choice_index) {
        super(title, desc);
        this.choices = choices;
        this.choice_index = choice_index;
    }

    /**
     * Affiche le menu et ses différents choix
     * TODO
     */
    public void print() {

    }

    /**
     * Sélectionne le prochain choix disponible
     */
    public void select_next_choice() {
        // Mémorisation de l'ancien index
        int remember = this.choice_index;

        // Si placé sur le dernier choix ou aucun, wrap au premier
        if (this.choice_index == this.choices.length - 1 || this.choice_index == -1)
            this.choice_index = 0;

        // Séléctonne le prochain choix disponible
        int tries = 0;
        do {
            // Si tous les choix ont été essayés, on restaure l'index à l'ancien
            if (tries == this.choices.length) {
                this.choice_index = remember;
                break;
            }

            // Si dernier choix dépassé, retour au premier
            this.choice_index++;
            if (this.choice_index >= this.choices.length)
                this.choice_index = 0;

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

        // Si placé sur le premier choix ou aucun, wrap au dernier
        if (this.choice_index == 0 || this.choice_index == -1)
            this.choice_index = this.choices.length - 1;

        // Séléctonne le prochain choix disponible
        int tries = 0;
        do {
            // Si tous les choix ont été essayés, on restaure l'index à l'ancien
            if (tries == this.choices.length) {
                this.choice_index = remember;
                break;
            }

            // Si en-dessous du premier choix, retour au dernier
            this.choice_index--;
            if (this.choice_index < 0)
                this.choice_index = this.choices.length - 1;

            tries++;
        }
        while (!this.choices[this.choice_index].get_state());
    }
}
