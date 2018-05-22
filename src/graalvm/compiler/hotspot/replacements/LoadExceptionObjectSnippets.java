package graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.WordFactory;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions;
import graalvm.compiler.hotspot.word.HotSpotWordTypes;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.nodes.java.LoadExceptionObjectNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.replacements.nodes.ReadRegisterNode;
import graalvm.compiler.word.Word;

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
