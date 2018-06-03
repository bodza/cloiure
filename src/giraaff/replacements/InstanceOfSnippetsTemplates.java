package giraaff.replacements;

import java.util.List;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.graph.Node;
import giraaff.nodes.ConditionAnchorNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ShortCircuitOrNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.util.Providers;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.UsageReplacer;

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
// @class InstanceOfSnippetsTemplates
public abstract class InstanceOfSnippetsTemplates extends AbstractTemplates
{
    // @cons
    public InstanceOfSnippetsTemplates(Providers __providers, SnippetReflectionProvider __snippetReflection, TargetDescription __target)
    {
        super(__providers, __snippetReflection, __target);
    }

    /**
     * Gets the arguments used to retrieve and instantiate an instanceof snippet template.
     */
    protected abstract Arguments makeArguments(InstanceOfUsageReplacer replacer, LoweringTool tool);

    public void lower(FloatingNode __instanceOf, LoweringTool __tool)
    {
        List<Node> __usages = __instanceOf.usages().snapshot();

        Instantiation __instantiation = new Instantiation();
        for (Node __usage : __usages)
        {
            final StructuredGraph __graph = (StructuredGraph) __usage.graph();

            InstanceOfUsageReplacer __replacer = createReplacer(__instanceOf, __instantiation, __usage, __graph);

            if (__instantiation.isInitialized())
            {
                // no need to re-instantiate the snippet - just re-use its result
                __replacer.replaceUsingInstantiation();
            }
            else
            {
                Arguments __args = makeArguments(__replacer, __tool);
                template(__instanceOf, __args).instantiate(providers.getMetaAccess(), __instanceOf, __replacer, __tool, __args);
            }
        }

        if (!__instanceOf.isDeleted())
        {
            GraphUtil.killWithUnusedFloatingInputs(__instanceOf);
        }
    }

    /**
     * Gets the specific replacer object used to replace the usage of an instanceof node with the
     * result of an instantiated instanceof snippet.
     */
    protected InstanceOfUsageReplacer createReplacer(FloatingNode __instanceOf, Instantiation __instantiation, Node __usage, final StructuredGraph __graph)
    {
        InstanceOfUsageReplacer __replacer;
        if (!canMaterialize(__usage))
        {
            ValueNode __trueValue = ConstantNode.forInt(1, __graph);
            ValueNode __falseValue = ConstantNode.forInt(0, __graph);
            if (__instantiation.isInitialized() && (__trueValue != __instantiation.trueValue || __falseValue != __instantiation.falseValue))
            {
                // This code doesn't really care what values are used so adopt the values from the previous instantiation.
                __trueValue = __instantiation.trueValue;
                __falseValue = __instantiation.falseValue;
            }
            __replacer = new NonMaterializationUsageReplacer(__instantiation, __trueValue, __falseValue, __instanceOf, __usage);
        }
        else
        {
            ConditionalNode __c = (ConditionalNode) __usage;
            __replacer = new MaterializationUsageReplacer(__instantiation, __c.trueValue(), __c.falseValue(), __instanceOf, __c);
        }
        return __replacer;
    }

    /**
     * Determines if an {@code instanceof} usage can be materialized.
     */
    protected boolean canMaterialize(Node __usage)
    {
        if (__usage instanceof ConditionalNode)
        {
            ConditionalNode __cn = (ConditionalNode) __usage;
            return __cn.trueValue().isConstant() && __cn.falseValue().isConstant();
        }
        if (__usage instanceof IfNode || __usage instanceof FixedGuardNode || __usage instanceof ShortCircuitOrNode || __usage instanceof ConditionAnchorNode)
        {
            return false;
        }
        return true;
    }

    /**
     * The result of instantiating an instanceof snippet. This enables a snippet instantiation to be
     * re-used which reduces compile time and produces better code.
     */
    // @class InstanceOfSnippetsTemplates.Instantiation
    public static final class Instantiation
    {
        // @field
        private ValueNode result;
        // @field
        private LogicNode condition;
        // @field
        private ValueNode trueValue;
        // @field
        private ValueNode falseValue;

        /**
         * Determines if the instantiation has occurred.
         */
        boolean isInitialized()
        {
            return result != null;
        }

        void initialize(ValueNode __r, ValueNode __t, ValueNode __f)
        {
            this.result = __r;
            this.trueValue = __t;
            this.falseValue = __f;
        }

