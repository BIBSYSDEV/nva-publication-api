package no.unit.nva.model.instancetypes.researchdata;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public abstract class UriSet implements Set<URI> {

    private final Set<URI> uris;

    protected UriSet(Set<URI> uris) {
        this.uris = uris;
    }

    @Override
    public int size() {
        return uris.size();
    }

    @Override
    public boolean isEmpty() {
        return uris.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return uris.contains(o);
    }

    @Override
    public Iterator<URI> iterator() {
        return uris.iterator();
    }

    @Override
    public Object[] toArray() {
        return uris.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return uris.toArray(a);
    }

    @Override
    public boolean add(URI uri) {
        return uris.add(uri);
    }

    @Override
    public boolean remove(Object o) {
        return uris.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return uris.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends URI> c) {
        return uris.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return uris.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return uris.removeAll(c);
    }

    @Override
    public void clear() {
        uris.clear();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UriSet)) {
            return false;
        }
        UriSet uris1 = (UriSet) o;
        return Objects.equals(uris, uris1.uris);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(uris);
    }
}
