package giraaff.lir;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.util.GraalError;

public class LIRInstructionClass<T> extends LIRIntrospection<T>
{
    public static <T extends LIRInstruction> LIRInstructionClass<T> create(Class<T> c)
    {
        return new LIRInstructionClass<>(c);
    }

    private static final Class<LIRInstruction> INSTRUCTION_CLASS = LIRInstruction.class;

    private final Values uses;
    private final Values alives;
    private final Values temps;
    private final Values defs;

    private final boolean isMoveOp;
    private final boolean isValueMoveOp;
    private final boolean isLoadConstantOp;

    private String opcodeConstant;
    private int opcodeIndex;

    private LIRInstructionClass(Class<T> clazz)
    {
        this(clazz, new FieldsScanner.DefaultCalcOffset());
    }

    public LIRInstructionClass(Class<T> clazz, FieldsScanner.CalcOffset calcOffset)
    {
        super(clazz);

        LIRInstructionFieldsScanner ifs = new LIRInstructionFieldsScanner(calcOffset);
        ifs.scan(clazz);

        uses = new Values(ifs.valueAnnotations.get(LIRInstruction.Use.class));
        alives = new Values(ifs.valueAnnotations.get(LIRInstruction.Alive.class));
        temps = new Values(ifs.valueAnnotations.get(LIRInstruction.Temp.class));
        defs = new Values(ifs.valueAnnotations.get(LIRInstruction.Def.class));

        data = new Fields(ifs.data);

        opcodeConstant = ifs.opcodeConstant;
        if (ifs.opcodeField == null)
        {
            opcodeIndex = -1;
        }
        else
        {
            opcodeIndex = ifs.data.indexOf(ifs.opcodeField);
        }

        isMoveOp = MoveOp.class.isAssignableFrom(clazz);
        isValueMoveOp = ValueMoveOp.class.isAssignableFrom(clazz);
        isLoadConstantOp = LoadConstantOp.class.isAssignableFrom(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> LIRInstructionClass<T> get(Class<T> clazz)
    {
        try
        {
            Field field = clazz.getDeclaredField("TYPE");
            field.setAccessible(true);
            LIRInstructionClass<T> result = (LIRInstructionClass<T>) field.get(null);
            if (result == null)
            {
                throw GraalError.shouldNotReachHere("TYPE field not initialized for class " + clazz.getTypeName());
            }
            return result;
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class LIRInstructionFieldsScanner extends LIRFieldsScanner
    {
        private String opcodeConstant;

        /**
         * Field (if any) annotated by {@link Opcode}.
         */
        private FieldsScanner.FieldInfo opcodeField;

        LIRInstructionFieldsScanner(FieldsScanner.CalcOffset calc)
        {
            super(calc);

            valueAnnotations.put(LIRInstruction.Use.class, new OperandModeAnnotation());
            valueAnnotations.put(LIRInstruction.Alive.class, new OperandModeAnnotation());
            valueAnnotations.put(LIRInstruction.Temp.class, new OperandModeAnnotation());
            valueAnnotations.put(LIRInstruction.Def.class, new OperandModeAnnotation());
        }

        @Override
        protected EnumSet<OperandFlag> getFlags(Field field)
        {
            EnumSet<OperandFlag> result = EnumSet.noneOf(OperandFlag.class);
            // Unfortunately, annotations cannot have class hierarchies or implement interfaces,
            // so we have to duplicate the code for every operand mode.
            // Unfortunately, annotations cannot have an EnumSet property, so we have to convert
            // from arrays to EnumSet manually.
            if (field.isAnnotationPresent(LIRInstruction.Use.class))
            {
                result.addAll(Arrays.asList(field.getAnnotation(LIRInstruction.Use.class).value()));
            }
            else if (field.isAnnotationPresent(LIRInstruction.Alive.class))
            {
                result.addAll(Arrays.asList(field.getAnnotation(LIRInstruction.Alive.class).value()));
            }
            else if (field.isAnnotationPresent(LIRInstruction.Temp.class))
            {
                result.addAll(Arrays.asList(field.getAnnotation(LIRInstruction.Temp.class).value()));
            }
            else if (field.isAnnotationPresent(LIRInstruction.Def.class))
            {
                result.addAll(Arrays.asList(field.getAnnotation(LIRInstruction.Def.class).value()));
            }
            else
            {
                GraalError.shouldNotReachHere();
            }
            return result;
        }

        public void scan(Class<?> clazz)
        {
            if (clazz.getAnnotation(Opcode.class) != null)
            {
                opcodeConstant = null;
            }
            opcodeField = null;

            super.scan(clazz, LIRInstruction.class, false);

            if (opcodeConstant == null && opcodeField == null)
            {
                opcodeConstant = clazz.getSimpleName();
                if (opcodeConstant.endsWith("Op"))
                {
                    opcodeConstant = opcodeConstant.substring(0, opcodeConstant.length() - 2);
                }
            }
        }

        @Override
        protected void scanField(Field field, long offset)
        {
            super.scanField(field, offset);

            if (field.getAnnotation(Opcode.class) != null)
            {
                opcodeField = data.get(data.size() - 1);
            }
        }
    }

    @Override
    public Fields[] getAllFields()
    {
        return new Fields[] { data, uses, alives, temps, defs };
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" ").append(getClazz().getSimpleName()).append(" use[");
        uses.appendFields(sb);
        sb.append("] alive[");
        alives.appendFields(sb);
        sb.append("] temp[");
        temps.appendFields(sb);
        sb.append("] def[");
        defs.appendFields(sb);
        sb.append("] data[");
        data.appendFields(sb);
        sb.append("]");
        return sb.toString();
    }

    Values getValues(OperandMode mode)
    {
        switch (mode)
        {
            case USE:
                return uses;
            case ALIVE:
                return alives;
            case TEMP:
                return temps;
            case DEF:
                return defs;
            default:
                throw GraalError.shouldNotReachHere("unknown OperandMode: " + mode);
        }
    }

    final String getOpcode(LIRInstruction obj)
    {
        if (opcodeConstant != null)
        {
            return opcodeConstant;
        }
        return String.valueOf(data.getObject(obj, opcodeIndex));
    }

    final boolean hasOperands()
    {
        return uses.getCount() > 0 || alives.getCount() > 0 || temps.getCount() > 0 || defs.getCount() > 0;
    }

    final void forEachUse(LIRInstruction obj, InstructionValueProcedure proc)
    {
        forEach(obj, uses, OperandMode.USE, proc);
    }

    final void forEachAlive(LIRInstruction obj, InstructionValueProcedure proc)
    {
        forEach(obj, alives, OperandMode.ALIVE, proc);
    }

    final void forEachTemp(LIRInstruction obj, InstructionValueProcedure proc)
    {
        forEach(obj, temps, OperandMode.TEMP, proc);
    }

    final void forEachDef(LIRInstruction obj, InstructionValueProcedure proc)
    {
        forEach(obj, defs, OperandMode.DEF, proc);
    }

    final void visitEachUse(LIRInstruction obj, InstructionValueConsumer proc)
    {
        visitEach(obj, uses, OperandMode.USE, proc);
    }

    final void visitEachAlive(LIRInstruction obj, InstructionValueConsumer proc)
    {
        visitEach(obj, alives, OperandMode.ALIVE, proc);
    }

    final void visitEachTemp(LIRInstruction obj, InstructionValueConsumer proc)
    {
        visitEach(obj, temps, OperandMode.TEMP, proc);
    }

    final void visitEachDef(LIRInstruction obj, InstructionValueConsumer proc)
    {
        visitEach(obj, defs, OperandMode.DEF, proc);
    }

    final Value forEachRegisterHint(LIRInstruction obj, OperandMode mode, InstructionValueProcedure proc)
    {
        Values hints;
        if (mode == OperandMode.USE)
        {
            hints = defs;
        }
        else if (mode == OperandMode.DEF)
        {
            hints = uses;
        }
        else
        {
            return null;
        }

        for (int i = 0; i < hints.getCount(); i++)
        {
            if (i < hints.getDirectCount())
            {
                Value hintValue = hints.getValue(obj, i);
                Value result = proc.doValue(obj, hintValue, null, null);
                if (result != null)
                {
                    return result;
                }
            }
            else
            {
                Value[] hintValues = hints.getValueArray(obj, i);
                for (int j = 0; j < hintValues.length; j++)
                {
                    Value hintValue = hintValues[j];
                    Value result = proc.doValue(obj, hintValue, null, null);
                    if (result != null)
                    {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    String toString(LIRInstruction obj)
    {
        StringBuilder sb = new StringBuilder();

        appendValues(sb, obj, "", " = ", "(", ")", new String[] { "" }, defs);
        sb.append(String.valueOf(getOpcode(obj)).toUpperCase());
        appendValues(sb, obj, " ", "", "(", ")", new String[] { "", "~" }, uses, alives);
        appendValues(sb, obj, " ", "", "{", "}", new String[] { "" }, temps);

        for (int i = 0; i < data.getCount(); i++)
        {
            if (i != opcodeIndex)
            {
                sb.append(" ").append(data.getName(i)).append(": ").append(getFieldString(obj, i, data));
            }
        }

        return sb.toString();
    }

    final boolean isMoveOp()
    {
        return isMoveOp;
    }

    final boolean isValueMoveOp()
    {
        return isValueMoveOp;
    }

    final boolean isLoadConstantOp()
    {
        return isLoadConstantOp;
    }
}
