package no.unit.nva.publication.model.business.publicationchannel;

import java.util.List;
import nva.commons.core.JacocoGenerated;

public class Constraint {

    private final ChannelPolicy publishingPolicy;
    private final ChannelPolicy editingPolicy;
    private final List<String> scope;

    public Constraint(ChannelPolicy publishingPolicy, ChannelPolicy editingPolicy, List<String> scope) {
        this.publishingPolicy = publishingPolicy;
        this.editingPolicy = editingPolicy;
        this.scope = scope;
    }

    @JacocoGenerated
    public ChannelPolicy getPublishingPolicy() {
        return publishingPolicy;
    }

    @JacocoGenerated
    public ChannelPolicy getEditingPolicy() {
        return editingPolicy;
    }

    @JacocoGenerated
    public List<String> getScope() {
        return scope;
    }
}
