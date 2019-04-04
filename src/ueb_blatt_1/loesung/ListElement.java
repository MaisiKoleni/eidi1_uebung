package ueb_blatt_1.loesung;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;

public final class ListElement<S, T> {

    private final ReentrantReadWriteLock val1Lock;
    private final ReentrantReadWriteLock val2Lock;

    private ListElement<S, T> next;

    private S val1;
    private T val2;

    ListElement(S val1, T val2) {
        this(null, val1, val2);
    }

    ListElement(ListElement<S, T> next, S val1, T val2) {
        this.val1Lock = new ReentrantReadWriteLock();
        this.val2Lock = new ReentrantReadWriteLock();
        this.next = next;
        this.val1 = val1;
        this.val2 = val2;
    }

    final ListElement<S, T> getNext() {
        return next;
    }

    final void setNext(ListElement<S, T> next) {
        this.next = next;
    }

    final ReentrantReadWriteLock getVal1Lock() {
        return val1Lock;
    }

    final ReentrantReadWriteLock getVal2Lock() {
        return val2Lock;
    }

    public final S getVal1() {
        val1Lock.readLock().lock();
        S temp = val1;
        val1Lock.readLock().unlock();
        return temp;
    }

    public final T getVal2() {
        val2Lock.readLock().lock();
        T temp = val2;
        val2Lock.readLock().unlock();
        return temp;
    }

    public final void setVal1(S val1) {
        val1Lock.writeLock().lock();
        this.val1 = val1;
        val1Lock.writeLock().unlock();
    }

    public final void setVal2(T val2) {
        val2Lock.writeLock().lock();
        this.val2 = val2;
        val2Lock.writeLock().unlock();
    }

    public final void updateVal1(UnaryOperator<S> operation) {
        val1Lock.writeLock().lock();
        try {
            this.val1 = Objects.requireNonNull(operation, "Operator must not be null").apply(val1);
        } finally {
            val1Lock.writeLock().unlock();
        }
    }

    public final void updateVal2(UnaryOperator<T> operation) {
        val2Lock.writeLock().lock();
        try {
            this.val2 = Objects.requireNonNull(operation, "Operator must not be null").apply(val2);
        } finally {
            val2Lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "(" + val1 + ", " + val2 + ")";
    }

    @Override
    public boolean equals(Object obj) {
        /*
         * equals zu synchronisieren ist begrenzt sinnvoll, gemacht habe ich es, um
         * wenigstens Cache-Kohärenz zu erzwingen; zu Fehlern führt es jedenfalls nicht,
         * man kann es nur nicht wirklich sinnvoll einsetzten. Hier bräuchte es eine
         * equals(Object o, Consumer<Boolean> callback) Methode.
         */
        if (this == obj)
            return true;
        if (!(obj instanceof ListElement))
            return false;
        ListElement<?, ?> other = (ListElement<?, ?>) obj;
        this.val1Lock.readLock().lock();
        this.val2Lock.readLock().lock();
        other.val1Lock.readLock().lock();
        other.val2Lock.readLock().lock();
        try {
            return Objects.equals(val1, other.val1) && Objects.equals(val2, other.val2);
        } finally {
            other.val2Lock.readLock().unlock();
            other.val1Lock.readLock().unlock();
            this.val2Lock.readLock().unlock();
            this.val1Lock.readLock().unlock();
        }
    }

    @Override
    public int hashCode() {
        /*
         * Die einzige legale Möglichkeit, hashCode() gemäß Kontrakt für komplett
         * veränderliche Objekte zu implementieren
         */
        return 0;
    }
}
