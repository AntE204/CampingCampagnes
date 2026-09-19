package utils;
import java.text.DecimalFormatSymbols;
import java.util.Scanner;

public class Input {
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Demande une entrée texte
     * @param message Le message à afficher dans la console
     * @return Le nombre rentré par l'utilisateur
     */
    public static String get_string(String message) {
        IO.print(message);
        return Input.scanner.nextLine();
    }

    public static String get_password(String message) {
        IO.print(message);
         return new String(System.console().readPassword());
    }

    /**
     * Demande une entrée d'un nombre entier
     * La fonction boucle tant que l'entrée n'est pas un nombre entier
     * @param message Le message à afficher dans la console
     * @return Le nombre entier rentré par l'utilisateur
     */
    public static int get_int(String message) {
        while (true) {
            IO.print(message);
            try {
                return Input.scanner.nextInt();
            }
            catch (Exception ex) {
                // L'exception est ignorée
            }
        }
    }

    /**
     * Demande une entrée d'un nombre flottant
     * La fonction boucle tant que l'entrée n'est pas un nombre flottant
     * @param message Le message à afficher dans la console
     * @return Le nombre flottant rentré par l'utilisateur
     */
    public static double get_double(String message) {
        while (true) {
            IO.print(message);
            try {
                // On prend l'input en string, on force le caractère décimal en point, puis on cast la valeur en double
                String input_str = Input.scanner.nextLine();
                DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                input_str = input_str.replace(dfs.getDecimalSeparator(), '.');
                return Double.parseDouble(input_str);
            }
            catch (Exception ex) {
                // L'exception est ignorée
            }
        }
    }

    /**
     * Demande une entrée à deux choix possibles (généralement oui ou non)
     * La fonction boucle tant que l'entrée n'est pas un des deux caractères possibles
     * @param message Le message à afficher dans la console
     * @param yes Le caractère à rentrer considéré comme le choix positif
     * @param no Le caractère à rentrer considéré comme le choix négatif
     * @return Un booléen représentant le choix de l'utilisateur
     */
    public static boolean get_bool(String message, char yes, char no) {
        while (true) {
            IO.print(message);
            try {
                char input = Input.scanner.nextLine().charAt(0);

                if (input == yes)
                    return true;

                else if (input == no)
                    return false;
            }
            catch (Exception ex) {
                // L'exception est ignorée
            }
        }
    }
}
