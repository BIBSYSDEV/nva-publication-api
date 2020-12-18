package no.unit.nva.publication.service;

import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Table;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PublicationsDynamoDBLocalTest {

    private PublicationsDynamoDBLocal db;

    @BeforeEach
    public void setUp() throws Throwable {
        db = new PublicationsDynamoDBLocal();
        db.before();
    }

    @AfterEach
    public void tearDown() {
        db.after();
    }

    @Test
    public void canGetByPublishedDateIndex() {
        Index index = db.getByPublishedDateIndex();
        assertThat(index, is(notNullValue()));
    }

    @Test
    public void canGetByPublisherIndex() {
        Index index = db.getByPublisherIndex();
        assertThat(index, is(notNullValue()));
    }

    @Test
    public void canGetTable() {
        Table table = db.getTable();
        assertThat(table, is(notNullValue()));
    }

}
