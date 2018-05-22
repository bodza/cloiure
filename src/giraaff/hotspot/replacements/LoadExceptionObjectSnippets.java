package giraaff.hotspot.replacements;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.type.Stamp;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.HotspotSnippetsOptions;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.java.LoadExceptionObjectNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.ReadRegisterNode;
import giraaff.word.Word;

/**
 * Snippet for loading the exception object at the start of an exception dispatcher.
 *
 * The frame state upon entry to an exception handler is such that it is a
 * {@link BytecodeFrame#rethrowException rethrow exception} state and the stack contains exactly the
 * exception object (per the JVM spec) to rethrow. This means that the code generated for this node
 * must not cause a deoptimization as the runtime/interpreter would not have a valid location to
 * find the exception object to be rethrown.
 */
public class LoadExceptionObjectSnippets implements Snippets
{
    @Snippet
    public static Object loadException(@ConstantParameter Register threadRegister)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Object exception = HotSpotReplacementsUtil.readExceptionOop(thread);
        HotSpotReplacementsUtil.writeExceptionOop(thread, null);
        HotSpotReplacementsUtil.writeExceptionPc(thread, WordFactory.zero());
        return PiNode.piCastToSnippetReplaceeStamp(exception);
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo loadException = snippet(LoadExceptionObjectSnippets.class, "loadException", HotSpotReplacementsUtil.EXCEPTION_OOP_LOCATION, HotSpotReplacementsUtil.EXCEPTION_PC_LOCATION);
        private final HotSpotWordTypes wordTypes;

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            this.wordTypes = providers.getWordTypes();
        }

        public void lower(LoadExceptionObjectNode loadExceptionObject, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = loadExceptionObject.graph();
            if (HotspotSnippetsOptions.LoadExceptionObjectInVM.getValue(graph.getOptions()))
            {
                ResolvedJavaType wordType = providers.getMetaAccess().lookupJavaType(Word.class);
                Stamp stamp = wordTypes.getWordStamp(wordType);
                ReadRegisterNode thread = graph.add(new ReadRegisterNode(stamp, registers.getThreadRegister(), true, false));
                graph.addBeforeFixed(loadExceptionObject, thread);
                ForeignCallNode loadExceptionC = graph.add(new ForeignCallNode(providers.getForeignCalls(), HotSpotForeignCallsProviderImpl.LOAD_AND_CLEAR_EXCEPTION, thread));
                loadExceptionC.setStateAfter(loadExceptionObject.stateAfter());
                graph.replaceFixedWithFixed(loadExceptionObject, loadExceptionC);
            }
            else
            {
                Arguments args = new Arguments(loadException, loadExceptionObject.graph().getGuardsStage(), tool.getLoweringStage());
                args.addConst("threadRegister", registers.getThreadRegister());
                template(loadExceptionObject, args).instantiate(providers.getMetaAccess(), loadExceptionObject, SnippetTemplate.DEFAULT_REPLACER, args);
            }
        }
    }
}
