package giraaff.hotspot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.hotspot.HotSpotCompilationRequest;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.code.CompilationResult;
import giraaff.code.DataSection;

// @class HotSpotCompiledCodeBuilder
public final class HotSpotCompiledCodeBuilder
{
    // @cons HotSpotCompiledCodeBuilder
    private HotSpotCompiledCodeBuilder()
    {
        super();
    }

    public static HotSpotCompiledCode createCompiledCode(CodeCacheProvider __codeCache, ResolvedJavaMethod __method, HotSpotCompilationRequest __compRequest, CompilationResult __compResult)
    {
        byte[] __targetCode = __compResult.getTargetCode();
        int __targetCodeSize = __compResult.getTargetCodeSize();

        Site[] __sites = getSortedSites(__codeCache, __compResult);

        Assumption[] __assumptions = __compResult.getAssumptions();

        ResolvedJavaMethod[] __methods = __compResult.getMethods();

        Comment[] __comments = new Comment[0];

        DataSection __data = __compResult.getDataSection();
        byte[] __dataSection = new byte[__data.getSectionSize()];

        ByteBuffer __buffer = ByteBuffer.wrap(__dataSection).order(ByteOrder.nativeOrder());
        Builder<DataPatch> __patchBuilder = Stream.builder();
        __data.buildDataSection(__buffer, (__position, __vmConstant) ->
        {
            __patchBuilder.accept(new DataPatch(__position, new ConstantReference(__vmConstant)));
        });

        int __dataSectionAlignment = __data.getSectionAlignment();
        DataPatch[] __dataSectionPatches = __patchBuilder.build().toArray(__len -> new DataPatch[__len]);

        int __totalFrameSize = __compResult.getTotalFrameSize();

        if (__method instanceof HotSpotResolvedJavaMethod)
        {
            HotSpotResolvedJavaMethod __hsMethod = (HotSpotResolvedJavaMethod) __method;
            int __entryBCI = __compResult.getEntryBCI();
            boolean __hasUnsafeAccess = __compResult.hasUnsafeAccess();

            int __id;
            long __jvmciEnv;
            if (__compRequest != null)
            {
                __id = __compRequest.getId();
                __jvmciEnv = __compRequest.getJvmciEnv();
            }
            else
            {
                __id = __hsMethod.allocateCompileId(__entryBCI);
                __jvmciEnv = 0L;
            }
            return new HotSpotCompiledNmethod(null, __targetCode, __targetCodeSize, __sites, __assumptions, __methods, __comments, __dataSection, __dataSectionAlignment, __dataSectionPatches, false, __totalFrameSize, null, __hsMethod, __entryBCI, __id, __jvmciEnv, __hasUnsafeAccess);
        }
        else
        {
            return new HotSpotCompiledCode(null, __targetCode, __targetCodeSize, __sites, __assumptions, __methods, __comments, __dataSection, __dataSectionAlignment, __dataSectionPatches, false, __totalFrameSize, null);
        }
    }

    // @class HotSpotCompiledCodeBuilder.SiteComparator
    static final class SiteComparator implements Comparator<Site>
    {
        @Override
        public int compare(Site __s1, Site __s2)
        {
            if (__s1.pcOffset == __s2.pcOffset)
            {
                // Marks must come first since patching a call site
                // may need to know the mark denoting the call type
                // (see uses of CodeInstaller::_next_call_type).
                boolean __s1IsMark = __s1 instanceof Mark;
                boolean __s2IsMark = __s2 instanceof Mark;
                if (__s1IsMark != __s2IsMark)
                {
                    return __s1IsMark ? -1 : 1;
                }
            }
            return __s1.pcOffset - __s2.pcOffset;
        }
    }

    ///
    // HotSpot expects sites to be presented in ascending order of PC (see {@code DebugInformationRecorder::add_new_pc_offset}).
    ///
    private static Site[] getSortedSites(CodeCacheProvider __codeCache, CompilationResult __result)
    {
        List<Site> __sites = new ArrayList<>(__result.getExceptionHandlers().size() + __result.getDataPatches().size() + __result.getMarks().size());
        __sites.addAll(__result.getExceptionHandlers());
        __sites.addAll(__result.getDataPatches());
        __sites.addAll(__result.getMarks());

        Collections.sort(__sites, new HotSpotCompiledCodeBuilder.SiteComparator());
        return __sites.toArray(new Site[__sites.size()]);
    }
}
