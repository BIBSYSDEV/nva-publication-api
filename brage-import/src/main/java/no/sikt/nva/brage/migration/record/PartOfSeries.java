package no.sikt.nva.brage.migration.record;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class PartOfSeries {

    private String name;
    private String number;

    public PartOfSeries() {

    }

    public PartOfSeries(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getNumber());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PartOfSeries that = (PartOfSeries) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getNumber(), that.getNumber());
    }
}
