package analysis.value;

import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;

import com.sun.tools.javac.parser.Tokens;

import utils.TreeValueUtils;

public class StrValue extends TreeValue<Node, String, StrValue> {

    public StrValue(Type type, Object leafValue) {
        super(type, leafValue);
    }

    public StrValue (Type type) {
        super(type);
    }

    public StrValue(StrValue left, StrValue right) {
        super(left, right);
    }

    public void solveVar(Node target, Node expression) {
        switch (this.type) {
            case TOP: return;
            case MERGE: {
                System.out.println("====before solve: " + this.toString());
                for (StrValue strValue : mergedSet) {
                    strValue.solveVar(target, expression);
                }
                System.out.println("------after solve: " + this.toString());
                return;
            }

            case REDUCED: {
                return; // reduced type doesn't need to solve anymore
            }

            case VAR: {
                if (!root.varTable.containsKey(target)) {
                    return;
                }

                if (expression instanceof StringLiteralNode) {
                    StringLiteralNode literalNode = (StringLiteralNode) expression;
                    solve(target, literalNode.getValue());
                } else if (expression instanceof StringConcatenateNode) {
                    StrValue concatenateValue = TreeValueUtils.createStrValue(expression);
                    solve(target, concatenateValue);
                } else {
                    solve(target, expression);
                }
            }
            default:
                assert false;
                return;
        }
        
    }

    @Override
    public void reduce() {
        Type thisType = this.type;
        switch (thisType) {
            case TOP: {
                return;
            }

            case MERGE: {
                for (StrValue strValue : mergedSet) {
                    strValue.reduce();
                }
                return;
            }

            case REDUCED: {
                if (this.isLeaf) {
                    if (leafValue instanceof String) {
                        return;
                    } else {
                        throw new  RuntimeException("leaf strValue type is REDUCED, but leafValue is not String! leafValue is: " + leafValue);
                    }
                }
                //otherwise using same code in case VAR
            }

            case VAR: {
                if (this.isLeaf) {
                    if (this.leafValue instanceof String) {
                        this.type = Type.REDUCED; //TODO: need think clear whether this is safe
                    }
                    return;
                }
                if (!left.isLeaf) {
                    left.reduce();
                }
                if (!right.isLeaf) {
                    right.reduce();
                }
                if ((left.isLeaf && left.leafValue instanceof String) &&
                        (right.isLeaf && right.leafValue instanceof String)) {
                    String newLeafValue = ((String) left.leafValue) + ((String) right.leafValue);
                    this.leafValue = newLeafValue;
                    this.isLeaf = true;
                    this.left = null;
                    this.right = null;
                    this.type = Type.REDUCED;
                    // also clean varTable and mergeSet
                    this.varTable = null;
                    this.mergedSet = null;
                }
            }

            default:
                assert false;
                return;
        }
    }

    @Override
    protected StrValue getSubclassInstance() {
        return this;
    }

    @Override
    protected StrValue createInstance(TreeValue.Type type) {
        return new StrValue(type);
    }

    @Override
    protected StrValue createInstance(TreeValue.Type type, Object leafValue) {
        return new StrValue(type, leafValue);
    }

    @Override
    protected StrValue createInstance(StrValue left, StrValue right) {
        return new StrValue(left, right);
    }

}
