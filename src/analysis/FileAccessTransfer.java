package analysis;

import java.util.List;

import javax.lang.model.type.TypeMirror;

import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ObjectCreationNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.javacutil.TypesUtils;

import analysis.value.PathValue;
import utils.TreeValueUtils;

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
                PathValue pathValue = TreeValueUtils.createPathValue(args);
                store.trackVarInArgs(args);
                store.putToFileMap(n, pathValue);
                return new RegularTransferResult<>(pathValue, store);
            }
        }
        return (RegularTransferResult<PathValue, FileAccessStore>) super.visitReturn(n, p);
    }

    @Override
    public RegularTransferResult<PathValue, FileAccessStore> visitObjectCreation(ObjectCreationNode n,
            TransferInput<PathValue, FileAccessStore> p) {
        if (TypesUtils.isDeclaredOfName(n.getConstructor().getType(), "java.io.File")) {
            FileAccessStore store = p.getRegularStore();
            List<Node> args = n.getArguments();
            PathValue pathValue = TreeValueUtils.createPathValue(args);
            store.trackVarInArgs(args);
            store.putToFileMap(n, pathValue);
            return new RegularTransferResult<> (pathValue, store);
        }
        return (RegularTransferResult<PathValue, FileAccessStore>) super.visitObjectCreation(n, p);
    }

    @Override
    public RegularTransferResult<PathValue, FileAccessStore> visitAssignment(AssignmentNode n,
            TransferInput<PathValue, FileAccessStore> p) {
        Node target = n.getTarget();
        Node expression = n.getExpression();
        TypeMirror expressionType = expression.getType();

        //TODO: need consider about field
        if (TypesUtils.isDeclaredOfName(expressionType, "java.io.File")) {
            FileAccessStore store = p.getRegularStore();
            //TODO: extract out this block as a method?
            if (expression instanceof ObjectCreationNode) {
                ObjectCreationNode oNode = (ObjectCreationNode) expression;
                assert TypesUtils.isDeclaredOfName(oNode.getConstructor().getType(), "java.io.File");


                List<Node> args = oNode.getArguments();
                PathValue pathValue = TreeValueUtils.createPathValue(args);
                store.trackVarInArgs(args);
                store.putToFileMap(n.getTarget(), pathValue);

                if (store.isTrackingVar(target)) {
                    store.solveFileVar(target, pathValue);
                }
                return new RegularTransferResult<>(pathValue, store);

            } else {
                if (store.isTrackingVar(target)) {
                    store.solveFileVar(target, expression);
                }
                return new RegularTransferResult<>(null, store);
            }

            //TODO: need consider alias among File instances, e.g. File f = f2;
        }

        if (TypesUtils.isDeclaredOfName(expressionType, "java.lang.String")) {
            FileAccessStore store = p.getRegularStore();
            if (store.isTrackingVar(target)) {
                if (expression instanceof StringConcatenateNode) {
                    store.trackStrInConcatenation((StringConcatenateNode) expression);
                }
                store.solveStrVar(target,  expression);
                return new RegularTransferResult<>(null, store);
            }
        }

        return (RegularTransferResult<PathValue, FileAccessStore>) super.visitAssignment(n, p);
    }

}
