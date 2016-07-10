package analysis.classic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.checkerframework.dataflow.cfg.node.Node;

import utils.FileAccessUtils;

public class StrComPath extends PathValue {

    /**
     * key: partent dir, value: child path
     */
    private final Map<StrValue, StrValue> pathMap;

    public StrComPath(StrValue parDir, StrValue childPath) {
        this.pathMap = new HashMap<>();
        this.pathMap.put(parDir, childPath);
    }

    public StrComPath(Map<StrValue, StrValue> pathMap) {
        this.pathMap = pathMap;
    }

    /**
     * @return immutable view of this.pathMap
     */
    public Map<StrValue, StrValue> getPathMap() {
        return Collections.unmodifiableMap(pathMap);
    }

    @Override
    public StrComPath leastUpperBound(PathValue other) {
        StrComPath otherStrComPath = (StrComPath) other;
        Map<StrValue, StrValue> otherPathmap = otherStrComPath.getPathMap();

        Map<StrValue, StrValue> newPathMap = FileAccessUtils.merge(pathMap, otherPathmap);
        return new StrComPath(newPathMap);
    }

    @Override
    public void solveVar(Node var, Node expression) {
        for (Entry<StrValue, StrValue> entry : pathMap.entrySet()) {
            entry.getKey().solve(var, expression);
            entry.getValue().solve(var, expression);
        }
        
    }

    @Override
    public PathValue copy() {
        // TODO Auto-generated method stub
        return null;
    }
}
