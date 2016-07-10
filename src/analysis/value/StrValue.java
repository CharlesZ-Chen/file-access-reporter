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
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof StrValue)) {
            return false;
        }

        StrValue other = (StrValue) obj;
        if (this.type != other.type) {
            return false;
        }

        switch(other.type) {
            case TOP:
            case MERGE:
            case VAR:
            case REDUCED:
            default:
                assert false;
                return false;
        }
    };
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

    public StrValue buildStrValue(Node node) {
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
        // TODO Auto-generated method stub
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
