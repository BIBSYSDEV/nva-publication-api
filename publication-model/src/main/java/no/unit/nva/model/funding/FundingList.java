package no.unit.nva.model.funding;

import static java.util.Collections.emptySet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FundingList implements Set<Funding> {

    private final Set<Funding> fundings;

    public FundingList(Collection<Funding> fundings) {
        this.fundings = Objects.nonNull(fundings) ? new HashSet<>(fundings) : emptySet();
    }

    @Override
    public int size() {
        return fundings.size();
    }

    @Override
    public boolean isEmpty() {
        return fundings.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return fundings.contains(o);
    }

    @Override
    public Iterator<Funding> iterator() {
        return fundings.iterator();
    }

    @Override
    public Object[] toArray() {
        return fundings.toArray();
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return fundings.toArray(generator);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return fundings.toArray(a);
    }

    @Override
    public boolean add(Funding funding) {
        return fundings.add(funding);
    }

    @Override
    public boolean remove(Object o) {
        return fundings.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return fundings.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Funding> c) {
        return fundings.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return fundings.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return fundings.retainAll(c);
    }

    @Override
    public void clear() {
        fundings.clear();
    }

    @Override
    public boolean equals(Object o) {
        return fundings.equals(o);
    }

    @Override
    public int hashCode() {
        return fundings.hashCode();
    }

    @Override
    public boolean removeIf(Predicate<? super Funding> filter) {
        return fundings.removeIf(filter);
    }

    @Override
    public Stream<Funding> stream() {
        return fundings.stream();
    }

    @Override
    public Stream<Funding> parallelStream() {
        return fundings.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super Funding> action) {
        fundings.forEach(action);
    }
}
