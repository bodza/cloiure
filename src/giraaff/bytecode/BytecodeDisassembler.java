package giraaff.bytecode;

import jdk.vm.ci.meta.ConstantPool;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaField;
import jdk.vm.ci.meta.JavaMethod;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.bytecode.Bytecodes;

/**
 * Utility for producing a {@code javap}-like disassembly of bytecode.
 */
// @class BytecodeDisassembler
public final class BytecodeDisassembler
{
    /**
     * Specifies if the disassembly for a single instruction can span multiple lines.
     */
    private final boolean multiline;

    private final boolean newLine;

    // @cons
    public BytecodeDisassembler(boolean multiline, boolean newLine)
    {
        super();
        this.multiline = multiline;
        this.newLine = newLine;
    }

    // @cons
    public BytecodeDisassembler(boolean multiline)
    {
        this(multiline, true);
    }

    // @cons
    public BytecodeDisassembler()
    {
        this(true, true);
    }

    public static String disassembleOne(ResolvedJavaMethod method, int bci)
    {
        return new BytecodeDisassembler(false, false).disassemble(method, bci, bci);
    }

    /**
     * Disassembles the bytecode of a given method in a {@code javap}-like format.
     *
     * @return {@code null} if {@code method} has no bytecode (e.g. it is native or abstract)
     */
    public String disassemble(ResolvedJavaMethod method)
    {
        return disassemble(method, 0, Integer.MAX_VALUE);
    }

    /**
     * Disassembles the bytecode of a given method in a {@code javap}-like format.
     *
     * @return {@code null} if {@code method} has no bytecode (e.g. it is native or abstract)
     */
    public String disassemble(ResolvedJavaMethod method, int startBci, int endBci)
    {
        return disassemble(new ResolvedJavaMethodBytecode(method), startBci, endBci);
    }

    /**
     * Disassembles {@code code} in a {@code javap}-like format.
     */
    public String disassemble(Bytecode code)
    {
        return disassemble(code, 0, Integer.MAX_VALUE);
    }

