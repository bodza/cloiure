package giraaff.replacements;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

import giraaff.bytecode.Bytecode;
import giraaff.bytecode.BytecodeProvider;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.AllowAssumptions;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import giraaff.nodes.spi.StampProvider;
import giraaff.util.GraalError;

/**
 * Implementation of {@link GraphBuilderContext} used to produce a graph for a method based on an
 * {@link InvocationPlugin} for the method.
 */
// @class IntrinsicGraphBuilder
public final class IntrinsicGraphBuilder implements GraphBuilderContext, Receiver
{
    // @field
    protected final MetaAccessProvider metaAccess;
    // @field
    protected final ConstantReflectionProvider constantReflection;
    // @field
    protected final ConstantFieldProvider constantFieldProvider;
    // @field
    protected final StampProvider stampProvider;
    // @field
    protected final StructuredGraph graph;
    // @field
    protected final Bytecode code;
    // @field
    protected final ResolvedJavaMethod method;
    // @field
    protected final int invokeBci;
    // @field
    protected FixedWithNextNode lastInstr;
    // @field
    protected ValueNode[] arguments;
    // @field
    protected ValueNode returnValue;

    // @cons
    public IntrinsicGraphBuilder(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, StampProvider __stampProvider, Bytecode __code, int __invokeBci)
    {
        this(__metaAccess, __constantReflection, __constantFieldProvider, __stampProvider, __code, __invokeBci, AllowAssumptions.YES);
    }

    // @cons
    protected IntrinsicGraphBuilder(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, StampProvider __stampProvider, Bytecode __code, int __invokeBci, AllowAssumptions __allowAssumptions)
    {
        super();
        this.metaAccess = __metaAccess;
        this.constantReflection = __constantReflection;
        this.constantFieldProvider = __constantFieldProvider;
        this.stampProvider = __stampProvider;
        this.code = __code;
        this.method = __code.getMethod();
        this.graph = new StructuredGraph.Builder(__allowAssumptions).method(method).build();
        this.invokeBci = __invokeBci;
        this.lastInstr = graph.start();

        Signature __sig = method.getSignature();
        int __max = __sig.getParameterCount(false);
        this.arguments = new ValueNode[__max + (method.isStatic() ? 0 : 1)];

        int __javaIndex = 0;
        int __index = 0;
        if (!method.isStatic())
        {
            // add the receiver
            Stamp __receiverStamp = StampFactory.objectNonNull(TypeReference.createWithoutAssumptions(method.getDeclaringClass()));
            ValueNode __receiver = graph.addWithoutUnique(new ParameterNode(__javaIndex, StampPair.createSingle(__receiverStamp)));
            arguments[__index] = __receiver;
            __javaIndex = 1;
            __index = 1;
        }
        ResolvedJavaType __accessingClass = method.getDeclaringClass();
        for (int __i = 0; __i < __max; __i++)
        {
            JavaType __type = __sig.getParameterType(__i, __accessingClass).resolve(__accessingClass);
            JavaKind __kind = __type.getJavaKind();
            Stamp __stamp;
            if (__kind == JavaKind.Object && __type instanceof ResolvedJavaType)
            {
                __stamp = StampFactory.object(TypeReference.createWithoutAssumptions((ResolvedJavaType) __type));
            }
            else
            {
                __stamp = StampFactory.forKind(__kind);
            }
            ValueNode __param = graph.addWithoutUnique(new ParameterNode(__index, StampPair.createSingle(__stamp)));
            arguments[__index] = __param;
            __javaIndex += __kind.getSlotCount();
            __index++;
        }
    }

    private <T extends ValueNode> void updateLastInstruction(T __v)
    {
        if (__v instanceof FixedNode)
        {
            FixedNode __fixedNode = (FixedNode) __v;
            lastInstr.setNext(__fixedNode);
            if (__fixedNode instanceof FixedWithNextNode)
            {
                FixedWithNextNode __fixedWithNextNode = (FixedWithNextNode) __fixedNode;
                lastInstr = __fixedWithNextNode;
            }
            else
            {
                lastInstr = null;
            }
        }
    }

    @Override
    public <T extends ValueNode> T append(T __v)
    {
        if (__v.graph() != null)
        {
            return __v;
        }
        T __added = graph.addOrUniqueWithInputs(__v);
        if (__added == __v)
        {
            updateLastInstruction(__v);
        }
        return __added;
    }

    @Override
    public void push(JavaKind __kind, ValueNode __value)
    {
        returnValue = __value;
    }

    @Override
    public void handleReplacedInvoke(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __args, boolean __forceInlineEverything)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public void handleReplacedInvoke(CallTargetNode __callTarget, JavaKind __resultType)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public StampProvider getStampProvider()
    {
        return stampProvider;
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return metaAccess;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return constantReflection;
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return constantFieldProvider;
    }

    @Override
    public StructuredGraph getGraph()
    {
        return graph;
    }

    @Override
    public void setStateAfter(StateSplit __sideEffect)
    {
        FrameState __stateAfter = getGraph().add(new FrameState(BytecodeFrame.BEFORE_BCI));
        __sideEffect.setStateAfter(__stateAfter);
    }

    @Override
    public GraphBuilderContext getParent()
    {
        return null;
    }

    @Override
    public Bytecode getCode()
    {
        return code;
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    @Override
    public int bci()
    {
        return invokeBci;
    }

    @Override
    public InvokeKind getInvokeKind()
    {
        return method.isStatic() ? InvokeKind.Static : InvokeKind.Virtual;
    }

    @Override
    public JavaType getInvokeReturnType()
    {
        return method.getSignature().getReturnType(method.getDeclaringClass());
    }

    @Override
    public int getDepth()
    {
        return 0;
    }

    @Override
    public boolean parsingIntrinsic()
    {
        return true;
    }

    @Override
    public IntrinsicContext getIntrinsic()
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public BailoutException bailout(String __msg)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public ValueNode get(boolean __performNullCheck)
    {
        return arguments[0];
    }

    public StructuredGraph buildGraph(InvocationPlugin __plugin)
    {
        Receiver __receiver = method.isStatic() ? null : this;
        if (__plugin.execute(this, method, __receiver, arguments))
        {
            append(new ReturnNode(returnValue));
            return graph;
        }
        return null;
    }

    @Override
    public boolean intrinsify(BytecodeProvider __bytecodeProvider, ResolvedJavaMethod __targetMethod, ResolvedJavaMethod __substitute, InvocationPlugin.Receiver __receiver, ValueNode[] __args)
    {
        throw GraalError.shouldNotReachHere();
    }
}
