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
import giraaff.debug.GraalError;
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
import giraaff.options.OptionValues;

/**
 * Implementation of {@link GraphBuilderContext} used to produce a graph for a method based on an
 * {@link InvocationPlugin} for the method.
 */
public class IntrinsicGraphBuilder implements GraphBuilderContext, Receiver
{
    protected final MetaAccessProvider metaAccess;
    protected final ConstantReflectionProvider constantReflection;
    protected final ConstantFieldProvider constantFieldProvider;
    protected final StampProvider stampProvider;
    protected final StructuredGraph graph;
    protected final Bytecode code;
    protected final ResolvedJavaMethod method;
    protected final int invokeBci;
    protected FixedWithNextNode lastInstr;
    protected ValueNode[] arguments;
    protected ValueNode returnValue;

    public IntrinsicGraphBuilder(OptionValues options, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, StampProvider stampProvider, Bytecode code, int invokeBci)
    {
        this(options, metaAccess, constantReflection, constantFieldProvider, stampProvider, code, invokeBci, AllowAssumptions.YES);
    }

    protected IntrinsicGraphBuilder(OptionValues options, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, StampProvider stampProvider, Bytecode code, int invokeBci, AllowAssumptions allowAssumptions)
    {
        this.metaAccess = metaAccess;
        this.constantReflection = constantReflection;
        this.constantFieldProvider = constantFieldProvider;
        this.stampProvider = stampProvider;
        this.code = code;
        this.method = code.getMethod();
        this.graph = new StructuredGraph.Builder(options, allowAssumptions).method(method).build();
        this.invokeBci = invokeBci;
        this.lastInstr = graph.start();

        Signature sig = method.getSignature();
        int max = sig.getParameterCount(false);
        this.arguments = new ValueNode[max + (method.isStatic() ? 0 : 1)];

        int javaIndex = 0;
        int index = 0;
        if (!method.isStatic())
        {
            // add the receiver
            Stamp receiverStamp = StampFactory.objectNonNull(TypeReference.createWithoutAssumptions(method.getDeclaringClass()));
            ValueNode receiver = graph.addWithoutUnique(new ParameterNode(javaIndex, StampPair.createSingle(receiverStamp)));
            arguments[index] = receiver;
            javaIndex = 1;
            index = 1;
        }
        ResolvedJavaType accessingClass = method.getDeclaringClass();
        for (int i = 0; i < max; i++)
        {
            JavaType type = sig.getParameterType(i, accessingClass).resolve(accessingClass);
            JavaKind kind = type.getJavaKind();
            Stamp stamp;
            if (kind == JavaKind.Object && type instanceof ResolvedJavaType)
            {
                stamp = StampFactory.object(TypeReference.createWithoutAssumptions((ResolvedJavaType) type));
            }
            else
            {
                stamp = StampFactory.forKind(kind);
            }
            ValueNode param = graph.addWithoutUnique(new ParameterNode(index, StampPair.createSingle(stamp)));
            arguments[index] = param;
            javaIndex += kind.getSlotCount();
            index++;
        }
    }

    private <T extends ValueNode> void updateLastInstruction(T v)
    {
        if (v instanceof FixedNode)
        {
            FixedNode fixedNode = (FixedNode) v;
            lastInstr.setNext(fixedNode);
            if (fixedNode instanceof FixedWithNextNode)
            {
                FixedWithNextNode fixedWithNextNode = (FixedWithNextNode) fixedNode;
                lastInstr = fixedWithNextNode;
            }
            else
            {
                lastInstr = null;
            }
        }
    }

    @Override
    public <T extends ValueNode> T append(T v)
    {
        if (v.graph() != null)
        {
            return v;
        }
        T added = graph.addOrUniqueWithInputs(v);
        if (added == v)
        {
            updateLastInstruction(v);
        }
        return added;
    }

    @Override
    public void push(JavaKind kind, ValueNode value)
    {
        returnValue = value;
    }

    @Override
    public void handleReplacedInvoke(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] args, boolean forceInlineEverything)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public void handleReplacedInvoke(CallTargetNode callTarget, JavaKind resultType)
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
    public void setStateAfter(StateSplit sideEffect)
    {
        FrameState stateAfter = getGraph().add(new FrameState(BytecodeFrame.BEFORE_BCI));
        sideEffect.setStateAfter(stateAfter);
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
    public BailoutException bailout(String string)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public ValueNode get(boolean performNullCheck)
    {
        return arguments[0];
    }

    public StructuredGraph buildGraph(InvocationPlugin plugin)
    {
        Receiver receiver = method.isStatic() ? null : this;
        if (plugin.execute(this, method, receiver, arguments))
        {
            append(new ReturnNode(returnValue));
            return graph;
        }
        return null;
    }

    @Override
    public boolean intrinsify(BytecodeProvider bytecodeProvider, ResolvedJavaMethod targetMethod, ResolvedJavaMethod substitute, InvocationPlugin.Receiver receiver, ValueNode[] args)
    {
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public String toString()
    {
        return String.format("%s:intrinsic", method.format("%H.%n(%p)"));
    }
}
