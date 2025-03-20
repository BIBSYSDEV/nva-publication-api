package no.unit.nva.model.associatedartifacts;

import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class AssociatedArtifactList implements List<AssociatedArtifact> {

    private final List<AssociatedArtifact> associatedArtifacts = new ArrayList<>();

    @JsonCreator
    public AssociatedArtifactList(List<AssociatedArtifact> artifacts) {
        if (containsNotOnlyNullAssociatedArtifacts(artifacts)) {
            var notNullAssociatedArtifacts = artifacts.stream()
                                                 .filter(not(NullAssociatedArtifact.class::isInstance))
                                                 .distinct()
                                                 .toList();
            this.associatedArtifacts.addAll(notNullAssociatedArtifacts);
        } else if (nonNull(artifacts)) {
            this.associatedArtifacts.addAll(artifacts.stream().distinct().toList());
        }
    }

    public AssociatedArtifactList(AssociatedArtifact... artifacts) {
        this(Arrays.asList(artifacts));
    }

    public static AssociatedArtifactList empty() {
        return new AssociatedArtifactList(Collections.emptyList());
    }

    @Override
    public int size() {
        return associatedArtifacts.size();
    }

    @Override
    public boolean isEmpty() {
        return associatedArtifacts.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return associatedArtifacts.contains(o);
    }

    @Override
    public Iterator<AssociatedArtifact> iterator() {
        return associatedArtifacts.iterator();
    }

    @Override
    public Object[] toArray() {
        return associatedArtifacts.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return associatedArtifacts.toArray(a);
    }

    @Override
    public boolean add(AssociatedArtifact associatedArtifact) {
        return associatedArtifacts.add(associatedArtifact);
    }

    @Override
    public boolean remove(Object o) {
        return associatedArtifacts.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return associatedArtifacts.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends AssociatedArtifact> c) {
        return associatedArtifacts.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends AssociatedArtifact> c) {
        return associatedArtifacts.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return associatedArtifacts.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return associatedArtifacts.retainAll(c);
    }

    @Override
    public void clear() {
        associatedArtifacts.clear();
    }

    @Override
    public AssociatedArtifact get(int index) {
        return associatedArtifacts.get(index);
    }

    @Override
    public AssociatedArtifact set(int index, AssociatedArtifact element) {
        return associatedArtifacts.set(index, element);
    }

    @Override
    public void add(int index, AssociatedArtifact element) {
        associatedArtifacts.add(index, element);
    }

    @Override
    public AssociatedArtifact remove(int index) {
        return associatedArtifacts.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return associatedArtifacts.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return associatedArtifacts.lastIndexOf(o);
    }

    @Override
    public ListIterator<AssociatedArtifact> listIterator() {
        return associatedArtifacts.listIterator();
    }

    @Override
    public ListIterator<AssociatedArtifact> listIterator(int index) {
        return associatedArtifacts.listIterator(index);
    }

    @Override
    public List<AssociatedArtifact> subList(int fromIndex, int toIndex) {
        return associatedArtifacts.subList(fromIndex, toIndex);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(associatedArtifacts);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        return associatedArtifacts.equals(o);
    }

    private boolean containsNotOnlyNullAssociatedArtifacts(List<AssociatedArtifact> artifacts) {
        return nonNull(artifacts) && artifacts.stream().anyMatch(NullAssociatedArtifact.class::isInstance) &&
               !artifacts.stream().allMatch(NullAssociatedArtifact.class::isInstance);
    }
}
