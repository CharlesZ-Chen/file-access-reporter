package analysis.value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;

public abstract class TreeValue <V extends Node, R, T extends TreeValue<V, R, T>> {
    public enum Type {
        TOP,
        MERGE,
        VAR,
        REDUCED;
    }

    protected Type type;
    protected boolean isLeaf;
    protected Object leafValue;
    protected Set<T> mergedSet;
    protected T root;
    protected T left;
    protected T right;
    
    protected Hashtable<V, List<T>> varTable;

    //TODO: move logic to TreeValue(Type, Object) make this a wrapper of leafValue=null
    public TreeValue (Type type) {
        this(type, null);
    }

    @SuppressWarnings("unchecked")
    public TreeValue (Type type, Object leafValue) {
        root = (T) this;
        this.type = type;
        isLeaf = true;
        this.leafValue = leafValue;
        mergedSet = null;
        left = null;
        right = null;
        switch (type) {
        case TOP: {
            return;
        }
        case MERGE: {
            mergedSet = new HashSet<>();
            return;
        }

        case VAR: {
            root.varTable = new Hashtable<>();
            putToVarTable((V) leafValue, (T) this);
            return;
        }

        case REDUCED: {
            root.varTable = null;
        }
        default:
            break;
        }
    }

    @SuppressWarnings("unchecked")
    public TreeValue (T leftTree, T rightTree) {
        if (leftTree.type == Type.MERGE || rightTree.type == Type.MERGE ||
                leftTree.type == Type.TOP || rightTree.type == Type.TOP) {
            throw new RuntimeException("MERGE or TOP tree cannot be a subTree! leftTree type: " + leftTree.type + "rightTree type: " + rightTree.type);
        }

        if (leftTree.type == Type.REDUCED && rightTree.type == Type.REDUCED) {
            this.type = Type.REDUCED;
        } else {
            this.type = Type.VAR;
        }

        mergedSet = null;
        this.root = (T) this;
        leftTree.root = (T) this;
        rightTree.root = (T) this;
        this.left = leftTree;
        this.right = rightTree;
        isLeaf = false;

        root.varTable = new Hashtable<>();
        if (leftTree.varTable != null) {
            root.varTable.putAll(leftTree.varTable);
            leftTree.varTable = null;
        }

        if (rightTree.varTable != null) {
            for (Entry<V, List<T>> entry : rightTree.varTable.entrySet()) {
                V key = entry.getKey();
                List<T> valueList = entry.getValue();
                putToVarTable(key, valueList);
            }
            rightTree.varTable = null;
        }

        //TODO: should avoid calling overridable method in constrcutor, maybe lazy evaluation?
        if (this.type == Type.REDUCED) {
            this.reduce();
        }
    }

    protected boolean validateSolve(List<T> leafList, V target) {
        if (leafList == null) {
            return false; // varTable does not contain this target, do nothing
        }

        for (T leaf : leafList) {
            if ( !leaf.isLeaf) {
                throw new RuntimeException("varTable should hold a leaf contains the target, while this TreeValue is not a leaf: " + leaf);
            }

            if ( !target.equals(leaf.leafValue)) {
                throw new RuntimeException("varTable hold " + target + " -> " + leaf + " mapping, but leaf doesn't contains target. leaf value:" + leaf.leafValue);
            }
        }

        return true;
    }

    public abstract void solveVar(Node target, Node expression);

    public void solve(V target, R reducedValue) {
        switch (this.type) {
            case TOP: return;
            case MERGE: {
                for (T treeValue : mergedSet) {
                    treeValue.solve(target, reducedValue);
                }
                return;
            }

            case VAR:
            case REDUCED: {
                List<T> leafList = root.varTable.get(target);
                if (!validateSolve(leafList, target)) {
                    return;
                }

                root.varTable.remove(target);
                for (T leaf : leafList) {
                    leaf.leafValue = reducedValue;
                    leaf.type = Type.REDUCED;
                }

                if (root.varTable.isEmpty()) {
                    this.reduce();
                }
            }
            default:
                assert false;
                return;
        }
        
    }

    public void solve(V target, V substitution) {
        switch (this.type) {
            case TOP: return;

            case MERGE: {
                for (T treeValue : mergedSet) {
                    treeValue.solve(target, substitution);
                }
                return;
            }

            case VAR:
            case REDUCED: {
                List<T> leafList = root.varTable.get(target);
                if (!validateSolve(leafList, target)) {
                    return;
                }
                root.varTable.remove(target);
                for (T leaf : leafList) {
                    leaf.type = Type.VAR;
                    leaf.leafValue = substitution;
                    putToVarTable(substitution, leaf);
                }
            }
            default:
                assert false;
        }
    }

    public void solve(V target, T subTree) {
        switch (this.type) {
            case TOP: return;

            case MERGE: {
                for (T treeValue : mergedSet) {
                    treeValue.solve(target, subTree);
                }
                return;
            }

            case VAR:
            case REDUCED: {
                List<T> leafList = root.varTable.get(target);
                if (!validateSolve(leafList, target)) {
                    return;
                }

                // merge subTree to this tree
                root.varTable.remove(target);

                subTree.root = this.root;

                if (subTree.varTable == null && subTree.type == Type.REDUCED) {
                    subTree.reduce();
                }

                //if subTree is a leaf, then we just copy it leafValue, otherwise, we jsut copy the left and right child of this subTree
                for (T leaf : leafList) {
                    if (subTree.isLeaf) {
                        leaf.type = subTree.type;
                        leaf.leafValue = subTree.leafValue;
                    } else {
                        leaf.type = subTree.type;
                        leaf.left = subTree.left;
                        leaf.right = subTree.right;
                        leaf.leafValue = null;
                        leaf.isLeaf = false;
                    }
                }

                if (subTree.varTable != null) {
                    root.varTable.putAll(subTree.varTable);
                    subTree.varTable = null;
                }
            }
            default:
                assert false;
        }
    }

