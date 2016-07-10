package analysis.value;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;

public class PathValue extends TreeValue<Node, StrValue, PathValue> implements AbstractValue<PathValue> {

    public PathValue(TreeValue.Type type, Object leafValue) {
        super(type, leafValue);
        // TODO Auto-generated constructor stub
    }

    @Override
    public PathValue leastUpperBound(PathValue other) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void solveVar(Node target, Node expression) {
        // TODO Auto-generated method stub
    }

    @Override
    public PathValue copy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void reduce() {
        // TODO Auto-generated method stub
    }

}
