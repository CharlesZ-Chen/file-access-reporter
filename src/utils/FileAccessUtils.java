package utils;

import java.awt.dnd.MouseDragGestureRecognizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.javacutil.TypesUtils;

import analysis.value.FileStrPath;
import analysis.value.PathValue;
import analysis.value.StrValue;
import analysis.value.PathValue.Type;
import analysis.value.SingleStrPath;
import analysis.value.StrComPath;

public class FileAccessUtils {

    public static <K, V extends AbstractValue<V>>
        Map<K, V> merge(Map<K, V> leftMap, Map<K, V> rightMap) {
        Map<K, V> mergedMap = new HashMap<>();
        for (Entry<K, V> entry : leftMap.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (rightMap.containsKey(key)) {
                value = value.leastUpperBound(rightMap.get(key));
            }
            // TODO: is safe here using the original value?
            // provide a copy maybe better?
            mergedMap.put(key, value);
        }

        for (Entry<K, V> entry : rightMap.entrySet()) {
            mergedMap.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return mergedMap;
    }

    public static <E>
    Set<E> merge(Set<E> set1, Set<E> set2) {
        Set<E> mergedSet = new HashSet<>();
        mergedSet.addAll(set1);
        mergedSet.addAll(set2);
        return mergedSet;
    }
    public static PathValue createPathValue(List<Node> args) {
        PathValue.Type pathType = getPathType(args);

        switch (pathType) {
        case SINGLE_STR_PATH: {
            assert args.size() == 1;
            StrValue path = FileAccessUtils.createStrValue(args.get(0));
            return new SingleStrPath(path);
        }

        case FILE_STR_PATH: {
            assert args.size() == 2;
            assert TypesUtils.isDeclaredOfName(args.get(0).getType(), "java.io.File");

            LocalVariableNode parDir = (LocalVariableNode) args.get(0);
            StrValue childPath = FileAccessUtils.createStrValue(args.get(1));
            return new FileStrPath(parDir, childPath);
            
        }

        case STR_COM_PATH: {
            assert args.size() == 2;
            StrValue parDir = FileAccessUtils.createStrValue(args.get(0));
            StrValue childPath = FileAccessUtils.createStrValue(args.get(1));
            return new StrComPath(parDir, childPath);
        }

        case URI_PATH: {
            // TODO
            return null;
        }

        default:
            assert false;
            return null;
        }
    }

    public static PathValue.Type getPathType(List<Node> args) {
        switch (args.size()) {
        case 1: {
            TypeMirror argType = args.get(0).getType();

            if (TypesUtils.isDeclaredOfName(argType, "java.net.URI")) {
                return Type.URI_PATH;
            } else if (TypesUtils.isDeclaredOfName(argType, "java.lang.String")) {
                return Type.SINGLE_STR_PATH;
            }
            assert false;
            return null;
        }

        case 2: {
            TypeMirror firstArgType = args.get(0).getType();
            TypeMirror secondArgType = args.get(1).getType();

            if (TypesUtils.isDeclaredOfName(firstArgType, "java.io.File")) {
                assert TypesUtils.isDeclaredOfName(secondArgType, "java.lang.String");
                return Type.FILE_STR_PATH;
            } else if (TypesUtils.isDeclaredOfName(firstArgType, "java.lang.String")) {
                assert TypesUtils.isDeclaredOfName(secondArgType, "java.lang.String");
                return Type.STR_COM_PATH;
            }
            assert false;
            return null;
        }

        default:
            assert false;
            return null;
        }
    }

    public static StrValue createStrValue(Node strArg) {
        StrValue path = null;
        if (strArg instanceof StringLiteralNode) {
            path = new StrValue(StrValue.Type.LITERAL);
            path.addLiterals(((StringLiteralNode) strArg).getValue());

        } else if (strArg instanceof LocalVariableNode) {
            path = new StrValue(StrValue.Type.LOCAL_VAR);
            path.addVariables((LocalVariableNode) strArg);

        } else if (strArg instanceof StringConcatenateNode) {
            path = new StrValue(StrValue.Type.TOP, strArg);
        } else {
            assert false;
        }

        return path;
    }
}
