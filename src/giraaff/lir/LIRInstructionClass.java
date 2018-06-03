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

// @class LIRInstructionClass
public final class LIRInstructionClass<T> extends LIRIntrospection<T>
{
    public static <T extends LIRInstruction> LIRInstructionClass<T> create(Class<T> __c)
    {
        return new LIRInstructionClass<>(__c);
    }

    // @def
    private static final Class<LIRInstruction> INSTRUCTION_CLASS = LIRInstruction.class;

    // @field
    private final Values uses;
    // @field
    private final Values alives;
    // @field
    private final Values temps;
    // @field
    private final Values defs;

    // @field
    private final boolean isMoveOp;
    // @field
    private final boolean isValueMoveOp;
    // @field
    private final boolean isLoadConstantOp;

    // @field
    private String opcodeConstant;
    // @field
    private int opcodeIndex;

    // @cons
    private LIRInstructionClass(Class<T> __clazz)
    {
        this(__clazz, new FieldsScanner.DefaultCalcOffset());
    }

    // @cons
    public LIRInstructionClass(Class<T> __clazz, FieldsScanner.CalcOffset __calcOffset)
    {
        super(__clazz);

        LIRInstructionFieldsScanner __ifs = new LIRInstructionFieldsScanner(__calcOffset);
        __ifs.scan(__clazz);

        uses = new Values(__ifs.valueAnnotations.get(LIRInstruction.Use.class));
        alives = new Values(__ifs.valueAnnotations.get(LIRInstruction.Alive.class));
        temps = new Values(__ifs.valueAnnotations.get(LIRInstruction.Temp.class));
        defs = new Values(__ifs.valueAnnotations.get(LIRInstruction.Def.class));

        data = new Fields(__ifs.data);

        opcodeConstant = __ifs.opcodeConstant;
        if (__ifs.opcodeField == null)
        {
            opcodeIndex = -1;
        }
        else
        {
            opcodeIndex = __ifs.data.indexOf(__ifs.opcodeField);
        }

        isMoveOp = MoveOp.class.isAssignableFrom(__clazz);
        isValueMoveOp = ValueMoveOp.class.isAssignableFrom(__clazz);
        isLoadConstantOp = LoadConstantOp.class.isAssignableFrom(__clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> LIRInstructionClass<T> get(Class<T> __clazz)
    {
        try
        {
            Field __field = __clazz.getDeclaredField("TYPE");
            __field.setAccessible(true);
            LIRInstructionClass<T> __result = (LIRInstructionClass<T>) __field.get(null);
            if (__result == null)
            {
                throw GraalError.shouldNotReachHere("TYPE field not initialized for class " + __clazz.getTypeName());
            }
            return __result;
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException __e)
        {
            throw new RuntimeException(__e);
        }
    }

    // @class LIRInstructionClass.LIRInstructionFieldsScanner
    private static final class LIRInstructionFieldsScanner extends LIRFieldsScanner
    {
        // @field
        private String opcodeConstant;

        /**
         * Field (if any) annotated by {@link Opcode}.
         */
        // @field
        private FieldsScanner.FieldInfo opcodeField;

        // @cons
        LIRInstructionFieldsScanner(FieldsScanner.CalcOffset __calc)
        {
            super(__calc);

            valueAnnotations.put(LIRInstruction.Use.class, new OperandModeAnnotation());
            valueAnnotations.put(LIRInstruction.Alive.class, new OperandModeAnnotation());
            valueAnnotations.put(LIRInstruction.Temp.class, new OperandModeAnnotation());
            valueAnnotations.put(LIRInstruction.Def.class, new OperandModeAnnotation());
        }

        @Override
        protected EnumSet<OperandFlag> getFlags(Field __field)
        {
            EnumSet<OperandFlag> __result = EnumSet.noneOf(OperandFlag.class);
            // Unfortunately, annotations cannot have class hierarchies or implement interfaces,
            // so we have to duplicate the code for every operand mode.
            // Unfortunately, annotations cannot have an EnumSet property, so we have to convert
            // from arrays to EnumSet manually.
            if (__field.isAnnotationPresent(LIRInstruction.Use.class))
            {
                __result.addAll(Arrays.asList(__field.getAnnotation(LIRInstruction.Use.class).value()));
            }
            else if (__field.isAnnotationPresent(LIRInstruction.Alive.class))
            {
                __result.addAll(Arrays.asList(__field.getAnnotation(LIRInstruction.Alive.class).value()));
            }
            else if (__field.isAnnotationPresent(LIRInstruction.Temp.class))
            {
                __result.addAll(Arrays.asList(__field.getAnnotation(LIRInstruction.Temp.class).value()));
            }
            else if (__field.isAnnotationPresent(LIRInstruction.Def.class))
            {
                __result.addAll(Arrays.asList(__field.getAnnotation(LIRInstruction.Def.class).value()));
            }
            else
            {
                GraalError.shouldNotReachHere();
            }
            return __result;
        }

        public void scan(Class<?> __clazz)
        {
            if (__clazz.getAnnotation(Opcode.class) != null)
            {
                opcodeConstant = null;
            }
            opcodeField = null;

            super.scan(__clazz, LIRInstruction.class, false);

            if (opcodeConstant == null && opcodeField == null)
            {
                opcodeConstant = __clazz.getSimpleName();
                if (opcodeConstant.endsWith("Op"))
                {
                    opcodeConstant = opcodeConstant.substring(0, opcodeConstant.length() - 2);
                }
            }
        }

        @Override
        protected void scanField(Field __field, long __offset)
        {
            super.scanField(__field, __offset);

            if (__field.getAnnotation(Opcode.class) != null)
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

    Values getValues(OperandMode __mode)
    {
        switch (__mode)
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
                throw GraalError.shouldNotReachHere("unknown OperandMode: " + __mode);
        }
    }

    final String getOpcode(LIRInstruction __obj)
    {
        if (opcodeConstant != null)
        {
            return opcodeConstant;
        }
        return String.valueOf(data.getObject(__obj, opcodeIndex));
    }

    final boolean hasOperands()
    {
        return uses.getCount() > 0 || alives.getCount() > 0 || temps.getCount() > 0 || defs.getCount() > 0;
    }

    final void forEachUse(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, uses, OperandMode.USE, __proc);
    }

    final void forEachAlive(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, alives, OperandMode.ALIVE, __proc);
    }

    final void forEachTemp(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, temps, OperandMode.TEMP, __proc);
    }

