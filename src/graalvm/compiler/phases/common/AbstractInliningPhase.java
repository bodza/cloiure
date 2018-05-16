package graalvm.compiler.phases.common;

import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.tiers.HighTierContext;

/**
 * Common superclass for phases that perform inlining.
 */
public abstract class AbstractInliningPhase extends BasePhase<HighTierContext>
{
}
