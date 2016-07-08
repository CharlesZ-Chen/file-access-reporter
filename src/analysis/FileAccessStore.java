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
import org.checkerframework.javacutil.TypesUtils;

import analysis.value.PathValue;
import utils.FileAccessUtils;

public class FileAccessStore implements Store<FileAccessStore> {

    protected Map<Node, PathValue> filePathMap;
    protected Set<Node> strVarSet;

    public FileAccessStore() {
        filePathMap = new HashMap<>();
        strVarSet = new HashSet<>();
    }

    public FileAccessStore(Map<Node, PathValue> filePathMap,
            Set<Node> strVarSet) {
        this.filePathMap = new HashMap<>(filePathMap);
        this.strVarSet = new HashSet<>(strVarSet);
    }

    @Override
    public FileAccessStore copy() {
        Map<Node, PathValue> copyFilePathMap = new HashMap<>();
        for (Entry<Node, PathValue> entry : filePathMap.entrySet()) {
            copyFilePathMap.put(entry.getKey(), entry.getValue().copy());
        }
        return new FileAccessStore(copyFilePathMap, new HashSet<>(strVarSet));
    }

    @Override
    public FileAccessStore leastUpperBound(FileAccessStore other) {
        Map<Node, PathValue> newFilePathMap =
                FileAccessUtils.merge(filePathMap, other.filePathMap);
        Set<Node> newStrVarSet =
                FileAccessUtils.merge(strVarSet, other.strVarSet);
        return new FileAccessStore(newFilePathMap, newStrVarSet);
    }

    @Override
    public boolean canAlias(Receiver a, Receiver b) {
        return false;
    }

    @Override
    public void visualize(CFGVisualizer<?, FileAccessStore, ?> viz) {
        for (Entry<Node, PathValue> entry : filePathMap.entrySet()) {
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

            if (arg instanceof LocalVariableNode) {
                this.strVarSet.add(arg);
            }
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
        return strVarSet.contains(node);
    }

    public void solveStrVar(Node strVar, Node expression) {
        for (PathValue pathValue : filePathMap.values()) {
            pathValue.solveVar(strVar, expression);
        }
    }
}