        /**
         * Gets the result of this instantiation as a condition.
         *
         * @param testValue the returned condition is true if the result is equal to this value
         */
        LogicNode asCondition(ValueNode __testValue)
        {
            if (result.isConstant())
            {
                return LogicConstantNode.forBoolean(result.asConstant().equals(__testValue.asConstant()), result.graph());
            }
            if (condition == null || (!(condition instanceof CompareNode)) || ((CompareNode) condition).getY() != __testValue)
            {
                // re-use previously generated condition if the trueValue for the test is the same
                condition = CompareNode.createCompareNode(result.graph(), CanonicalCondition.EQ, result, __testValue, null, NodeView.DEFAULT);
            }
            return condition;
        }

        /**
         * Gets the result of the instantiation as a materialized value.
         *
         * @param t the true value for the materialization
         * @param f the false value for the materialization
         */
        ValueNode asMaterialization(StructuredGraph __graph, ValueNode __t, ValueNode __f)
        {
            if (__t == this.trueValue && __f == this.falseValue)
            {
                // Can simply use the phi result if the same materialized values are expected.
                return result;
            }
            else
            {
                return __graph.unique(new ConditionalNode(asCondition(trueValue), __t, __f));
            }
        }
    }

    /**
     * Replaces a usage of an {@link InstanceOfNode} or {@link InstanceOfDynamicNode}.
     */
    // @class InstanceOfSnippetsTemplates.InstanceOfUsageReplacer
    public abstract static class InstanceOfUsageReplacer implements UsageReplacer
    {
        // @field
        public final Instantiation instantiation;
        // @field
        public final FloatingNode instanceOf;
        // @field
        public final ValueNode trueValue;
        // @field
        public final ValueNode falseValue;

        // @cons
        public InstanceOfUsageReplacer(Instantiation __instantiation, FloatingNode __instanceOf, ValueNode __trueValue, ValueNode __falseValue)
        {
            super();
            this.instantiation = __instantiation;
            this.instanceOf = __instanceOf;
            this.trueValue = __trueValue;
            this.falseValue = __falseValue;
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
    // @class InstanceOfSnippetsTemplates.NonMaterializationUsageReplacer
    public static final class NonMaterializationUsageReplacer extends InstanceOfUsageReplacer
    {
        // @field
        private final Node usage;

        // @cons
        public NonMaterializationUsageReplacer(Instantiation __instantiation, ValueNode __trueValue, ValueNode __falseValue, FloatingNode __instanceOf, Node __usage)
        {
            super(__instantiation, __instanceOf, __trueValue, __falseValue);
            this.usage = __usage;
        }

        @Override
        public void replaceUsingInstantiation()
        {
            usage.replaceFirstInput(instanceOf, instantiation.asCondition(trueValue));
        }

        @Override
        public void replace(ValueNode __oldNode, ValueNode __newNode)
        {
            __newNode.inferStamp();
            instantiation.initialize(__newNode, trueValue, falseValue);
            usage.replaceFirstInput(__oldNode, instantiation.asCondition(trueValue));
        }
    }

    /**
     * Replaces the usage of an {@link InstanceOfNode} or {@link InstanceOfDynamicNode} that does
     * materializes the result of the type test.
     */
    // @class InstanceOfSnippetsTemplates.MaterializationUsageReplacer
    public static final class MaterializationUsageReplacer extends InstanceOfUsageReplacer
    {
        // @field
        public final ConditionalNode usage;

        // @cons
        public MaterializationUsageReplacer(Instantiation __instantiation, ValueNode __trueValue, ValueNode __falseValue, FloatingNode __instanceOf, ConditionalNode __usage)
        {
            super(__instantiation, __instanceOf, __trueValue, __falseValue);
            this.usage = __usage;
        }

        @Override
        public void replaceUsingInstantiation()
        {
            ValueNode __newValue = instantiation.asMaterialization(usage.graph(), trueValue, falseValue);
            usage.replaceAtUsages(__newValue);
            GraphUtil.killWithUnusedFloatingInputs(usage);
        }

        @Override
        public void replace(ValueNode __oldNode, ValueNode __newNode)
        {
            __newNode.inferStamp();
            instantiation.initialize(__newNode, trueValue, falseValue);
            usage.replaceAtUsages(__newNode);
            GraphUtil.killWithUnusedFloatingInputs(usage);
        }
    }
}
