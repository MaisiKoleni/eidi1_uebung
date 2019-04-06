package ueb_blatt_1.loesung;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class ConcurrentList<S, T> implements List<S, T> {

	private final ReentrantReadWriteLock lock;

	private ListElement<S, T> first;

	public ConcurrentList() {
		lock = new ReentrantReadWriteLock();
	}

	@Override
	public void add(S value1, T value2) {
		lock.writeLock().lock();
		try {
			if (first == null)
				first = new ListElement<>(value1, value2);
			else
				addRecursive(first, value1, value2);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void addRecursive(ListElement<S, T> e, S value1, T value2) {
		if (e.getNext() == null)
			e.setNext(new ListElement<>(value1, value2));
		else
			addRecursive(e.getNext(), value1, value2);
	}

	@Override
	public ListElement<S, T> get(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException(index);
		lock.readLock().lock();
		try {
			return getRecursive(first, index);
		} finally {
			lock.readLock().unlock();
		}
	}

	private ListElement<S, T> getRecursive(ListElement<S, T> e, int index) {
		if (e == null)
			throw new IndexOutOfBoundsException();
		if (index == 0)
			return e;
		return getRecursive(e.getNext(), index - 1);
	}

	@Override
	public ListElement<S, T> remove(int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException(index);
		lock.writeLock().lock();
		ListElement<S, T> res = null;
		try {
			if (index == 0) {
				if (first == null)
					throw new IndexOutOfBoundsException(index);
				res = first;
				first = first.getNext();
			} else
				res = removeRecursive(first, index);
		} finally {
			lock.writeLock().unlock();
		}
		return res;
	}

	private ListElement<S, T> removeRecursive(ListElement<S, T> e, int index) {
		ListElement<S, T> next = e.getNext();
		if (next == null)
			throw new IndexOutOfBoundsException();
		if (index == 1) {
			e.setNext(next.getNext());
			return next;
		}
		return removeRecursive(e.getNext(), index - 1);
	}

	@Override
	public int size() {
		lock.readLock().lock();
		try {
			return sizeRecursive(first);
		} finally {
			lock.readLock().unlock();
		}
	}

	private int sizeRecursive(ListElement<S, T> e) {
		if (e == null)
			return 0;
		return 1 + sizeRecursive(e.getNext());
	}

	@Override
	public int indexOf(ListElement<S, T> e) {
		if (e == null)
			return -1;
		lock.readLock().lock();
		try {
			return indexOf(e, first, 0);
		} finally {
			lock.readLock().unlock();
		}
	}

	private int indexOf(ListElement<S, T> e, ListElement<S, T> current, int index) {
		if (current == null)
			return -1;
		if (current.equals(e))
			return index;
		return indexOf(e, current.getNext(), index + 1);
	}

	@Override
	public void reverse() {
		lock.writeLock().lock();
		try {
			if (first == null || first.getNext() == null)
				return;
			ListElement<S, T> oldFirst = first;
			first = reverseRecursive(first);
			oldFirst.setNext(null);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private ListElement<S, T> reverseRecursive(ListElement<S, T> e) {
		ListElement<S, T> next = e.getNext();
		if (next == null)
			return e;
		ListElement<S, T> res = reverseRecursive(next);
		next.setNext(e);
		return res;
	}

	@Override
	public void doSelectionSort(Comparator<ListElement<S, T>> comp) {
		Objects.requireNonNull(comp, "Comparator must not be null");
		if (first == null || first.getNext() == null)
			return;
		lock.writeLock().lock();
		try {
			doSelectionSortRecursive(first, 0, comp);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void doSelectionSortRecursive(ListElement<S, T> e, int index, Comparator<ListElement<S, T>> comp) {
		if (e == null)
			return;
		e.getVal1Lock().readLock().lock();
		e.getVal2Lock().readLock().lock();
		ListElement<S, T> max = null, newE = null;
		try {
			doSelectionSortRecursive(e.getNext(), index + 1, comp);
			max = maximumUpTo(first, index, comp);
			newE = get(index);
			swap(max, newE);
		} finally {
			if (max != null) {
				max.getVal2Lock().readLock().unlock();
				max.getVal1Lock().readLock().unlock();
			} else {
				if (newE == null)
					newE = get(index);
				newE.getVal2Lock().readLock().unlock();
				newE.getVal1Lock().readLock().unlock();
			}
		}
	}

	private ListElement<S, T> maximumUpTo(ListElement<S, T> current, int end, Comparator<ListElement<S, T>> comp) {
		if (end == 0)
			return current;
		ListElement<S, T> max = maximumUpTo(current.getNext(), end - 1, comp);
		if (comp.compare(current, max) > 0)
			return current;
		return max;
	}

	private void swap(ListElement<S, T> e1, ListElement<S, T> e2) {
		if (e1 == e2)
			return;
		swap(first, e1, e2, null);
	}

	private void swap(ListElement<S, T> current, ListElement<S, T> e1, ListElement<S, T> e2,
			ListElement<S, T> previous) {
		if (current == null)
			return;
		ListElement<S, T> next = current.getNext();
		boolean e1IsCurrent = current == e1;
		boolean e2IsCurrent = current == e2;
		if (e1IsCurrent || e2IsCurrent) {
			ListElement<S, T> other = e1IsCurrent ? e2 : e1;
			if (next == other) {
				current.setNext(other.getNext());
				other.setNext(current);
				if (previous == null)
					first = other;
				else
					previous.setNext(other);
				return;
			}
			// we can only do that because e1 always comes before e2, removing the if allows
			// for any order but is slower
			if (!e2IsCurrent)
				swap(next, e1, e2, current);
			if (previous == null)
				first = other;
			else
				previous.setNext(other);
			other.setNext(next);
		} else
			swap(next, e1, e2, current);
	}

	@Override
	public void forEach(Consumer<ListElement<S, T>> action) {
		Objects.requireNonNull(action, "Consumer must not be null");
		lock.readLock().lock();
		try {
			forEachRecursive(first, action);
		} finally {
			lock.readLock().unlock();
		}
	}

	private void forEachRecursive(ListElement<S, T> current, Consumer<ListElement<S, T>> action) {
		if (current == null)
			return;
		action.accept(current);
		forEachRecursive(current.getNext(), action);
	}

	@Override
	public boolean equals(Object obj) {
		/*
		 * Ähnlich wie bei ListElement.
		 */
		if (obj == this)
			return true;
		if (!(obj instanceof ConcurrentList))
			return false;
		ConcurrentList<?, ?> other = (ConcurrentList<?, ?>) obj;
		this.lock.readLock().lock();
		other.lock.readLock().lock();
		try {
			return equalsRecursive(first, other.first);
		} finally {
			other.lock.readLock().unlock();
			this.lock.readLock().unlock();
		}
	}

	private boolean equalsRecursive(ListElement<?, ?> e1, ListElement<?, ?> e2) {
		if (e1 == e2)
			return true;
		if (e1 == null || e2 == null)
			return false;
		if (!e1.equals(e2))
			return false;
		return equalsRecursive(e1.getNext(), e2.getNext());
	}

	@Override
	public String toString() {
		if (first == null)
			return "[]";
		return "[" + toStringRecursive(first);
	}

	private String toStringRecursive(ListElement<S, T> e) {
		ListElement<S, T> next = e.getNext();
		if (next == null)
			return e.toString() + "]";
		return e.toString() + ", " + toStringRecursive(next);
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
