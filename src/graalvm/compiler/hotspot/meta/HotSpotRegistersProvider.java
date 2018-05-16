package graalvm.compiler.hotspot.meta;

import jdk.vm.ci.code.Register;

/**
 * Special registers reserved by HotSpot for frequently used values.
 */
public interface HotSpotRegistersProvider
{
    /**
     * Gets the register holding the current thread.
     */
    Register getThreadRegister();

    /**
     * Gets the register holding the heap base address for compressed pointers.
     */
    Register getHeapBaseRegister();

    /**
     * Gets the stack pointer register.
     */
    Register getStackPointerRegister();
}
