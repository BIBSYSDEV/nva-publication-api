package no.unit.nva.model.pages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class NullPages implements Pages {
    public static final NullPages NULL_PAGES = new NullPages();
}
