package analysis.classic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;

import analysis.FileAccessStore;

/**
 * TODO: currently not consider CONCATENATION
 * @author charleszhuochen
 *
 */
public class StrValue implements AbstractValue<StrValue> {

    public enum Type {
        TOP,
        COMPOSITION,
        LOCAL_VAR,
        LITERAL;
    }

    private Type type;

    /**
     * follow invariant should be hold:
     * topVar == null <=> literalVals != null && localVars != null
     * topVar != null <=> literalVals == null && localVars == null
     */
    final Node topVar;
    final Set<String> literalVals;
    final Set<Node> variables;

    public List<String> getValues() {
        List<String> values = new ArrayList<>();
        if (type == Type.TOP) {
            values.add("<var>" + topVar.toString() + "</var>");
            return values;
        }

        for (String literal : literalVals) {
            values.add(literal);
        }

        for (Node var : variables) {
            values.add("<var>" + var.toString() + "</var>");
        }
        return values;
    }

    public void solve(Node target, Node expression) {
        if (type == Type.TOP || !variables.contains(target)) {
            return;
        }

        if (expression instanceof StringLiteralNode) {
            StringLiteralNode literalNode = (StringLiteralNode) expression;
            literalVals.add(literalNode.getValue());
            variables.remove(target);
            if (variables.isEmpty()) {
                this.type = Type.LITERAL;
            }
            
            return;
        }

        variables.remove(target);
        variables.add(expression);
    }

    public StrValue(Type type, Node topVar) {
        this.type = type;
        switch (this.type) {
        case LITERAL:
        case COMPOSITION:
        case LOCAL_VAR: {
            this.topVar = null;
            this.literalVals = new HashSet<>();
            this.variables = new HashSet<>();
            break;
        }

        case TOP: {
            // TODO: think clear about initialization
            assert topVar != null : "initialize TOP StrValue expect a non-null topVar reference.";
            this.topVar = topVar;
            this.literalVals = null;
            this.variables = null;
            break;
        }

        default:
            assert false : "unexpected type: " + type;
            this.topVar = null;
            this.literalVals = null;
            this.variables = null;
        }

    }

    public StrValue(Type type) {
      this(type, null);
    }

    public final Type getType() {
        return this.type;
    }

    public Node getTopVar() {
        return topVar;
    }

    public Set<String> getLiteralVals() {
        return literalVals;
    }

    public Set<Node> getVariables() {
        return variables;
    }

    public void addLiterals(String... literals) {
        this.literalVals.addAll(Arrays.asList(literals));
    }

    public void addVariables(Node... variables) {
        this.variables.addAll(Arrays.asList(variables));
    }

    public void addLiterals(Set<String> literals) {
        this.literalVals.addAll(literals);
    }

    public void addVariables(Set<Node> variables) {
        this.variables.addAll(variables);
    }

    public StrValue copy() {
        switch (this.type) {
        case TOP: {
            assert variables == null && literalVals == null;
            return new StrValue(type, topVar);
        }

        case COMPOSITION: {
            StrValue copy = new StrValue(Type.COMPOSITION);
            copy.addLiterals(literalVals);
            copy.addVariables(variables);
            return copy;
        }

        case LOCAL_VAR: {
            assert this.literalVals.isEmpty();
            StrValue copy = new StrValue(Type.LOCAL_VAR);
            copy.addVariables(variables);
            return copy;
        }

        case LITERAL: {
            assert this.variables.isEmpty();
            StrValue copy = new StrValue(Type.LITERAL);
            copy.addLiterals(literalVals);
            return copy;
        }

        default:
            assert false;
            return null;
        }
    }

    @Override
    public StrValue leastUpperBound(StrValue other) {
       if (this.type == Type.TOP || other.getType() == Type.TOP) {
           // TODO: is this.topVar always equal to other.topVar?
           assert this.topVar != null && this.topVar.equals(other.getTopVar());
           return new StrValue(Type.TOP, this.topVar);
       }

       assert this.literalVals != null && this.variables != null;
       assert other.getLiteralVals() != null && other.getVariables() != null;

       StrValue lubValue = null;
       if (literalVals.isEmpty() && other.getLiteralVals().isEmpty()) {
           lubValue = new StrValue(Type.LOCAL_VAR);
       } else if (variables.isEmpty() && other.getVariables().isEmpty()) {
           lubValue = new StrValue(Type.LITERAL);
       } else {
           lubValue = new StrValue(Type.COMPOSITION);
       }

       lubValue.addLiterals(literalVals);
       lubValue.addLiterals(other.getLiteralVals());
       lubValue.addVariables(variables);
       lubValue.addVariables(other.getVariables());
       return lubValue;
    }
}
