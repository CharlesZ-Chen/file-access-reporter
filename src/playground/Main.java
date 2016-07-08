package playground;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.JavaSource2CFG;

import analysis.FileAccessStore;
import analysis.FileAccessTransfer;
import analysis.value.PathValue;

public class Main {

    public static void main(String[] args) {


        /* Configuration: change as appropriate */
        String inputFile = "/Users/charleszhuochen/Desktop/Test.java"; // input file name and path
        String outputDir = "/Users/charleszhuochen/Desktop/cfg"; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        Analysis<PathValue, FileAccessStore, FileAccessTransfer> analysis = new BackwardAnalysisImpl<>(new FileAccessTransfer());
//        ControlFlowGraph controlFlowGraph = JavaSource2CFG.generateMethodCFG(inputFile, clazz, method);
//        analysis.performAnalysis(controlFlowGraph);
        CFGVisualizeLauncher.generateDOTofCFG(inputFile, outputDir, method, clazz, true, analysis);
    }
}
