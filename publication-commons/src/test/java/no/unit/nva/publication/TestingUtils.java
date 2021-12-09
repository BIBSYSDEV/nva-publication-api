package no.unit.nva.publication;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import java.net.URI;

public final  class TestingUtils {

    private TestingUtils(){

    }

    public static URI createOrgUnitId(int first, int second, int third, int fourth) {
        return URI.create(String.format("https://example.org/some/path/%s.%s.%s.%s", first, second, third, fourth));
    }

    public static URI createRandomOrgUnitId() {
        return URI.create(String.format("https://example.org/some/path/%s.%s.%s.%s",
                                        randomInteger(),
                                        randomInteger(),
                                        randomInteger(),
                                        randomInteger()));
    }


}
