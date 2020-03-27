package no.unit.nva;

import java.util.Arrays;

@JacocoGenerated
public class Logger {
    
    public static void logError(Throwable e) {
        System.out.println(Arrays.asList(e.getStackTrace()));
    }

    public static void log(String message) {
        System.out.println(message);
    }

}
