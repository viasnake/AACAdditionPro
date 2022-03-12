package de.photon.anticheataddition.util.datastructure.kdtree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an implementation with a backing {@link HashSet}.
 * Therefore, any remove and contains operations are O(1), but the getAny() operation is costly.
 */
public class QuadTreeSet<T> extends QuadTreeCollection<T>
{
    private final Set<Node<T>> nodes = new HashSet<>();

    @Override
    protected Collection<Node<T>> getBackingCollection()
    {
        return nodes;
    }

    @Override
    public Node<T> getAny()
    {
        var iter = nodes.iterator();
        return iter.hasNext() ? iter.next() : null;
    }

    @Override
    public Node<T> removeAny()
    {
        var any = getAny();
        this.remove(any);
        return any;
    }
}
