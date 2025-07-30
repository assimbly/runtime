package org.assimbly.util.helper;

@FunctionalInterface
public interface Filter<E> {

    /**
     * Returns true if element e is accepted by this filter, else false.
     * @param e The element.
     * @return true if element e is accepted by this filter, else false.
     */
    boolean accept(E e);
}