    /**
     * Disassembles {@code code} in a {@code javap}-like format.
     */
    public String disassemble(Bytecode code, int startBci, int endBci)
    {
        if (code.getCode() == null)
        {
            return null;
        }
        ResolvedJavaMethod method = code.getMethod();
        ConstantPool cp = code.getConstantPool();
        BytecodeStream stream = new BytecodeStream(code.getCode());
        StringBuilder buf = new StringBuilder();
        int opcode = stream.currentBC();
        try
        {
            while (opcode != Bytecodes.END)
            {
                int bci = stream.currentBCI();
                if (bci >= startBci && bci <= endBci)
                {
                    String mnemonic = Bytecodes.nameOf(opcode);
                    buf.append(String.format("%4d: %-14s", bci, mnemonic));
                    if (stream.nextBCI() > bci + 1)
                    {
                        decodeOperand(buf, stream, cp, method, bci, opcode);
                    }
                    if (newLine)
                    {
                        buf.append(String.format("%n"));
                    }
                }
                stream.next();
                opcode = stream.currentBC();
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException(String.format("Error disassembling %s%nPartial disassembly:%n%s", method.format("%H.%n(%p)"), buf.toString()), e);
        }
        return buf.toString();
    }

    private void decodeOperand(StringBuilder buf, BytecodeStream stream, ConstantPool cp, ResolvedJavaMethod method, int bci, int opcode)
    {
        switch (opcode)
        {
            case Bytecodes.BIPUSH:          buf.append(stream.readByte()); break;
            case Bytecodes.SIPUSH:          buf.append(stream.readShort()); break;
            case Bytecodes.NEW:
            case Bytecodes.CHECKCAST:
            case Bytecodes.INSTANCEOF:
            case Bytecodes.ANEWARRAY:
            {
                int cpi = stream.readCPI();
                JavaType type = cp.lookupType(cpi, opcode);
                buf.append(String.format("#%-10d // %s", cpi, type.toJavaName()));
                break;
            }
            case Bytecodes.GETSTATIC:
            case Bytecodes.PUTSTATIC:
            case Bytecodes.GETFIELD:
            case Bytecodes.PUTFIELD:
            {
                int cpi = stream.readCPI();
                JavaField field = cp.lookupField(cpi, method, opcode);
                String fieldDesc = field.getDeclaringClass().getName().equals(method.getDeclaringClass().getName()) ? field.format("%n:%T") : field.format("%H.%n:%T");
                buf.append(String.format("#%-10d // %s", cpi, fieldDesc));
                break;
            }
            case Bytecodes.INVOKEVIRTUAL:
            case Bytecodes.INVOKESPECIAL:
            case Bytecodes.INVOKESTATIC:
            {
                int cpi = stream.readCPI();
                JavaMethod callee = cp.lookupMethod(cpi, opcode);
                String calleeDesc = callee.getDeclaringClass().getName().equals(method.getDeclaringClass().getName()) ? callee.format("%n:(%P)%R") : callee.format("%H.%n:(%P)%R");
                buf.append(String.format("#%-10d // %s", cpi, calleeDesc));
                break;
            }
            case Bytecodes.INVOKEINTERFACE:
            {
                int cpi = stream.readCPI();
                JavaMethod callee = cp.lookupMethod(cpi, opcode);
                String calleeDesc = callee.getDeclaringClass().getName().equals(method.getDeclaringClass().getName()) ? callee.format("%n:(%P)%R") : callee.format("%H.%n:(%P)%R");
                buf.append(String.format("#%-10s // %s", cpi + ", " + stream.readUByte(bci + 3), calleeDesc));
                break;
            }
            case Bytecodes.INVOKEDYNAMIC:
            {
                int cpi = stream.readCPI4();
                JavaMethod callee = cp.lookupMethod(cpi, opcode);
                String calleeDesc = callee.getDeclaringClass().getName().equals(method.getDeclaringClass().getName()) ? callee.format("%n:(%P)%R") : callee.format("%H.%n:(%P)%R");
                buf.append(String.format("#%-10d // %s", cpi, calleeDesc));
                break;
            }
            case Bytecodes.LDC:
            case Bytecodes.LDC_W:
            case Bytecodes.LDC2_W:
            {
                int cpi = stream.readCPI();
                Object constant = cp.lookupConstant(cpi);
                String desc = null;
                if (constant instanceof JavaConstant)
                {
                    JavaConstant c = ((JavaConstant) constant);
                    desc = c.toValueString();
                }
                else
                {
                    desc = constant.toString();
                }
                if (!multiline)
                {
                    desc = desc.replaceAll("\\n", "");
                }
                buf.append(String.format("#%-10d // %s", cpi, desc));
                break;
            }
            case Bytecodes.RET:
            case Bytecodes.ILOAD:
            case Bytecodes.LLOAD:
            case Bytecodes.FLOAD:
            case Bytecodes.DLOAD:
            case Bytecodes.ALOAD:
            case Bytecodes.ISTORE:
            case Bytecodes.LSTORE:
            case Bytecodes.FSTORE:
            case Bytecodes.DSTORE:
            case Bytecodes.ASTORE:
            {
                buf.append(String.format("%d", stream.readLocalIndex()));
                break;
            }
            case Bytecodes.IFEQ:
            case Bytecodes.IFNE:
            case Bytecodes.IFLT:
            case Bytecodes.IFGE:
            case Bytecodes.IFGT:
            case Bytecodes.IFLE:
            case Bytecodes.IF_ICMPEQ:
            case Bytecodes.IF_ICMPNE:
            case Bytecodes.IF_ICMPLT:
            case Bytecodes.IF_ICMPGE:
            case Bytecodes.IF_ICMPGT:
            case Bytecodes.IF_ICMPLE:
            case Bytecodes.IF_ACMPEQ:
            case Bytecodes.IF_ACMPNE:
            case Bytecodes.GOTO:
            case Bytecodes.JSR:
            case Bytecodes.IFNULL:
            case Bytecodes.IFNONNULL:
            case Bytecodes.GOTO_W:
            case Bytecodes.JSR_W:
            {
                buf.append(String.format("%d", stream.readBranchDest()));
                break;
            }
            case Bytecodes.LOOKUPSWITCH:
            case Bytecodes.TABLESWITCH:
            {
                BytecodeSwitch bswitch = opcode == Bytecodes.LOOKUPSWITCH ? new BytecodeLookupSwitch(stream, bci) : new BytecodeTableSwitch(stream, bci);
                if (multiline)
                {
                    buf.append("{ // " + bswitch.numberOfCases());
                    for (int i = 0; i < bswitch.numberOfCases(); i++)
                    {
                        buf.append(String.format("%n           %7d: %d", bswitch.keyAt(i), bswitch.targetAt(i)));
                    }
                    buf.append(String.format("%n           default: %d", bswitch.defaultTarget()));
                    buf.append(String.format("%n      }"));
                }
                else
                {
                    buf.append("[" + bswitch.numberOfCases()).append("] {");
                    for (int i = 0; i < bswitch.numberOfCases(); i++)
                    {
                        buf.append(String.format("%d: %d", bswitch.keyAt(i), bswitch.targetAt(i)));
                        if (i != bswitch.numberOfCases() - 1)
                        {
                            buf.append(", ");
                        }
                    }
                    buf.append(String.format("} default: %d", bswitch.defaultTarget()));
                }
                break;
            }
            case Bytecodes.NEWARRAY:
            {
                int typecode = stream.readLocalIndex();
                switch (typecode)
                {
                    case 4:  buf.append("boolean"); break;
                    case 5:  buf.append("char"); break;
                    case 6:  buf.append("float"); break;
                    case 7:  buf.append("double"); break;
                    case 8:  buf.append("byte"); break;
                    case 9:  buf.append("short"); break;
                    case 10: buf.append("int"); break;
                    case 11: buf.append("long"); break;
                }

                break;
            }
            case Bytecodes.MULTIANEWARRAY:
            {
                int cpi = stream.readCPI();
                JavaType type = cp.lookupType(cpi, opcode);
                buf.append(String.format("#%-10s // %s", cpi + ", " + stream.readUByte(bci + 3), type.toJavaName()));
                break;
            }
        }
    }

    public static JavaMethod getInvokedMethodAt(ResolvedJavaMethod method, int invokeBci)
    {
        if (method.getCode() == null)
        {
            return null;
        }
        ConstantPool cp = method.getConstantPool();
        BytecodeStream stream = new BytecodeStream(method.getCode());
        int opcode = stream.currentBC();
        while (opcode != Bytecodes.END)
        {
            int bci = stream.currentBCI();
            if (bci == invokeBci)
            {
                if (stream.nextBCI() > bci + 1)
                {
                    switch (opcode)
                    {
                        case Bytecodes.INVOKEVIRTUAL:
                        case Bytecodes.INVOKESPECIAL:
                        case Bytecodes.INVOKESTATIC:
                        {
                            int cpi = stream.readCPI();
                            return cp.lookupMethod(cpi, opcode);
                        }
                        case Bytecodes.INVOKEINTERFACE:
                        {
                            int cpi = stream.readCPI();
                            return cp.lookupMethod(cpi, opcode);
                        }
                        case Bytecodes.INVOKEDYNAMIC:
                        {
                            int cpi = stream.readCPI4();
                            return cp.lookupMethod(cpi, opcode);
                        }
                        default:
                            throw new InternalError(BytecodeDisassembler.disassembleOne(method, invokeBci));
                    }
                }
            }
            stream.next();
            opcode = stream.currentBC();
        }
        return null;
    }

    public static int getBytecodeAt(ResolvedJavaMethod method, int invokeBci)
    {
        if (method.getCode() == null)
        {
            return -1;
        }
        BytecodeStream stream = new BytecodeStream(method.getCode());
        int opcode = stream.currentBC();
        while (opcode != Bytecodes.END)
        {
            int bci = stream.currentBCI();
            if (bci == invokeBci)
            {
                return opcode;
            }
            stream.next();
            opcode = stream.currentBC();
        }
        return -1;
    }
}
