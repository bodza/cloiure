package graalvm.compiler.graph;

import static graalvm.compiler.graph.NodeSourcePosition.Marker.None;
import static graalvm.compiler.graph.NodeSourcePosition.Marker.Placeholder;
import static graalvm.compiler.graph.NodeSourcePosition.Marker.Substitution;

import java.util.Objects;

import graalvm.compiler.bytecode.BytecodeDisassembler;
import graalvm.compiler.bytecode.Bytecodes;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.BytecodePosition;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.MetaUtil;
import jdk.vm.ci.meta.ResolvedJavaMethod;

public class NodeSourcePosition extends BytecodePosition
{
    private final int hashCode;
    private final Marker marker;
    private final SourceLanguagePosition sourceLanguagePosition;

    /**
     * Remove marker frames.
     */
    public NodeSourcePosition trim()
    {
        if (marker != None)
        {
            return null;
        }
        NodeSourcePosition caller = getCaller();
        if (caller != null)
        {
            caller = caller.trim();
        }
        if (caller != getCaller())
        {
            return new NodeSourcePosition(caller, getMethod(), getBCI());
        }
        return this;
    }

    public ResolvedJavaMethod getRootMethod()
    {
        NodeSourcePosition cur = this;
        while (cur.getCaller() != null)
        {
            cur = cur.getCaller();
        }
        return cur.getMethod();
    }

    public boolean verifyRootMethod(ResolvedJavaMethod root)
    {
        JavaMethod currentRoot = getRootMethod();
        return true;
    }

    enum Marker
    {
        None,
        Placeholder,
        Substitution
    }

    public NodeSourcePosition(NodeSourcePosition caller, ResolvedJavaMethod method, int bci)
    {
        this(caller, method, bci, None);
    }

    public NodeSourcePosition(NodeSourcePosition caller, ResolvedJavaMethod method, int bci, Marker marker)
    {
        this(null, caller, method, bci, marker);
    }

    public NodeSourcePosition(SourceLanguagePosition sourceLanguagePosition, NodeSourcePosition caller, ResolvedJavaMethod method, int bci)
    {
        this(sourceLanguagePosition, caller, method, bci, None);
    }

    public NodeSourcePosition(SourceLanguagePosition sourceLanguagePosition, NodeSourcePosition caller, ResolvedJavaMethod method, int bci, Marker marker)
    {
        super(caller, method, bci);
        if (caller == null)
        {
            this.hashCode = 31 * bci + method.hashCode();
        }
        else
        {
            this.hashCode = caller.hashCode * 7 + 31 * bci + method.hashCode();
        }
        this.marker = marker;
        this.sourceLanguagePosition = sourceLanguagePosition;
    }

    public static NodeSourcePosition placeholder(ResolvedJavaMethod method)
    {
        return new NodeSourcePosition(null, method, BytecodeFrame.INVALID_FRAMESTATE_BCI, Placeholder);
    }

    public static NodeSourcePosition placeholder(ResolvedJavaMethod method, int bci)
    {
        return new NodeSourcePosition(null, method, bci, Placeholder);
    }

    public boolean isPlaceholder()
    {
        return marker == Placeholder;
    }

    public static NodeSourcePosition substitution(ResolvedJavaMethod method)
    {
        return substitution(null, method);
    }

    public static NodeSourcePosition substitution(NodeSourcePosition caller, ResolvedJavaMethod method)
    {
        return new NodeSourcePosition(caller, method, BytecodeFrame.INVALID_FRAMESTATE_BCI, Substitution);
    }

    public boolean isSubstitution()
    {
        return marker == Substitution;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (obj != null && getClass() == obj.getClass())
        {
            NodeSourcePosition that = (NodeSourcePosition) obj;
            if (hashCode != that.hashCode)
            {
                return false;
            }
            if (this.getBCI() == that.getBCI() && Objects.equals(this.getMethod(), that.getMethod()) && Objects.equals(this.getCaller(), that.getCaller()) && Objects.equals(this.sourceLanguagePosition, that.sourceLanguagePosition))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    public int depth()
    {
        int d = 0;
        NodeSourcePosition pos = this;
        while (pos != null)
        {
            d++;
            pos = pos.getCaller();
        }
        return d;
    }

    public SourceLanguagePosition getSourceLanguage()
    {
        return sourceLanguagePosition;
    }

    @Override
    public NodeSourcePosition getCaller()
    {
        return (NodeSourcePosition) super.getCaller();
    }

    public NodeSourcePosition addCaller(SourceLanguagePosition newSourceLanguagePosition, NodeSourcePosition link)
    {
        return addCaller(newSourceLanguagePosition, link, false);
    }

    public NodeSourcePosition addCaller(NodeSourcePosition link)
    {
        return addCaller(null, link, false);
    }

    public NodeSourcePosition addCaller(NodeSourcePosition link, boolean isSubstitution)
    {
        return addCaller(null, link, isSubstitution);
    }

    public NodeSourcePosition addCaller(SourceLanguagePosition newSourceLanguagePosition, NodeSourcePosition link, boolean isSubstitution)
    {
        if (getCaller() == null)
        {
            if (isPlaceholder())
            {
                return new NodeSourcePosition(newSourceLanguagePosition, link, getMethod(), 0);
            }

            return new NodeSourcePosition(newSourceLanguagePosition, link, getMethod(), getBCI());
        }
        else
        {
            return new NodeSourcePosition(getCaller().addCaller(newSourceLanguagePosition, link, isSubstitution), getMethod(), getBCI());
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(100);
        NodeSourcePosition pos = this;
        while (pos != null)
        {
            format(sb, pos);
            if (pos.sourceLanguagePosition != null)
            {
                sb.append(" source=" + pos.sourceLanguagePosition.toShortString());
            }
            pos = pos.getCaller();
            if (pos != null)
            {
                sb.append(CodeUtil.NEW_LINE);
            }
        }
        return sb.toString();
    }

    private static void format(StringBuilder sb, NodeSourcePosition pos)
    {
        MetaUtil.appendLocation(sb.append("at "), pos.getMethod(), pos.getBCI());
    }

    String shallowToString()
    {
        StringBuilder sb = new StringBuilder(100);
        format(sb, this);
        return sb.toString();
    }
}
