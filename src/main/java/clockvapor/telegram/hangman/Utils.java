package clockvapor.telegram.hangman;

public class Utils {
    public static boolean isLetterOrDigit(int codePoint) {
        return Character.isLetterOrDigit(codePoint);
    }

    public static char[] toChars(int codePoint) {
        return Character.toChars(codePoint);
    }
}
