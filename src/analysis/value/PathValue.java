package analysis.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.TypesUtils;

import utils.TreeValueUtils;

/**
 * In PathValue, only the left-most child could be a VAR
 * @author charleszhuochen
 *
 */
public class PathValue extends TreeValue<Node, StrValue, PathValue> implements AbstractValue<PathValue> {

    public PathValue(TreeValue.Type type) {
        super(type);
    }

    public PathValue(TreeValue.Type type, Object leafValue) {
        super(type, leafValue);
    }

    public PathValue(PathValue left, PathValue right) {
        super(left, right);
        if (right.type != Type.REDUCED) {
            throw new RuntimeException("right tree of PathValue can only be REDUCED, but get " + right.type);
        }
    }

    @Override
    public void putToVarTable(Node target, PathValue singleLeaf) {
        if (root.varTable != null && !root.varTable.isEmpty()) {
            throw new RuntimeException("Path value should only hold one variable! varTable is not empty when put variable into table: " + root.varTable);
        }
        List<PathValue> leafList = new ArrayList<>(1);
        leafList.add(singleLeaf);
        root.varTable.put(target, Collections.unmodifiableList(leafList));
    }

    @Override
    public void putToVarTable(Node target, List<PathValue> leafList) {
        if (root.varTable != null && !root.varTable.isEmpty()) {
            throw new RuntimeException("Path value should only hold one variable! varTable is not empty when put variable into table: " + root.varTable);
        }
        if (leafList.size() != 1) {
            throw new RuntimeException("leafList should exactly contains one leaf!");
        }
        root.varTable.put(target, Collections.unmodifiableList(leafList));
    }

    @Override
    public PathValue leastUpperBound(PathValue other) {
        // TODO Auto-generated method stub
        return null;
    }

    public void solveFileVar(Node target, PathValue substitution) {
        switch(this.type) {
            case TOP: return;
            case MERGE: {
                for (PathValue pathValue : mergedSet) {
                    if (pathValue.type == Type.VAR) {
                        pathValue.solveFileVar(target, substitution);
                    }
                }
            }

            case VAR: {
                if (root.varTable.containsKey(target)) {
                    solve(target, substitution);
                }
            }

            case REDUCED: {
                return;
            }

            default:
                assert false;
        }
    }

    public void solveFileVar(Node target, Node substitution) {
        if (!TypesUtils.isDeclaredOfName(substitution.getType(), "java.io.File")) {
            throw new RuntimeException("expect java.io.File type, but get substitution type: " + substitution.getType());
        }

        switch (this.type) {
        case TOP: return;

        case MERGE: {
            for (PathValue pathValue : mergedSet) {
                if (pathValue.type == Type.VAR) {
                    pathValue.solveFileVar(target, substitution);
                }
            }
        }

        case VAR: {
            if (root.varTable.containsKey(target)) {
                solve(target, substitution);
            }
        }

        case REDUCED: {
            return;
        }

        default:
            assert false;
            return;
        }
    }

    //TODO: currently I do a recursive search to solve str value.
    //TODO: need to think how to improve performance by avoiding brute-force like this
    public void solveStrVar(Node target, Node expression) {
        switch (this.type) {
        case TOP: return;
        case MERGE: {
            for (PathValue pathValue : mergedSet) {
                pathValue.solveStrVar(target, expression);
            }
        }
        case VAR:
        case REDUCED: {
            if (this.isLeaf) {
                if (this.leafValue instanceof StrValue) {
                    StrValue strValue = (StrValue) this.leafValue;
                    strValue.solveVar(target, expression);
                }
            } else {
                this.left.solveStrVar(target, expression);
                this.right.solveStrVar(target, expression);
            }
        }
        default:
            assert false;
            return;
        }
    }

    @Override
    public PathValue copy() {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public void reduce() {
        // TODO Auto-generated method stub
    }

}
