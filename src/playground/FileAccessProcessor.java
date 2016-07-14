package playground;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic.Kind;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.CFGVisualizer;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.DOTCFGVisualizer;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BasicTypeProcessor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;

import analysis.FileAccessStore;
import analysis.FileAccessTransfer;
import analysis.value.PathValue;

@SupportedAnnotationTypes("*")
public class FileAccessProcessor extends BasicTypeProcessor {

    protected CFGVisualizer<PathValue, FileAccessStore, FileAccessTransfer> cfgVisualizer;
    protected CompilationUnitTree rootTree;
    protected ClassTree currentClassTree;
    protected MethodTree currentMethodTree;
    protected StringBuilder finalReport;
    protected boolean verbose = false;

    public FileAccessProcessor() {
       super();
       Map<String, Object> args = new HashMap<>();
       args.put("outdir", "./analysis-report");
       args.put("checkerName", "");
       cfgVisualizer = new DOTCFGVisualizer<>();
       cfgVisualizer.init(args);
    }

    @Override
    public void typeProcessingOver() {
        cfgVisualizer.shutdown();
        if (this.finalReport == null) {
            initFinalReport();
            preFinalReport();
            finalReport.append("no file access found!");
        }
        System.out.println(finalReport.toString());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported();
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        rootTree = root;
        return new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void p) {
                currentClassTree = node;
                return super.visitClass(node, p);
            }

            @Override
            public Void visitMethod(MethodTree node, Void p) {
                currentMethodTree = node;
                ControlFlowGraph cfg = CFGBuilder.build(rootTree, processingEnv, currentMethodTree, currentClassTree);
                analysisCFG(cfg);
                return super.visitMethod(node, p);
            }
        };
    }

    protected void analysisCFG(ControlFlowGraph cfg) {
        BackwardAnalysis<PathValue, FileAccessStore, FileAccessTransfer> analysis = new BackwardAnalysisImpl<>(new FileAccessTransfer());
        UnderlyingAST ast = (CFGMethod) cfg.getUnderlyingAST();
        String cfgIdentifier;
        if (ast.getKind() == UnderlyingAST.Kind.METHOD) {
            CFGMethod cfgm = (CFGMethod) ast;
            cfgIdentifier = cfgm.getClassTree().getSimpleName().toString()+ "#" + cfgm.getMethod().getName().toString();
        } else {
            cfgIdentifier = "non-method";
        }

        analysis.performAnalysis(cfg);

        processingEnv.getMessager().printMessage(Kind.NOTE, "anlysis cfg: " + cfgIdentifier);

        buildReport(cfgIdentifier, ((FileAccessStore) analysis.getEntrySotre()).getFilePathMap());

        cfgVisualizer.visualize(cfg, cfg.getEntryBlock(), analysis);
    }

    protected void buildReport(String cfgIdentifier, Map<Node, PathValue> filePathMap) {
        if (filePathMap.isEmpty()) {
            return;
        }

        if (this.finalReport == null) {
            initFinalReport();
            preFinalReport();
        }

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("Analysis Result of " + cfgIdentifier + ":\n");
        for (Entry<Node, PathValue> entry : filePathMap.entrySet()) {
            entry.getValue().reduce(); // final reduce before out put
            if (verbose) {
                sBuilder.append("\t" + entry.getKey() + " = ");
            }
            sBuilder.append( entry.getValue() + "\n");
        }
        sBuilder.append("\n");
        this.finalReport.append(sBuilder);
    }

    protected void initFinalReport() {
        this.finalReport = new StringBuilder();
    }

    protected void preFinalReport() {
        finalReport.append("====================================\n");
        finalReport.append("    R E P O R T                     \n");
        finalReport.append("====================================\n");
    }
}
