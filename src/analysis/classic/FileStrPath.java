package analysis.classic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.TypesUtils;

import utils.FileAccessUtils;

public class FileStrPath extends PathValue {

    private final Map<Node, StrValue> pathMap;

    public FileStrPath(Node parDir, StrValue childPath) {
        assert TypesUtils.isDeclaredOfName(parDir.getType(), "java.io.File");
        pathMap = new HashMap<>();
        pathMap.put(parDir, childPath);
    }

    protected FileStrPath(Map<Node, StrValue> otherPathMap) {
        pathMap = new HashMap<>();
        for (Entry<Node, StrValue> entry : otherPathMap.entrySet()) {
            pathMap.put(entry.getKey(), entry.getValue().copy());
        }
    }

    /**
     * @return immutable view of this.pathMap
     */
    public final Map<Node, StrValue> getPathMap() {
        return Collections.unmodifiableMap(pathMap);
    }

    @Override
    public FileStrPath leastUpperBound(PathValue other) {
        assert other instanceof FileStrPath;
        FileStrPath otherFileStrPath = (FileStrPath) other;
        Map<Node, StrValue> otherPathMap = otherFileStrPath.getPathMap();
        Map<Node, StrValue> newPathMap = FileAccessUtils.merge(pathMap, otherPathMap);
        return new FileStrPath(newPathMap);
    }

    @Override
    public void solveVar(Node var, Node expression) {
        for (StrValue childPath : pathMap.values()) {
            childPath.solve(var, expression);
        }
    }

    @Override
    public PathValue copy() {
        // TODO Auto-generated method stub
        return null;
    }
}
