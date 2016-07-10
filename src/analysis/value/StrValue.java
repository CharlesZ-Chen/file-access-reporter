package analysis.value;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;

public class StrValue extends TreeValue<Node, String, StrValue> implements AbstractValue<StrValue>{

    public StrValue(Type type, Object leafValue) {
        super(type, leafValue);
    }

    public StrValue (Type type) {
        super(type);
    }

    public StrValue(StrValue left, StrValue right) {
        super(left, right);
    }

    @Override
    public StrValue leastUpperBound(StrValue other) {
        if (this.type == Type.TOP || other.getType() == Type.TOP) {
            return new StrValue(Type.TOP);
        }

        // lub would only get called when two branches needed to merged, thus the lub would always be Type.MERGE
        StrValue lub = new StrValue(Type.MERGE);
        mergeStrValue(lub, this);
        mergeStrValue(lub, other);
        return lub;
    }

    public static void mergeStrValue (StrValue target, StrValue source) {
        assert target.type ==  Type.MERGE;
        if (source.type == Type.TOP) {
            return;
        }

        if (source.type == Type.MERGE) {
            for (StrValue strValue : source.mergedSet) {
                StrValue copy = strValue.copy();
                target.mergedSet.add(copy);
            }
        } else {
            StrValue copy = source.copy();
            target.mergedSet.add(copy);
        }
    }

    public static StrValue buildStrValue(Node node) {
        if (node instanceof StringLiteralNode) {
            return new StrValue(Type.REDUCED, ((StringLiteralNode) node).getValue());
        }else if (node instanceof LocalVariableNode ||
                node instanceof FieldAccessNode) {
            return new StrValue(Type.VAR, node);
        } else if (node instanceof StringConcatenateNode) {
            StringConcatenateNode scNode = (StringConcatenateNode) node;
            StrValue left = buildStrValue(scNode.getLeftOperand());
            StrValue right = buildStrValue(scNode.getRightOperand());
            StrValue root = new StrValue(left, right);
            return root;
        } else {
            System.out.println("===========missing consideration of this: " + node.getClass());
            return new StrValue(Type.VAR, node);
        }
    }

    @Override
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
                    StrValue concatenateValue = buildStrValue(expression);
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
    public StrValue copy() {
        switch (this.type) {
            case TOP: {
                return new StrValue(Type.TOP);
            }

            case MERGE: {
                StrValue copy = new StrValue(Type.MERGE);
                for (StrValue strValue : this.mergedSet) {
                    copy.mergedSet.add(strValue.copy());
                }
                return copy;
            }

            case VAR:
            case REDUCED: {
                if (this.isLeaf) {
                    return new StrValue(this.type, this.leafValue);
                }

                StrValue left = this.left.copy();
                StrValue right = this.right.copy();
                return new StrValue(left, right);
            }

            default:
                assert false;
                return null;
        }
    }
}
