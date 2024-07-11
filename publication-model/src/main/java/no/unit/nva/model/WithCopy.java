package no.unit.nva.model;

public interface WithCopy<T> {

    /**
     * Returns a Builder filled in with a copy of the data of the original object.
     *
     * @return a builder instance with filled in data.
     */
    T copy();
}
