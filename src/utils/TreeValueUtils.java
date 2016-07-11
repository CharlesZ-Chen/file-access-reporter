package utils;

import java.util.List;

import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.javacutil.TypesUtils;

import analysis.value.PathValue;
import analysis.value.StrValue;
import analysis.value.TreeValue.Type;

public class TreeValueUtils {

    public static StrValue createStrValue(Node node) {
        if (node instanceof StringLiteralNode) {
            return new StrValue(Type.REDUCED, ((StringLiteralNode) node).getValue());
        }else if (node instanceof LocalVariableNode ||
                node instanceof FieldAccessNode) {
            return new StrValue(Type.VAR, node);
        } else if (node instanceof StringConcatenateNode) {
            StringConcatenateNode scNode = (StringConcatenateNode) node;
            StrValue left = createStrValue(scNode.getLeftOperand());
            StrValue right = createStrValue(scNode.getRightOperand());
            StrValue root = new StrValue(left, right);
            return root;
        } else {
            System.out.println("===========missing consideration of this: " + node.getClass());
            return new StrValue(Type.VAR, node);
        }
    }

    public static void mergeStrValue (StrValue target, StrValue source) {
        assert target.getType() ==  Type.MERGE;
        if (source.getType() == Type.TOP) {
            return;
        }

        if (source.getType() == Type.MERGE) {
            for (StrValue strValue : source.getMergedSet()) {
                StrValue copy = strValue.copy();
                target.getMergedSet().add(copy);
            }
        } else {
            StrValue copy = source.copy();
            target.getMergedSet().add(copy);
        }
    }

    public static PathValue createPathValue(List<Node> args) {
        switch (args.size()) {
            case 1: {
                TypeMirror argType = args.get(0).getType();

                if (TypesUtils.isDeclaredOfName(argType, "java.lang.String")) {
                    StrValue strValue = createStrValue(args.get(0));
                    return new PathValue(Type.REDUCED, strValue);
                } else if (TypesUtils.isDeclaredOfName(argType, "java.net.URI")) {
                    //TODO
                    return null;
                }
            }

            case 2: {
                TypeMirror firstArgType = args.get(0).getType();
                TypeMirror secondArgType = args.get(1).getType();
                Node arg1 = args.get(0);
                Node arg2 = args.get(1);

                if (TypesUtils.isDeclaredOfName(firstArgType, "java.io.File")) {
                    assert TypesUtils.isDeclaredOfName(secondArgType, "java.lang.String");

                    PathValue parDir;
                    if (arg1 instanceof ObjectCreationNode) {
                        // case: new File(new File(...), String)
                        parDir = createPathValue(((ObjectCreationNode) arg1).getArguments());
                    } else {
                        // case: new File(file, String)
                        parDir = new PathValue(Type.VAR, arg1);
                    }
                    PathValue childPath = new PathValue(Type.REDUCED, createStrValue(arg2));
                    return new PathValue(parDir, childPath);

                } else if (TypesUtils.isDeclaredOfName(firstArgType, "java.lang.String")) {
                    assert TypesUtils.isDeclaredOfName(secondArgType, "java.lang.String");
                    PathValue parDir = new PathValue(Type.REDUCED, createStrValue(arg1));
                    PathValue childPath = new PathValue(Type.REDUCED, createStrValue(arg2));
                    return new PathValue(parDir, childPath);
                }
            }

            default:
                assert false;
                return null;
        }
    }
}
