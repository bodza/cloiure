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
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
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
import giraaff.debug.GraalError;

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
        StackSlot customStackArea = compResult.getCustomStackArea();

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
            return new HotSpotCompiledNmethod(name, targetCode, targetCodeSize, sites, assumptions, methods, comments, dataSection, dataSectionAlignment, dataSectionPatches, false, totalFrameSize, customStackArea, hsMethod, entryBCI, id, jvmciEnv, hasUnsafeAccess);
        }
        else
        {
            return new HotSpotCompiledCode(name, targetCode, targetCodeSize, sites, assumptions, methods, comments, dataSection, dataSectionAlignment, dataSectionPatches, false, totalFrameSize, customStackArea);
        }
    }

    static class SiteComparator implements Comparator<Site>
    {
        /**
         * Defines an order for sorting {@link Infopoint}s based on their
         * {@linkplain Infopoint#reason reasons}. This is used to choose which infopoint to preserve
         * when multiple infopoints collide on the same PC offset. A negative order value implies a
         * non-optional infopoint (i.e., must be preserved).
         */
        static final Map<InfopointReason, Integer> HOTSPOT_INFOPOINT_SORT_ORDER = new EnumMap<>(InfopointReason.class);

        static
        {
            HOTSPOT_INFOPOINT_SORT_ORDER.put(InfopointReason.SAFEPOINT, -4);
            HOTSPOT_INFOPOINT_SORT_ORDER.put(InfopointReason.CALL, -3);
            HOTSPOT_INFOPOINT_SORT_ORDER.put(InfopointReason.IMPLICIT_EXCEPTION, -2);
            HOTSPOT_INFOPOINT_SORT_ORDER.put(InfopointReason.METHOD_START, 2);
            HOTSPOT_INFOPOINT_SORT_ORDER.put(InfopointReason.METHOD_END, 3);
            HOTSPOT_INFOPOINT_SORT_ORDER.put(InfopointReason.BYTECODE_POSITION, 4);
        }

        static int ord(Infopoint info)
        {
            return HOTSPOT_INFOPOINT_SORT_ORDER.get(info.reason);
        }

        static int checkCollision(Infopoint i1, Infopoint i2)
        {
            int o1 = ord(i1);
            int o2 = ord(i2);
            if (o1 < 0 && o2 < 0)
            {
                throw new GraalError("Non optional infopoints cannot collide: %s and %s", i1, i2);
            }
            return o1 - o2;
        }

        /**
         * Records whether any two {@link Infopoint}s had the same {@link Infopoint#pcOffset}.
         */
        boolean sawCollidingInfopoints;

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

                // Infopoints must group together so put them after
                // other Site types.
                boolean s1IsInfopoint = s1 instanceof Infopoint;
                boolean s2IsInfopoint = s2 instanceof Infopoint;
                if (s1IsInfopoint != s2IsInfopoint)
                {
                    return s1IsInfopoint ? 1 : -1;
                }

                if (s1IsInfopoint)
                {
                    sawCollidingInfopoints = true;
                    return checkCollision((Infopoint) s1, (Infopoint) s2);
                }
            }
            return s1.pcOffset - s2.pcOffset;
        }
    }

    /**
     * HotSpot expects sites to be presented in ascending order of PC (see
     * {@code DebugInformationRecorder::add_new_pc_offset}). In addition, it expects
     * {@link Infopoint} PCs to be unique.
     */
    private static Site[] getSortedSites(CodeCacheProvider codeCache, CompilationResult target)
    {
        List<Site> sites = new ArrayList<>(target.getExceptionHandlers().size() + target.getInfopoints().size() + target.getDataPatches().size() + target.getMarks().size());
        sites.addAll(target.getExceptionHandlers());
        sites.addAll(target.getInfopoints());
        sites.addAll(target.getDataPatches());
        sites.addAll(target.getMarks());

        SiteComparator c = new SiteComparator();
        Collections.sort(sites, c);
        if (c.sawCollidingInfopoints)
        {
            Infopoint lastInfopoint = null;
            List<Site> copy = new ArrayList<>(sites.size());
            for (Site site : sites)
            {
                if (site instanceof Infopoint)
                {
                    Infopoint info = (Infopoint) site;
                    if (lastInfopoint == null || lastInfopoint.pcOffset != info.pcOffset)
                    {
                        lastInfopoint = info;
                        copy.add(info);
                    }
                    else
                    {
                        // Omit this colliding infopoint
                    }
                }
                else
                {
                    copy.add(site);
                }
            }
            sites = copy;
        }
        return sites.toArray(new Site[sites.size()]);
    }
}
