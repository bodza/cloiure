package graalvm.compiler.truffle.compiler;

/**
 * Experimental.
 */
public interface TruffleStamp {

    TruffleStamp join(TruffleStamp p);

    TruffleStamp joinValue(Object value);

    boolean isCompatible(Object value);

    String toStringShort();

}
