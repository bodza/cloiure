package graalvm.compiler.replacements;

import static graalvm.compiler.nodes.calc.CompareNode.createCompareNode;

import java.util.List;

import graalvm.compiler.api.replacements.SnippetReflectionProvider;
import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.ConditionAnchorNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ShortCircuitOrNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.CompareNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.java.InstanceOfDynamicNode;
import graalvm.compiler.nodes.java.InstanceOfNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.util.Providers;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.UsageReplacer;

import jdk.vm.ci.code.TargetDescription;

/**
 * Helper class for lowering {@link InstanceOfNode}s with snippets. The majority of the complexity
 * in such a lowering derives from the fact that {@link InstanceOfNode} is a floating node. A
 * snippet used to lower an {@link InstanceOfNode} will almost always incorporate control flow and
 * replacing a floating node with control flow is not trivial.
 *
 * The mechanism implemented in this class ensures that the graph for an instanceof snippet is
 * instantiated once per {@link InstanceOfNode} being lowered. The result produced is then re-used
 * by all usages of the node. Additionally, if there is a single usage that is an {@link IfNode},
 * the control flow in the snippet is connected directly to the true and false successors of the
 * {@link IfNode}. This avoids materializing the instanceof test as a boolean which is then retested
 * by the {@link IfNode}.
 */
