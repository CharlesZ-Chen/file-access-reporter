package analysis.value;


import org.checkerframework.dataflow.cfg.node.Node;


public class TreeValueFactory {

    public static <V extends Node, R, T extends TreeValue<V, R, T>>
    T createLeaf(Object leafValue, TreeValue.Type type) {
        return null;
    }

    public static <V extends Node, R, T extends TreeValue<V, R, T>>
    T createCopy(final T source) {
        return null;
    }
}
