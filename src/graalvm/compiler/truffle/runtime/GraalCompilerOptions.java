package graalvm.compiler.truffle.runtime;

import com.oracle.truffle.api.impl.DefaultCompilerOptions;

public class GraalCompilerOptions extends DefaultCompilerOptions {

    private int minTimeThreshold = 0;
    private int minInliningMaxCallerSize = 0;

    @Override
    public boolean supportsOption(String name) {
        switch (name) {
            case "MinTimeThreshold":
                return true;
            case "MinInliningMaxCallerSize":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setOption(String name, Object value) {
        switch (name) {
            case "MinTimeThreshold":
                minTimeThreshold = getInteger(name, value);
                break;
            case "MinInliningMaxCallerSize":
                minInliningMaxCallerSize = getInteger(name, value);
                break;
            default:
                super.setOption(name, value);
                break;
        }
    }

    private static int getInteger(String name, Object value) {
        if (value instanceof Integer) {
            return (int) value;
        } else {
            throw new UnsupportedOperationException(String.format("Option %s expected an int value", name));
        }
    }

    public int getMinTimeThreshold() {
        return minTimeThreshold;
    }

    public int getMinInliningMaxCallerSize() {
        return minInliningMaxCallerSize;
    }

}
