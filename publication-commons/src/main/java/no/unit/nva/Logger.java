package no.unit.nva;

public class Logger {


    public static void logError(Throwable e) {
        e.printStackTrace();
    }

    public static void log(String message) {
        System.out.println(message);
    }

}
