package giraaff.hotspot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import giraaff.util.GraalError;

public class HotSpotCompiledCodeBuilder
{
    public static HotSpotCompiledCode createCompiledCode(CodeCacheProvider codeCache, ResolvedJavaMethod method, HotSpotCompilationRequest compRequest, CompilationResult compResult)
    {
        String name = compResult.getName();

        byte[] targetCode = compResult.getTargetCode();
        int targetCodeSize = compResult.getTargetCodeSize();

        Site[] sites = getSortedSites(codeCache, compResult);

        Assumption[] assumptions = compResult.getAssumptions();

        ResolvedJavaMethod[] methods = compResult.getMethods();

        Comment[] comments = new Comment[0];

        DataSection data = compResult.getDataSection();
        byte[] dataSection = new byte[data.getSectionSize()];

        ByteBuffer buffer = ByteBuffer.wrap(dataSection).order(ByteOrder.nativeOrder());
        Builder<DataPatch> patchBuilder = Stream.builder();
        data.buildDataSection(buffer, (position, vmConstant) ->
        {
            patchBuilder.accept(new DataPatch(position, new ConstantReference(vmConstant)));
        });

        int dataSectionAlignment = data.getSectionAlignment();
        DataPatch[] dataSectionPatches = patchBuilder.build().toArray(len -> new DataPatch[len]);

        int totalFrameSize = compResult.getTotalFrameSize();

        if (method instanceof HotSpotResolvedJavaMethod)
        {
            HotSpotResolvedJavaMethod hsMethod = (HotSpotResolvedJavaMethod) method;
            int entryBCI = compResult.getEntryBCI();
            boolean hasUnsafeAccess = compResult.hasUnsafeAccess();

            int id;
            long jvmciEnv;
            if (compRequest != null)
            {
                id = compRequest.getId();
                jvmciEnv = compRequest.getJvmciEnv();
            }
            else
            {
                id = hsMethod.allocateCompileId(entryBCI);
                jvmciEnv = 0L;
            }
            return new HotSpotCompiledNmethod(name, targetCode, targetCodeSize, sites, assumptions, methods, comments, dataSection, dataSectionAlignment, dataSectionPatches, false, totalFrameSize, null, hsMethod, entryBCI, id, jvmciEnv, hasUnsafeAccess);
        }
        else
        {
            return new HotSpotCompiledCode(name, targetCode, targetCodeSize, sites, assumptions, methods, comments, dataSection, dataSectionAlignment, dataSectionPatches, false, totalFrameSize, null);
        }
    }

    static class SiteComparator implements Comparator<Site>
    {
        @Override
        public int compare(Site s1, Site s2)
        {
            if (s1.pcOffset == s2.pcOffset)
            {
                // Marks must come first since patching a call site
                // may need to know the mark denoting the call type
                // (see uses of CodeInstaller::_next_call_type).
                boolean s1IsMark = s1 instanceof Mark;
                boolean s2IsMark = s2 instanceof Mark;
                if (s1IsMark != s2IsMark)
                {
                    return s1IsMark ? -1 : 1;
                }
            }
            return s1.pcOffset - s2.pcOffset;
        }
    }

    /**
     * HotSpot expects sites to be presented in ascending order of PC (see {@code DebugInformationRecorder::add_new_pc_offset}).
     */
    private static Site[] getSortedSites(CodeCacheProvider codeCache, CompilationResult target)
    {
        List<Site> sites = new ArrayList<>(target.getExceptionHandlers().size() + target.getDataPatches().size() + target.getMarks().size());
        sites.addAll(target.getExceptionHandlers());
        sites.addAll(target.getDataPatches());
        sites.addAll(target.getMarks());

        Collections.sort(sites, new SiteComparator());
        return sites.toArray(new Site[sites.size()]);
    }
}
