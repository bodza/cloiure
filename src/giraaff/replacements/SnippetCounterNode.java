package giraaff.replacements;

import java.util.Arrays;

import jdk.vm.ci.code.TargetDescription;

import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.Fold;
import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.type.StampFactory;
import giraaff.debug.GraalError;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.util.Providers;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.word.ObjectAccess;
import giraaff.util.UnsafeAccess;

/**
 * This node can be used to add a counter to the code that will estimate the dynamic number of calls
 * by adding an increment to the compiled code. This should of course only be used for
 * debugging/testing purposes.
 *
 * A unique counter will be created for each unique name passed to the constructor.
 * The name of the root method is added to the counter's name.
 */
public class SnippetCounterNode extends FixedWithNextNode implements Lowerable
{
    public static final NodeClass<SnippetCounterNode> TYPE = NodeClass.create(SnippetCounterNode.class);

    @Input protected ValueNode increment;

    protected final SnippetCounter counter;

    public SnippetCounterNode(SnippetCounter counter, ValueNode increment)
    {
        super(TYPE, StampFactory.forVoid());
        this.counter = counter;
        this.increment = increment;
    }

    public SnippetCounter getCounter()
    {
        return counter;
    }

    public ValueNode getIncrement()
    {
        return increment;
    }

    @NodeIntrinsic
    public static native void add(@ConstantNodeParameter SnippetCounter counter, int increment);

    public static void increment(@ConstantNodeParameter SnippetCounter counter)
    {
        add(counter, 1);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            SnippetCounterSnippets.Templates templates = tool.getReplacements().getSnippetTemplateCache(SnippetCounterSnippets.Templates.class);
            templates.lower(this, tool);
        }
    }

    /**
     * Add {@link #SNIPPET_COUNTER_LOCATION} to {@code privateLocations} if it isn't already there.
     *
     * @return a copy of privateLocations with any needed locations added
     */
    public static LocationIdentity[] addSnippetCounters(LocationIdentity[] privateLocations)
    {
        for (LocationIdentity location : privateLocations)
        {
            if (location.equals(SNIPPET_COUNTER_LOCATION))
            {
                return privateLocations;
            }
        }
        LocationIdentity[] result = Arrays.copyOf(privateLocations, privateLocations.length + 1);
        result[result.length - 1] = SnippetCounterNode.SNIPPET_COUNTER_LOCATION;
        return result;
    }

    /**
     * We do not want to use the {@link LocationIdentity} of the {@link SnippetCounter#value()}
     * field, so that the usage in snippets is always possible. If a method accesses the counter via
     * the field and the snippet, the result might not be correct though.
     */
    public static final LocationIdentity SNIPPET_COUNTER_LOCATION = NamedLocationIdentity.mutable("SnippetCounter");

    static class SnippetCounterSnippets implements Snippets
    {
        @Fold
        static int countOffset()
        {
            try
            {
                return (int) UnsafeAccess.UNSAFE.objectFieldOffset(SnippetCounter.class.getDeclaredField("value"));
            }
            catch (Exception e)
            {
                throw new GraalError(e);
            }
        }

        @Snippet
        public static void add(@ConstantParameter SnippetCounter counter, int increment)
        {
            long loadedValue = ObjectAccess.readLong(counter, countOffset(), SNIPPET_COUNTER_LOCATION);
            ObjectAccess.writeLong(counter, countOffset(), loadedValue + increment, SNIPPET_COUNTER_LOCATION);
        }

        public static class Templates extends AbstractTemplates
        {
            private final SnippetInfo add = snippet(SnippetCounterSnippets.class, "add", SNIPPET_COUNTER_LOCATION);

            Templates(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
            {
                super(options, providers, snippetReflection, target);
            }

            public void lower(SnippetCounterNode counter, LoweringTool tool)
            {
                StructuredGraph graph = counter.graph();
                Arguments args = new Arguments(add, graph.getGuardsStage(), tool.getLoweringStage());
                args.addConst("counter", counter.getCounter());
                args.add("increment", counter.getIncrement());

                template(counter, args).instantiate(providers.getMetaAccess(), counter, SnippetTemplate.DEFAULT_REPLACER, args);
            }
        }
    }
}
