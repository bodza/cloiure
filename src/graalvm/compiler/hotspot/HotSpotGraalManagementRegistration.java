package graalvm.compiler.hotspot;

/**
 * Communicates with an MBean providing a JMX interface to a {@link HotSpotGraalRuntime} instance.
 * The MBean will be dynamically created when a JMX client attaches or some other event causes the
 * platform MBean server to be started.
 */
public interface HotSpotGraalManagementRegistration
{
    /**
     * Completes the initialization of this registration by recording the
     * {@link HotSpotGraalRuntime} the MBean will provide an JMX interface to.
     */
    void initialize(HotSpotGraalRuntime runtime);

    /**
     * Polls this registration to see if the MBean is registered in a MBean server.
     *
     * @param sync synchronize with other threads that may be processing this registration. This is
     *            useful when the caller knows the server is active (e.g., it has a reference to
     *            server) and expects this poll to therefore return a non-null value.
     * @return an {@link javax.management.ObjectName} that can be used to access the MBean or
     *         {@code null} if the MBean has not been registered with an MBean server (e.g., no JMX
     *         client has attached to the VM)
     */
    Object poll(boolean sync);
}
