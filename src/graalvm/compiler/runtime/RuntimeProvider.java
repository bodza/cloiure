package graalvm.compiler.runtime;

import jdk.vm.ci.code.Architecture;

import graalvm.compiler.core.target.Backend;

/**
 * A runtime supporting a host backend as well, zero or more additional backends.
 */
public interface RuntimeProvider {

    /**
     * Gets the host backend.
     */
    Backend getHostBackend();

    /**
     * Gets the backend for a given architecture.
     *
     * @param arch a specific architecture class
     */
    <T extends Architecture> Backend getBackend(Class<T> arch);
}
