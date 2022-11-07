package no.unit.nva.schemaorg.document;

import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public final class FramedSchemaOrgDocumentBuilder {
    private PersonI creator;
    private Organization provider;
    private URI context;
    private URI id;
    private Object type;

    private String name;

    private FramedSchemaOrgDocumentBuilder() {
    }

    public static FramedSchemaOrgDocumentBuilder newInstance() {
        return new FramedSchemaOrgDocumentBuilder();
    }

    public FramedSchemaOrgDocumentBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public FramedSchemaOrgDocumentBuilder withCreator(List<Contributor> creators) {
        var persons = generatorCreatorList(creators);
        this.creator = creators.size() == 1 ? persons.get(0) : persons;
        return this;
    }

    private PersonList generatorCreatorList(List<Contributor> creators) {
        var persons = creators.stream()
                .map(Contributor::getIdentity)
                .map(Identity::getName)
                .map(Person::new)
                .collect(Collectors.toList());
        return new PersonList(persons);
    }

    public FramedSchemaOrgDocumentBuilder withProvider(URI uri, String name) {
        this.provider = new Organization(uri, name);
        return this;
    }

    public FramedSchemaOrgDocumentBuilder withContext(URI context) {
        this.context = context;
        return this;
    }

    public FramedSchemaOrgDocumentBuilder withId(URI id) {
        this.id = id;
        return this;
    }

    public FramedSchemaOrgDocumentBuilder withType(List<String> type) {
        this.type = type.size() == 1 ? type.get(0) : type;
        return this;
    }

    public FramedSchemaOrgDocument build() {
        return new FramedSchemaOrgDocument(context, id, type, name, creator, provider);
    }
}
