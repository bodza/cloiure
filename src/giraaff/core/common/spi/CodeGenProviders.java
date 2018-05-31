package giraaff.core.common.spi;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

/**
 * A set of providers which are required for LIR and/or code generation. Some may not be present (i.e. null).
 */
// @iface CodeGenProviders
public interface CodeGenProviders
{
    MetaAccessProvider getMetaAccess();

    CodeCacheProvider getCodeCache();

    ForeignCallsProvider getForeignCalls();

    ConstantReflectionProvider getConstantReflection();

    ArrayOffsetProvider getArrayOffsetProvider();
}
