package no.unit.nva.schemaorg.document;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PersonList implements List<Person>, PersonI {

    private final List<Person> persons;

    public PersonList(List<Person> persons) {
        this.persons = persons;
    }

    @Override
    public int size() {
        return persons.size();
    }

    @Override
    public boolean isEmpty() {
        return persons.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return persons.contains(o);
    }

    @Override
    public Iterator<Person> iterator() {
        return persons.iterator();
    }

    @Override
    public Person[] toArray() {
        return persons.toArray(Person[]::new);
    }

    @Override
    public Object[] toArray(Object[] objects) {
        return persons.toArray(objects);
    }

    @Override
    public boolean add(Person o) {
        return persons.add(o);
    }

    @Override
    public void add(int i, Person o) {
        persons.add(i, o);
    }

    @Override
    public boolean remove(Object o) {
        return persons.remove(o);
    }

    @Override
    public Person remove(int i) {
        return persons.remove(i);
    }

    @Override
    public boolean addAll(Collection collection) {
        return persons.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection collection) {
        return persons.addAll(i, collection);
    }

    @Override
    public void clear() {
        persons.clear();
    }

    @Override
    public Person get(int i) {
        return persons.get(i);
    }

    @Override
    public Person set(int i, Person o) {
        return persons.set(i, o);
    }

    @Override
    public int indexOf(Object o) {
        return persons.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return persons.lastIndexOf(o);
    }

    @Override
    public ListIterator<Person> listIterator() {
        return persons.listIterator();
    }

    @Override
    public ListIterator<Person> listIterator(int i) {
        return persons.listIterator(i);
    }

    @Override
    public List<Person> subList(int i, int i1) {
        return persons.subList(i, i1);
    }

    @Override
    public boolean retainAll(Collection collection) {
        return persons.retainAll(collection);
    }

    @Override
    public boolean removeAll(Collection collection) {
        return persons.removeAll(collection);
    }

    @Override
    public boolean containsAll(Collection collection) {
        return persons.containsAll(collection);
    }
}
