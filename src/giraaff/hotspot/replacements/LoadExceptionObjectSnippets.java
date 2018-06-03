package giraaff.hotspot.replacements;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.type.Stamp;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.HotSpotWordTypes;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.java.LoadExceptionObjectNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.ReadRegisterNode;
import giraaff.word.Word;

///
// Snippet for loading the exception object at the start of an exception dispatcher.
//
// The frame state upon entry to an exception handler is such that it is a
// {@link BytecodeFrame#rethrowException rethrow exception} state and the stack contains exactly the
// exception object (per the JVM spec) to rethrow. This means that the code generated for this node
// must not cause a deoptimization as the runtime/interpreter would not have a valid location to
// find the exception object to be rethrown.
///
// @class LoadExceptionObjectSnippets
public final class LoadExceptionObjectSnippets implements Snippets
{
    // @cons
    private LoadExceptionObjectSnippets()
    {
        super();
    }

    @Snippet
    public static Object loadException(@ConstantParameter Register __threadRegister)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        Object __exception = HotSpotReplacementsUtil.readExceptionOop(__thread);
        HotSpotReplacementsUtil.writeExceptionOop(__thread, null);
        HotSpotReplacementsUtil.writeExceptionPc(__thread, WordFactory.zero());
        return PiNode.piCastToSnippetReplaceeStamp(__exception);
    }

    // @class LoadExceptionObjectSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo ___loadException = snippet(LoadExceptionObjectSnippets.class, "loadException", HotSpotReplacementsUtil.EXCEPTION_OOP_LOCATION, HotSpotReplacementsUtil.EXCEPTION_PC_LOCATION);
        // @field
        private final HotSpotWordTypes ___wordTypes;

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
            this.___wordTypes = __providers.getWordTypes();
        }

        public void lower(LoadExceptionObjectNode __loadExceptionObject, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __loadExceptionObject.graph();
            if (GraalOptions.loadExceptionObjectInVM)
            {
                ResolvedJavaType __wordType = this.___providers.getMetaAccess().lookupJavaType(Word.class);
                Stamp __stamp = this.___wordTypes.getWordStamp(__wordType);
                ReadRegisterNode __thread = __graph.add(new ReadRegisterNode(__stamp, __registers.getThreadRegister(), true, false));
                __graph.addBeforeFixed(__loadExceptionObject, __thread);
                ForeignCallNode __loadExceptionC = __graph.add(new ForeignCallNode(this.___providers.getForeignCalls(), HotSpotForeignCallsProviderImpl.LOAD_AND_CLEAR_EXCEPTION, __thread));
                __loadExceptionC.setStateAfter(__loadExceptionObject.stateAfter());
                __graph.replaceFixedWithFixed(__loadExceptionObject, __loadExceptionC);
            }
            else
            {
                Arguments __args = new Arguments(this.___loadException, __loadExceptionObject.graph().getGuardsStage(), __tool.getLoweringStage());
                __args.addConst("threadRegister", __registers.getThreadRegister());
                template(__loadExceptionObject, __args).instantiate(this.___providers.getMetaAccess(), __loadExceptionObject, SnippetTemplate.DEFAULT_REPLACER, __args);
            }
        }
    }
}