    final void forEachDef(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, defs, OperandMode.DEF, __proc);
    }

    final void visitEachUse(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, uses, OperandMode.USE, __proc);
    }

    final void visitEachAlive(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, alives, OperandMode.ALIVE, __proc);
    }

    final void visitEachTemp(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, temps, OperandMode.TEMP, __proc);
    }

    final void visitEachDef(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, defs, OperandMode.DEF, __proc);
    }

    final Value forEachRegisterHint(LIRInstruction __obj, OperandMode __mode, InstructionValueProcedure __proc)
    {
        Values __hints;
        if (__mode == OperandMode.USE)
        {
            __hints = defs;
        }
        else if (__mode == OperandMode.DEF)
        {
            __hints = uses;
        }
        else
        {
            return null;
        }

        for (int __i = 0; __i < __hints.getCount(); __i++)
        {
            if (__i < __hints.getDirectCount())
            {
                Value __hintValue = __hints.getValue(__obj, __i);
                Value __result = __proc.doValue(__obj, __hintValue, null, null);
                if (__result != null)
                {
                    return __result;
                }
            }
            else
            {
                Value[] __hintValues = __hints.getValueArray(__obj, __i);
                for (int __j = 0; __j < __hintValues.length; __j++)
                {
                    Value __hintValue = __hintValues[__j];
                    Value __result = __proc.doValue(__obj, __hintValue, null, null);
                    if (__result != null)
                    {
                        return __result;
                    }
                }
            }
        }
        return null;
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
