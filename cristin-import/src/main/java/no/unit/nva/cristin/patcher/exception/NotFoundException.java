package no.unit.nva.cristin.patcher.exception;

import java.lang.reflect.Executable;

public class NotFoundException extends RuntimeException {

    public NotFoundException(Exception e) {
        super(e);
    }

}
