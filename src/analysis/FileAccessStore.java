package analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.javacutil.TypesUtils;

import analysis.classic.PathValue;
import analysis.value.StrValue;
import analysis.value.TreeValue;
import utils.FileAccessUtils;

public class FileAccessStore implements Store<FileAccessStore> {

    protected Map<Node, PathValue> filePathMap;
    protected Map<Node, StrValue> strMap;
    protected Set<Node> trackVarSet;

    public FileAccessStore() {
        filePathMap = new HashMap<>();
        trackVarSet = new HashSet<>();
        strMap = new HashMap<>();
    }

    public FileAccessStore(Map<Node, PathValue> filePathMap,
            Set<Node> strVarSet, Map<Node, StrValue> strMap) {
        this.filePathMap = new HashMap<>(filePathMap);
        this.trackVarSet = new HashSet<>(strVarSet);
        this.strMap = strMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FileAccessStore)) {
            return false;
        }

        FileAccessStore other = (FileAccessStore) obj;

        // first go through other
        for (Node trackVar : other.trackVarSet) {
            if (!trackVarSet.contains(trackVar)) {
                return false;
            }
        }

        for (Entry<Node, StrValue> entry : other.strMap.entrySet()) {
            StrValue value = strMap.get(entry.getKey());
            if (value == null || !value.equals(entry.getValue())) {
                return false;
            }
        }

        // next go through this compare with other
        for (Node trackVar : trackVarSet) {
            if (!other.trackVarSet.contains(trackVar)) {
                return false;
            }
        }

        for (Entry<Node, StrValue> entry : strMap.entrySet()) {
            StrValue value = other.strMap.get(entry.getKey());
            if (value == null || !value.equals(entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public FileAccessStore copy() {
        Map<Node, PathValue> copyFilePathMap = new HashMap<>();
        for (Entry<Node, PathValue> entry : filePathMap.entrySet()) {
            copyFilePathMap.put(entry.getKey(), entry.getValue().copy());
        }
        Map<Node, StrValue> copyStrMap = new HashMap<>();
        for (Entry<Node, StrValue> entry : strMap.entrySet()) {
            copyStrMap.put(entry.getKey(), entry.getValue().copy());
        }
        return new FileAccessStore(copyFilePathMap, new HashSet<>(trackVarSet), copyStrMap);
    }

    @Override
    public FileAccessStore leastUpperBound(FileAccessStore other) {
        Map<Node, PathValue> newFilePathMap =
                FileAccessUtils.merge(filePathMap, other.filePathMap);
        Set<Node> newStrVarSet =
                FileAccessUtils.merge(trackVarSet, other.trackVarSet);
        Map<Node, StrValue> newStrMap = FileAccessUtils.merge(strMap, other.strMap);
//        for (Entry<Node, StrValue> entry : newStrMap.entrySet()) {
//            System.out.println("lub: " + entry.getKey() + " -> " + entry.getValue() + " type: " + entry.getValue().getType() );
//        }
        return new FileAccessStore(newFilePathMap, newStrVarSet, newStrMap);
    }

    @Override
    public boolean canAlias(Receiver a, Receiver b) {
        return false;
    }

    @Override
    public void visualize(CFGVisualizer<?, FileAccessStore, ?> viz) {
//        for (Entry<Node, PathValue> entry : filePathMap.entrySet()) {
//            viz.visualizeStoreKeyVal(entry.getKey().toString(), entry.getValue());
//        }
        for (Entry<Node, StrValue> entry : strMap.entrySet()) {
//            System.out.println(entry.getKey().toString() + " : " + entry.getValue().toString());
            viz.visualizeStoreKeyVal(entry.getKey().toString(), entry.getValue());
        }
    }

    public void putToFileMap(Node node, PathValue value) {
        if (!(node instanceof ReturnNode) && !TypesUtils.isDeclaredOfName(node.getType(), "java.io.File")) {
            throw new RuntimeException("non java.io.File node unexpected. node type: " + node.getType());
        }
        this.filePathMap.put(node, value);
    }

    public void trackStrVarInArgs(List<Node> args) {
        for (Node arg : args) {
            if (!TypesUtils.isDeclaredOfName(arg.getType(), "java.lang.String")) {
                continue;
            }
            trackStrVarInNode(arg);
        }
    }

    public void trackStrVarInNode(Node node) {
        if (node instanceof StringConcatenateNode) {
            StringConcatenateNode scNode = (StringConcatenateNode) node;
            Node leftOpd = scNode.getLeftOperand();
            trackStrVarInNode(leftOpd);
            Node rightOpd = scNode.getRightOperand();
            trackStrVarInNode(rightOpd);
        } else if (node instanceof StringLiteralNode) {
            //don't track literal
        }
        else {
            System.out.println("track: " + node);
            this.trackVarSet.add(node);
            StrValue strValue = new StrValue(TreeValue.Type.VAR, node);
            this.strMap.put(node, strValue);
        }
    }
    /**
     * whether this store contains info about {@code ObjectCreationNode} node
     * @param node
     * @return
     */
    public boolean containsInFilePathMap(Node node) {
        return filePathMap.containsKey(node);
    }

    /**
     * whehter this store track String type var {@code node} 
     * @param node
     * @return
     */
    public boolean isTrackingStrVar(Node node) {
        return trackVarSet.contains(node);
    }

    public void solveStrVar(Node strVar, Node expression) {
        for (StrValue strValue : strMap.values()) {
            strValue.solveVar(strVar, expression);
        }
    }

    public void trackStrInConcatenation(StringConcatenateNode scNode) {
        Node leftOpd = scNode.getLeftOperand();
        Node rightOpd = scNode.getRightOperand();
        if (leftOpd instanceof StringConcatenateNode) {
            trackStrInConcatenation((StringConcatenateNode) leftOpd);
        } else if (! (leftOpd instanceof StringLiteralNode)) {
            trackVarSet.add(leftOpd);
        }

        if (rightOpd instanceof StringConcatenateNode) {
            trackStrInConcatenation((StringConcatenateNode) rightOpd);
        } else if (! (rightOpd instanceof StringLiteralNode)) {
            trackVarSet.add(rightOpd);
        }
    }
}
