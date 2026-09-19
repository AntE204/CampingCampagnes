package utils;
import java.text.DecimalFormatSymbols;
import java.util.Scanner;

public class Input {
    private static final Scanner scanner = new Scanner(System.in);

    public static String get_string(String message) {
        IO.print(message);
        return Input.scanner.nextLine();
    }

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
