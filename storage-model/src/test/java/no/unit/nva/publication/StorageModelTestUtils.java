package no.unit.nva.publication;

import com.github.javafaker.Faker;

public final class StorageModelTestUtils {

    private static Faker FAKER = Faker.instance();

    private StorageModelTestUtils() {

    }

    public static String randomString() {
        return FAKER.lorem().sentence();
    }
}
