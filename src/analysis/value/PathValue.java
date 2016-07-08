package analysis.value;

import javax.swing.text.DefaultEditorKit.CopyAction;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;

import analysis.FileAccessStore;


public abstract class PathValue implements AbstractValue<PathValue> {

    public enum Type {
        TOP,
        FILE_STR_PATH,
        STR_COM_PATH,
        URI_PATH,
        SINGLE_STR_PATH,
        BOTTOM;
    }

    public abstract void solveVar(Node var, Node expression);
    public abstract PathValue copy();

    @Override
    public PathValue leastUpperBound(PathValue other) {
        if (this.getClass() != other.getClass()) {
            throw new RuntimeException("unmatch run time class of pathValue! the run time class should be equal, while left is: " + this.getClass() +
                    "\tright is: " + other.getClass());
        }
        return this.leastUpperBound(other);
    }
}