    public abstract T copy();

    public abstract void reduce();

    public void putToVarTable(V target, T singleLeaf) {
        if (!root.varTable.containsKey(target)) {
            // Generally this would only put one value when initiate the set, and rarely would add more values to the same set
            List<T> leafList = new ArrayList<T>(1);
            root.varTable.put(target, leafList);
        }
        root.varTable.get(target).add(singleLeaf);
    }

    public void putToVarTable(V target, List<T> leafList) {
        if (root.varTable.containsKey(target)) {
            root.varTable.get(target).addAll(leafList);
        } else {
            root.varTable.put(target, leafList);
        }
    }

    public Type getType() {
        return type;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public Object getLeafValue() {
        return leafValue;
    }

    public Set<T> getMergedSet() {
        return mergedSet;
    }

    public T getLeft() {
        return left;
    }

    public void setLeft(T left) {
        this.left = left;
    }

    public T getRight() {
        return right;
    }

    public void setRight(T right) {
        this.right = right;
    }

    public static String prepareNodeSimpleName(String className) {
        return className.replace("class org.checkerframework.dataflow.cfg.node.", "");
    }
    @Override
    public String toString() {
        switch (type) {
            case TOP:
                return "TOP";
            case VAR: {
                if (isLeaf) {
                    Node nodeValue = (Node) leafValue;
                    if (nodeValue instanceof StringConversionNode) {
                        return nodeValue.toString();
                    } else {
                        return TreeValue.prepareNodeSimpleName(nodeValue.getClass().toString()) + "(" + nodeValue.toString() + ")";
                    }
                }
                // otherwise using same logic as reduced
            }
            case REDUCED: {
                if (isLeaf) {
                    return leafValue.toString();
                }
                StringBuilder sbBuilder = new StringBuilder();
                sbBuilder.append(left.toString() + " + " + right.toString());
                return sbBuilder.toString();
            }
            case MERGE: {
                StringBuilder sBuilder = new StringBuilder();
                sBuilder.append("[ ");
                for (T treeValue : mergedSet) {
                    sBuilder.append(treeValue.toString() + ", ");
                }
                sBuilder.append(" ]");
                return sBuilder.toString();
            }
            default:
                assert false;
                return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TreeValue<?, ?, ?>)) {
            return false;
        }

        TreeValue<?, ?, ?> other = (TreeValue<?, ?, ?>) obj;

        if (this.type != other.type) {
            return false;
        }

        switch(other.type) {
            case TOP: {
                return true;// TODO
            }
            case MERGE: {
                for (T treeValue : mergedSet) {
                    if (!other.mergedSet.contains(treeValue)) {
                        return false;
                    }
                }
                Iterator<?> iterator = other.mergedSet.iterator();
                if (iterator.hasNext()) {
                    if (!mergedSet.contains(iterator.next())) {
                        return false;
                    }
                }
                return true;
            }

            case REDUCED: {
                //lazy evaluation: first reduce this and other
                this.reduce();
                other.reduce();
                // go to the same block of VAR to judge
            }
            case VAR: {
                if (this.isLeaf != other.isLeaf) {
                    return false;
                }
                if (this.isLeaf) {
                    return this.leafValue.equals(other.leafValue);
                }
                return this.left.equals(other.left) && this.right.equals(other.right);
            }

            default:
                assert false;
                return true;
        }
    }

    // TODO: current is a VERY SIMPLE one, should have a better hashcode method
    @Override
    public int hashCode() {
        int hashcode;
        switch (this.type) {
            case TOP: {
                return super.hashCode(); // all TOP is same. TODO: add distinguishbility among TOPs
            }

            case MERGE: { //TODO: should merge type involve super.hashcode()?
                hashcode = super.hashCode();
                for (T treeValue : mergedSet) {
                    hashcode = hashcode ^ (treeValue.hashCode() >>> 16);
                }
                return hashcode;
            }

            case REDUCED: {
                if (this.isLeaf) {
                    hashcode = leafValue.hashCode();
                    return hashcode;
                }
                // if reduced is not leaf, then using same logic in var to process left child and right child of this tree
            }
            case VAR: {
                if (this.isLeaf) {
                    // hashcode = super.hashCode() ^ (leafValue.hashCode() >>> 16);
                    // currently I do not involve super.hashcode() (which means the instance equality of TreeValue), instead I using the leafValue to
                    // decide whether two leaf TreeValues are equal. This is a tradeoff to avoid infinite iteration when processing loop. But since two TreeValue
                    // that contains same leafValue are equal at this situation, the mapping of varTable should using V -> List<T> but not V -> Set<T>, since latter
                    // will only hold one reference when a concatenation contains same variable more than one time (e.g. str = var + var + "test";)
                    hashcode = leafValue.hashCode();
                    return hashcode;
                }
                hashcode = this.left.hashCode() >>> 16;
                hashcode = hashcode ^ (this.right.hashCode() >>> 16);
            }
            default:
                assert false;
                return 0;
        }
    }
}