public abstract class InstanceOfSnippetsTemplates extends AbstractTemplates
{
    public InstanceOfSnippetsTemplates(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
    {
        super(options, providers, snippetReflection, target);
    }

    /**
     * Gets the arguments used to retrieve and instantiate an instanceof snippet template.
     */
    protected abstract Arguments makeArguments(InstanceOfUsageReplacer replacer, LoweringTool tool);

    public void lower(FloatingNode instanceOf, LoweringTool tool)
    {
        List<Node> usages = instanceOf.usages().snapshot();

        Instantiation instantiation = new Instantiation();
        for (Node usage : usages)
        {
            final StructuredGraph graph = (StructuredGraph) usage.graph();

            InstanceOfUsageReplacer replacer = createReplacer(instanceOf, instantiation, usage, graph);

            if (instantiation.isInitialized())
            {
                // No need to re-instantiate the snippet - just re-use its result
                replacer.replaceUsingInstantiation();
            }
            else
            {
                Arguments args = makeArguments(replacer, tool);
                template(instanceOf, args).instantiate(providers.getMetaAccess(), instanceOf, replacer, tool, args);
            }
        }

        if (!instanceOf.isDeleted())
        {
            GraphUtil.killWithUnusedFloatingInputs(instanceOf);
        }
    }

    /**
     * Gets the specific replacer object used to replace the usage of an instanceof node with the
     * result of an instantiated instanceof snippet.
     */
    protected InstanceOfUsageReplacer createReplacer(FloatingNode instanceOf, Instantiation instantiation, Node usage, final StructuredGraph graph)
    {
        InstanceOfUsageReplacer replacer;
        if (!canMaterialize(usage))
        {
            ValueNode trueValue = ConstantNode.forInt(1, graph);
            ValueNode falseValue = ConstantNode.forInt(0, graph);
            if (instantiation.isInitialized() && (trueValue != instantiation.trueValue || falseValue != instantiation.falseValue))
            {
                /*
                 * This code doesn't really care what values are used so adopt the values from the
                 * previous instantiation.
                 */
                trueValue = instantiation.trueValue;
                falseValue = instantiation.falseValue;
            }
            replacer = new NonMaterializationUsageReplacer(instantiation, trueValue, falseValue, instanceOf, usage);
        }
        else
        {
            ConditionalNode c = (ConditionalNode) usage;
            replacer = new MaterializationUsageReplacer(instantiation, c.trueValue(), c.falseValue(), instanceOf, c);
        }
        return replacer;
    }

    /**
     * Determines if an {@code instanceof} usage can be materialized.
     */
    protected boolean canMaterialize(Node usage)
    {
        if (usage instanceof ConditionalNode)
        {
            ConditionalNode cn = (ConditionalNode) usage;
            return cn.trueValue().isConstant() && cn.falseValue().isConstant();
        }
        if (usage instanceof IfNode || usage instanceof FixedGuardNode || usage instanceof ShortCircuitOrNode || usage instanceof ConditionAnchorNode)
        {
            return false;
        }
        return true;
    }

    /**
     * The result of instantiating an instanceof snippet. This enables a snippet instantiation to be
     * re-used which reduces compile time and produces better code.
     */
    public static final class Instantiation
    {
        private ValueNode result;
        private LogicNode condition;
        private ValueNode trueValue;
        private ValueNode falseValue;

        /**
         * Determines if the instantiation has occurred.
         */
        boolean isInitialized()
        {
            return result != null;
        }

        void initialize(ValueNode r, ValueNode t, ValueNode f)
        {
            this.result = r;
            this.trueValue = t;
            this.falseValue = f;
        }

        /**
         * Gets the result of this instantiation as a condition.
         *
         * @param testValue the returned condition is true if the result is equal to this value
         */
        LogicNode asCondition(ValueNode testValue)
        {
            if (result.isConstant())
            {
                return LogicConstantNode.forBoolean(result.asConstant().equals(testValue.asConstant()), result.graph());
            }
            if (condition == null || (!(condition instanceof CompareNode)) || ((CompareNode) condition).getY() != testValue)
            {
                // Re-use previously generated condition if the trueValue for the test is the same
                condition = createCompareNode(result.graph(), CanonicalCondition.EQ, result, testValue, null, NodeView.DEFAULT);
            }
            return condition;
        }

        /**
         * Gets the result of the instantiation as a materialized value.
         *
         * @param t the true value for the materialization
         * @param f the false value for the materialization
         */
        ValueNode asMaterialization(StructuredGraph graph, ValueNode t, ValueNode f)
        {
            if (t == this.trueValue && f == this.falseValue)
            {
                // Can simply use the phi result if the same materialized values are expected.
                return result;
            }
            else
            {
                return graph.unique(new ConditionalNode(asCondition(trueValue), t, f));
            }
        }
    }

    /**
     * Replaces a usage of an {@link InstanceOfNode} or {@link InstanceOfDynamicNode}.
     */
    public abstract static class InstanceOfUsageReplacer implements UsageReplacer
    {
        public final Instantiation instantiation;
        public final FloatingNode instanceOf;
        public final ValueNode trueValue;
        public final ValueNode falseValue;

        public InstanceOfUsageReplacer(Instantiation instantiation, FloatingNode instanceOf, ValueNode trueValue, ValueNode falseValue)
        {
            this.instantiation = instantiation;
            this.instanceOf = instanceOf;
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }

        /**
         * Does the replacement based on a previously snippet instantiation.
         */
        public abstract void replaceUsingInstantiation();
    }

    /**
     * Replaces the usage of an {@link InstanceOfNode} or {@link InstanceOfDynamicNode} that does
     * not materialize the result of the type test.
     */
    public static class NonMaterializationUsageReplacer extends InstanceOfUsageReplacer
    {
        private final Node usage;

        public NonMaterializationUsageReplacer(Instantiation instantiation, ValueNode trueValue, ValueNode falseValue, FloatingNode instanceOf, Node usage)
        {
            super(instantiation, instanceOf, trueValue, falseValue);
            this.usage = usage;
        }

        @Override
        public void replaceUsingInstantiation()
        {
            usage.replaceFirstInput(instanceOf, instantiation.asCondition(trueValue));
        }

        @Override
        public void replace(ValueNode oldNode, ValueNode newNode)
        {
            newNode.inferStamp();
            instantiation.initialize(newNode, trueValue, falseValue);
            usage.replaceFirstInput(oldNode, instantiation.asCondition(trueValue));
        }
    }

    /**
     * Replaces the usage of an {@link InstanceOfNode} or {@link InstanceOfDynamicNode} that does
     * materializes the result of the type test.
     */
    public static class MaterializationUsageReplacer extends InstanceOfUsageReplacer
    {
        public final ConditionalNode usage;

        public MaterializationUsageReplacer(Instantiation instantiation, ValueNode trueValue, ValueNode falseValue, FloatingNode instanceOf, ConditionalNode usage)
        {
            super(instantiation, instanceOf, trueValue, falseValue);
            this.usage = usage;
        }

        @Override
        public void replaceUsingInstantiation()
        {
            ValueNode newValue = instantiation.asMaterialization(usage.graph(), trueValue, falseValue);
            usage.replaceAtUsages(newValue);
            GraphUtil.killWithUnusedFloatingInputs(usage);
        }

        @Override
        public void replace(ValueNode oldNode, ValueNode newNode)
        {
            newNode.inferStamp();
            instantiation.initialize(newNode, trueValue, falseValue);
            usage.replaceAtUsages(newNode);
            GraphUtil.killWithUnusedFloatingInputs(usage);
        }
    }
}
