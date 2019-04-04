package ueb_blatt_1.src;

import java.util.Comparator;
import java.util.function.Consumer;

public interface List<S, T> {
    int size();

    void add(S value1, T value2);

    int indexOf(ListElement<S, T> e);

    ListElement<S, T> get(int index);

    ListElement<S, T> remove(int index);

    void forEach(Consumer<ListElement<S, T>> action);

    void reverse();

    void doSelectionSort(Comparator<ListElement<S, T>> comp);
}
