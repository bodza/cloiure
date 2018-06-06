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
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.IntrinsicContext;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.spi.StampProvider;
import giraaff.util.GraalError;

///
// Implementation of {@link GraphBuilderContext} used to produce a graph for a method based on an
// {@link InvocationPlugin} for the method.
///
// @class IntrinsicGraphBuilder
public final class IntrinsicGraphBuilder implements GraphBuilderContext, InvocationPlugin.Receiver
{
    // @field
    protected final MetaAccessProvider ___metaAccess;
    // @field
    protected final ConstantReflectionProvider ___constantReflection;
    // @field
    protected final ConstantFieldProvider ___constantFieldProvider;
    // @field
    protected final StampProvider ___stampProvider;
    // @field
    protected final StructuredGraph ___graph;
    // @field
    protected final Bytecode ___code;
    // @field
    protected final ResolvedJavaMethod ___method;
    // @field
    protected final int ___invokeBci;
    // @field
    protected FixedWithNextNode ___lastInstr;
    // @field
    protected ValueNode[] ___arguments;
    // @field
    protected ValueNode ___returnValue;

    // @cons IntrinsicGraphBuilder
    public IntrinsicGraphBuilder(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, StampProvider __stampProvider, Bytecode __code, int __invokeBci)
    {
        this(__metaAccess, __constantReflection, __constantFieldProvider, __stampProvider, __code, __invokeBci, StructuredGraph.AllowAssumptions.YES);
    }

    // @cons IntrinsicGraphBuilder
    protected IntrinsicGraphBuilder(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection, ConstantFieldProvider __constantFieldProvider, StampProvider __stampProvider, Bytecode __code, int __invokeBci, StructuredGraph.AllowAssumptions __allowAssumptions)
    {
        super();
        this.___metaAccess = __metaAccess;
        this.___constantReflection = __constantReflection;
        this.___constantFieldProvider = __constantFieldProvider;
        this.___stampProvider = __stampProvider;
        this.___code = __code;
        this.___method = __code.getMethod();
        this.___graph = new StructuredGraph.GraphBuilder(__allowAssumptions).method(this.___method).build();
        this.___invokeBci = __invokeBci;
        this.___lastInstr = this.___graph.start();

        Signature __sig = this.___method.getSignature();
        int __max = __sig.getParameterCount(false);
        this.___arguments = new ValueNode[__max + (this.___method.isStatic() ? 0 : 1)];

        int __javaIndex = 0;
        int __index = 0;
        if (!this.___method.isStatic())
        {
            // add the receiver
            Stamp __receiverStamp = StampFactory.objectNonNull(TypeReference.createWithoutAssumptions(this.___method.getDeclaringClass()));
            ValueNode __receiver = this.___graph.addWithoutUnique(new ParameterNode(__javaIndex, StampPair.createSingle(__receiverStamp)));
            this.___arguments[__index] = __receiver;
            __javaIndex = 1;
            __index = 1;
        }
        ResolvedJavaType __accessingClass = this.___method.getDeclaringClass();
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
            ValueNode __param = this.___graph.addWithoutUnique(new ParameterNode(__index, StampPair.createSingle(__stamp)));
            this.___arguments[__index] = __param;
            __javaIndex += __kind.getSlotCount();
            __index++;
        }
    }

    private <T extends ValueNode> void updateLastInstruction(T __v)
    {
        if (__v instanceof FixedNode)
        {
            FixedNode __fixedNode = (FixedNode) __v;
            this.___lastInstr.setNext(__fixedNode);
            if (__fixedNode instanceof FixedWithNextNode)
            {
                FixedWithNextNode __fixedWithNextNode = (FixedWithNextNode) __fixedNode;
                this.___lastInstr = __fixedWithNextNode;
            }
            else
            {
                this.___lastInstr = null;
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
        T __added = this.___graph.addOrUniqueWithInputs(__v);
        if (__added == __v)
        {
            updateLastInstruction(__v);
        }
        return __added;
    }

    @Override
    public void push(JavaKind __kind, ValueNode __value)
    {
        this.___returnValue = __value;
    }

    @Override
    public void handleReplacedInvoke(CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __args, boolean __forceInlineEverything)
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
        return this.___stampProvider;
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return this.___metaAccess;
    }

    @Override
    public ConstantReflectionProvider getConstantReflection()
    {
        return this.___constantReflection;
    }

    @Override
    public ConstantFieldProvider getConstantFieldProvider()
    {
        return this.___constantFieldProvider;
    }

    @Override
    public StructuredGraph getGraph()
    {
        return this.___graph;
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
        return this.___code;
    }

    @Override
    public ResolvedJavaMethod getMethod()
    {
        return this.___method;
    }

    @Override
    public int bci()
    {
        return this.___invokeBci;
    }

    @Override
    public CallTargetNode.InvokeKind getInvokeKind()
    {
        return this.___method.isStatic() ? CallTargetNode.InvokeKind.Static : CallTargetNode.InvokeKind.Virtual;
    }

    @Override
    public JavaType getInvokeReturnType()
    {
        return this.___method.getSignature().getReturnType(this.___method.getDeclaringClass());
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
        return this.___arguments[0];
    }

    public StructuredGraph buildGraph(InvocationPlugin __plugin)
    {
        InvocationPlugin.Receiver __receiver = this.___method.isStatic() ? null : this;
        if (__plugin.execute(this, this.___method, __receiver, this.___arguments))
        {
            append(new ReturnNode(this.___returnValue));
            return this.___graph;
        }
        return null;
    }

    @Override
    public boolean intrinsify(BytecodeProvider __bytecodeProvider, ResolvedJavaMethod __targetMethod, ResolvedJavaMethod __substitute, InvocationPlugin.Receiver __receiver, ValueNode[] __args)
    {
        throw GraalError.shouldNotReachHere();
    }
}
