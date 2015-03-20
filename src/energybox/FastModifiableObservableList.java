package energybox;

import javafx.collections.ModifiableObservableListBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JavaFX list that does not fire change on every modification.
 *
 * This dramatically speeds up performance for larger (~10+ MB) trace files.
 * When modifying the list using single item modifying methods (add(), remove() ,...),
 * beforeChanges() and afterChanges() must be called surrounding such change-blocks.
 *
 * @param <E> Type. Should be Integer in this project most likely.
 */
public final class FastModifiableObservableList<E> extends ModifiableObservableListBase<E> {

    private final List<E> delegate = new ArrayList<>();

    @Override
    public boolean addAll(Collection<? extends E> col) {
        beginChange();
        boolean retval = delegate.addAll(col);
        endChange();
        return retval;
    }

    /**
     * MUST be called BEFORE a set of changes (add(), remove(), etc.) and be FOLLOWED by a call to afterChanges() to
     * fire change events.
     */
    public void beforeChanges() {
        beginChange();
    }

    /**
     * MUST be called AFTER a set of changes (add(), remove(), etc.) and FOLLOWING a call to beforeChanges() to
     * fire change events.
     */
    public void afterChanges() {
        endChange();
    }

    public E get(int index) {
        return delegate.get(index);
    }

    public int size() {
        return delegate.size();
    }

    protected void doAdd(int index, E element) {
        delegate.add(index, element);
    }

    protected E doSet(int index, E element) {
        return delegate.set(index, element);
    }

    protected E doRemove(int index) {
        return delegate.remove(index);
    }

}
