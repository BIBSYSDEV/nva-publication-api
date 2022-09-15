package no.unit.nva.publication.model.business;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

public class ViewedBy implements Set<User> {
    
    private final Set<User> views;
    
    public ViewedBy(Set<User> viewedBy) {
        this.views = Objects.nonNull(viewedBy) ? viewedBy : new HashSet<>();
    }
    
    public static ViewedBy addAll(String... users) {
        var viewedBy = Arrays.stream(users)
                           .map(User::new)
                           .collect(Collectors.toSet());
        return new ViewedBy(new HashSet<>(viewedBy));
    }
    
    public void remove(String owner) {
        views.remove(new User(owner));
    }
    
    @JacocoGenerated
    @Override
    public int size() {
        return views.size();
    }
    
    @JacocoGenerated
    @Override
    public boolean isEmpty() {
        return views.isEmpty();
    }
    
    @JacocoGenerated
    @Override
    public boolean contains(Object o) {
        return views.contains(o);
    }
    
    @JacocoGenerated
    @Override
    public Iterator<User> iterator() {
        return views.iterator();
    }
    
    @JacocoGenerated
    @Override
    public Object[] toArray() {
        return new Object[0];
    }
    
    @JacocoGenerated
    @Override
    public <T> T[] toArray(T[] a) {
        return views.toArray(a);
    }
    
    @JacocoGenerated
    @Override
    public boolean add(User user) {
        return views.add(user);
    }
    
    @JacocoGenerated
    @Override
    public boolean remove(Object o) {
        return views.remove(o);
    }
    
    @JacocoGenerated
    @Override
    public boolean containsAll(Collection<?> c) {
        return views.containsAll(c);
    }
    
    @JacocoGenerated
    @Override
    public boolean addAll(Collection<? extends User> c) {
        return views.addAll(c);
    }
    
    @JacocoGenerated
    @Override
    public boolean retainAll(Collection<?> c) {
        return views.retainAll(c);
    }
    
    @JacocoGenerated
    @Override
    public boolean removeAll(Collection<?> c) {
        return views.removeAll(c);
    }
    
    @JacocoGenerated
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViewedBy)) {
            return false;
        }
        ViewedBy users = (ViewedBy) o;
        return Objects.equals(views, users.views);
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(views);
    }
}
