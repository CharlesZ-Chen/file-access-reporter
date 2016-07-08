package analysis.value;

import java.util.List;

import org.checkerframework.dataflow.cfg.node.Node;

import analysis.FileAccessStore;

public class SingleStrPath extends PathValue {

    private final StrValue path;

    public SingleStrPath(StrValue path) {
        this.path = path;
    }

    /**
     * @return immutable reference view of this.path
     * TODO: should return deep immutable view of this.path
     */
    public final StrValue getPath() {
        return path;
    }

    @Override
    public SingleStrPath leastUpperBound(PathValue other) {
        SingleStrPath otherSingleStrPath = (SingleStrPath) other;
        StrValue otherPath = otherSingleStrPath.getPath();
        System.out.println("merge " + this + " with " + other);
        StrValue newPath = this.path.leastUpperBound(otherPath);
        SingleStrPath lub = new SingleStrPath(newPath);
        System.out.println("after merge: " + lub);
        return lub;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(FileAccessStore store) {
        
        List<String> pathValueList = path.getValues();
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append(path.getType().name() + " [ ");
        for (String pathValue : pathValueList) {
            sBuilder.append(pathValue + ", ");
        }
        sBuilder.append(" ]");
        return sBuilder.toString();
    }

    @Override
    public void solveVar(Node var, Node expression) {
        System.out.println("before solve: " + this);
        path.solve(var, expression);
        System.out.println("after solve: " + this);
        
    }

    @Override
    public PathValue copy() {
        return new SingleStrPath(path.copy());
    }
}
