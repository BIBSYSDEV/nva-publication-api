package no.unit.nva.model.funding;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class FundingList implements List<Funding> {

    private final List<Funding> fundings;

    public FundingList(List<Funding> fundings) {
        this.fundings = Objects.nonNull(fundings) ? fundings : Collections.emptyList();
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
    public void add(int index, Funding element) {
        fundings.add(index, element);
    }

    @Override
    public boolean remove(Object o) {
        return fundings.remove(o);
    }

    @Override
    public Funding remove(int index) {
        return fundings.remove(index);
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
    public boolean addAll(int index, Collection<? extends Funding> c) {
        return fundings.addAll(index, c);
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
    public void replaceAll(UnaryOperator<Funding> operator) {
        fundings.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super Funding> c) {
        fundings.sort(c);
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
    public Funding get(int index) {
        return fundings.get(index);
    }

    @Override
    public Funding set(int index, Funding element) {
        return fundings.set(index, element);
    }

    @Override
    public int indexOf(Object o) {
        return fundings.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return fundings.lastIndexOf(o);
    }

    @Override
    public ListIterator<Funding> listIterator() {
        return fundings.listIterator();
    }

    @Override
    public ListIterator<Funding> listIterator(int index) {
        return fundings.listIterator(index);
    }

    @Override
    public List<Funding> subList(int fromIndex, int toIndex) {
        return fundings.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<Funding> spliterator() {
        return fundings.spliterator();
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
