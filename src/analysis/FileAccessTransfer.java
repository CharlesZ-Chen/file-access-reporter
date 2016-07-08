package analysis;

import java.io.File;
import java.util.List;

import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.javacutil.TypesUtils;

import analysis.value.PathValue;
import utils.FileAccessUtils;

public class FileAccessTransfer
    extends
    AbstractNodeVisitor<TransferResult<PathValue, FileAccessStore>, TransferInput<PathValue, FileAccessStore>>
    implements BackwardTransferFunction<PathValue, FileAccessStore> {

    @Override
    public FileAccessStore initialNormalExitStore(UnderlyingAST underlyingAST, List<ReturnNode> returnNodes) {
        return new FileAccessStore();
    }

    @Override
    public FileAccessStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
        return new FileAccessStore();
    }

    @Override
    public RegularTransferResult<PathValue, FileAccessStore> visitNode(Node n, TransferInput<PathValue, FileAccessStore> p) {
        return new RegularTransferResult<PathValue, FileAccessStore>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<PathValue, FileAccessStore> visitReturn(ReturnNode n, TransferInput<PathValue, FileAccessStore> p) {
        Node result = n.getResult();
        if (result instanceof ObjectCreationNode && TypesUtils.isDeclaredOfName(result.getType(), "java.io.File")) {
            FileAccessStore store = p.getRegularStore();
            if (!store.containsInFilePathMap(n)) {
                List<Node> args = ((ObjectCreationNode) result).getArguments();
                PathValue pathValue = FileAccessUtils.createPathValue(args);
                store.trackStrVarInArgs(args);
                store.putToFileMap(n, pathValue);
                return new RegularTransferResult<>(pathValue, store);
            }
        }
        return (RegularTransferResult<PathValue, FileAccessStore>) super.visitReturn(n, p);
    }

    @Override
    public RegularTransferResult<PathValue, FileAccessStore> visitAssignment(AssignmentNode n,
            TransferInput<PathValue, FileAccessStore> p) {
//        System.out.println("assignment left: " + n.getTarget() + "\tclass " + n.getTarget().getClass());
        Node target = n.getTarget();
        Node expression = n.getExpression();
        TypeMirror expressionType = expression.getType();

        //TODO: need consider about field
        if (target instanceof LocalVariableNode && TypesUtils.isDeclaredOfName(expressionType, "java.io.File")) {
            //TODO: extract out this block as a method?
            if (expression instanceof ObjectCreationNode) {
                ObjectCreationNode oNode = (ObjectCreationNode) expression;
                assert TypesUtils.isDeclaredOfName(oNode.getConstructor().getType(), "java.io.File");

                FileAccessStore store = p.getRegularStore();
                if (!store.containsInFilePathMap(target)) {
                    List<Node> args = oNode.getArguments();
                    PathValue pathValue = FileAccessUtils.createPathValue(args);
                    store.trackStrVarInArgs(args);
                    store.putToFileMap(n.getTarget(), pathValue);
                    return new RegularTransferResult<>(pathValue, store);
                }
            }

            //TODO: need consider alias among File instances, e.g. File f = f2;
        }

        if (TypesUtils.isDeclaredOfName(expressionType, "java.lang.String")) {
            FileAccessStore store = p.getRegularStore();
            if (store.isTrackingStrVar(target)) {
                System.out.println("solve :" + expression);
                store.solveStrVar(target,  expression);
                return new RegularTransferResult<>(null, store);
            }
        }

        return (RegularTransferResult<PathValue, FileAccessStore>) super.visitAssignment(n, p);
    }

}
