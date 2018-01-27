package net.uniplovdiv.fmi.cs.vrs.event.parameters;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Comparable ArrayList used by the engine for bulk comparison of lists and arrays using compareTo method.
 * This class requires to be inherited in order to be instantiated.
 * The recommended approach working for serialization is to extend the class like that:
 * {@code
 *   StringComparableArrayList extends ComparableArrayList<String> {
 *     public StringComparableArrayList() { super(); }
 *     public StringComparableArrayList(StringComparableArrayList o) { super(o); }
 *   }
 * }
 *
 * Alternative approach is to use an anonymous class, however the serialization process will be greatly hindered:
 * {@code
 *   ComparableArrayList<String> a = new ComparableArrayList<String>(){};
 * }
 *
 * @param <T> The data types stored inside the ComparableArrayList. It the type has to be Comparable, but the checks
 *           about that will be made during the run-time. N.B. arrays are not comparable thus for e.g. having a
 *           String[] as T will throw an exception.
 */
@SuppressWarnings("unchecked")
public abstract class ComparableArrayList<T /*extends Comparable<T>*/> extends ArrayList<T> implements
        Serializable, List<T>, Comparable<ComparableArrayList<T>> {
    private static final long serialVersionUID = -543544340308401710L;

    {
        TypeResolver tr = new TypeResolver();
        ResolvedType resolvedType = tr.resolve(this.getClass());
        ResolvedType resolvedTypeParam = resolvedType.getParentClass().getTypeParameters().get(0);
        //System.out.println(resolvedTypeParam);

        boolean isGood = false;
        if (resolvedTypeParam != null) {
            Class<?> typeParam = resolvedTypeParam.getErasedType();
            if (typeParam != null) {
                //System.out.println("Caught something " + typeParam.getCanonicalName());
                isGood = typeParam.equals(Object.class) || Comparable.class.isAssignableFrom(typeParam);
            }
        }
        if (!isGood) {
            throw new ArrayStoreException(
                    "Generic data type not implementing java.lang.Comparable<T> used for instantiation of ComparableArrayList<T>");
        }
    }

    /**
     * Constructor. Constructs an empty list with an initial capacity of ten.
     * @throws ArrayStoreException If T class type parameter does not implement java.lang.Comparable
     */
    public ComparableArrayList() {
        super();
    }

    /**
     * Constructor. Constructs a list containing the elements of the specified collection, in the order they are
     * returned by the collection's iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws ArrayStoreException If T class type parameter does not implement java.lang.Comparable
     * @throws NullPointerException if the specified collection is null
     */
    public ComparableArrayList(Collection<? extends T> c) {
        super(c);
    }

    /**
     * Copy constructor.
     * @param c the collection whose elements are to be placed into this list
     * @throws ArrayStoreException If T class type parameter does not implement java.lang.Comparable
     * @throws NullPointerException if the specified collection is null
     */
    public ComparableArrayList(ComparableArrayList<T> c) {
        this((Collection<T>)c);
    }

    /**
     * Constructor. Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws ArrayStoreException If T class type parameter does not implement java.lang.Comparable
     */
    public ComparableArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public int compareTo(ComparableArrayList<T> o) {
        if (o == null) return 1;
        if (this == o) return 0;

        int sizeA = this.size();
        int sizeB = o.size();

        if (sizeA == 0 && sizeB == 0) return 0;
        if (sizeA == 0 && sizeB != 0) return -1;
        if (sizeA != 0 && sizeB == 0) return 1;

        // now the item-by-item comparison - the loop runs as long as items in both arrays are equal
        for (int i = 0; ; i++)
        {
            // shorter array whose items are all equal to the first items of a longer array is considered 'less than'
            boolean pastA = (i == sizeA);
            boolean pastB = (i == sizeB);
            if (pastA && !pastB)
                return -1; // "a < b"
            else if (!pastA && pastB)
                return 1; // "a > b"
            else if (pastA && pastB)
                return 0; // "a = b", same length, all items equal

            Comparable<T> ai = (Comparable<T>) this.get(i);
            Comparable<T> bi = (Comparable<T>) o.get(i);
            if (ai == null && bi == null)
                continue; // again, two null items are assumed 'equal'

            // arbitrary: non-null item is considered 'greater than' null item
            if (ai == null)
                return -1; // "a < b"
            else if (bi == null)
                return 1; // "a > b"

            int comp = ai.compareTo((T)bi);
            if (comp != 0)
                return comp;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof ComparableArrayList && (this.getClass() == obj.getClass())) {
            ComparableArrayList<T> _obj = (ComparableArrayList<T>)obj;
            return compareTo(_obj) == 0;
        }
        return false;
    }

    //@Override
    //public int hashCode() {
    // don't have to implement it actually, but since equals is implemented we "do" that
    //    return super.hashCode();
    //}
}
