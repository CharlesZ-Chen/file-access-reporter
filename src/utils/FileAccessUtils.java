package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.checkerframework.dataflow.analysis.AbstractValue;

public class FileAccessUtils {
    public static <K, V extends AbstractValue<V>>
        Map<K, V> merge(Map<K, V> leftMap, Map<K, V> rightMap) {

        Map<K, V> mergedMap = new HashMap<>();
        for (Entry<K, V> entry : leftMap.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (rightMap.containsKey(key)) {
                value = value.leastUpperBound(rightMap.get(key));
            }
            // TODO: is safe here using the original value?
            // provide a copy maybe better?
            mergedMap.put(key, value);
        }

        for (Entry<K, V> entry : rightMap.entrySet()) {
            mergedMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return mergedMap;
    }

    public static <E>
    Set<E> merge(Set<E> set1, Set<E> set2) {
        Set<E> mergedSet = new HashSet<>();
        mergedSet.addAll(set1);
        mergedSet.addAll(set2);
        return mergedSet;
    }

    public static<K, V>
    boolean isSuperMap(Map<K, V> superMap, Map<K, V> subMap) {
        for (Entry<K, V> entry : subMap.entrySet()) {
            K key = entry.getKey();
            if (!superMap.containsKey(key)) {
                return false;
            }
            if (!superMap.get(key).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public static<E>
    boolean isSuperSet(Set<E> superSet, Set<E> subSet) {
        for (E trackVar : subSet) {
            if (!superSet.contains(trackVar)) {
                return false;
            }
        }
        return true;
    }

    /**
     * debug print out utility
     * @param map
     */
    public static <K, V>
    void printMap (Map<K, V> map) {
        System.out.println("===map size is: " + map.size());
        for (Entry<K, V> entry : map.entrySet()) {
            System.out.println("\t" + entry.getKey() + " -> " + entry.getValue());
        }
    }

}
