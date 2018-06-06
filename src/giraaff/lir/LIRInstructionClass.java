package giraaff.lir;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp;
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
    private final LIRIntrospection.Values ___uses;
    // @field
    private final LIRIntrospection.Values ___alives;
    // @field
    private final LIRIntrospection.Values ___temps;
    // @field
    private final LIRIntrospection.Values ___defs;

    // @field
    private final boolean ___isMoveOp;
    // @field
    private final boolean ___isValueMoveOp;
    // @field
    private final boolean ___isLoadConstantOp;

    // @field
    private String ___opcodeConstant;
    // @field
    private int ___opcodeIndex;

    // @cons LIRInstructionClass
    private LIRInstructionClass(Class<T> __clazz)
    {
        this(__clazz, new FieldsScanner.DefaultCalcOffset());
    }

    // @cons LIRInstructionClass
    public LIRInstructionClass(Class<T> __clazz, FieldsScanner.CalcOffset __calcOffset)
    {
        super(__clazz);

        LIRInstructionClass.LIRInstructionFieldsScanner __ifs = new LIRInstructionClass.LIRInstructionFieldsScanner(__calcOffset);
        __ifs.scan(__clazz);

        this.___uses = new LIRIntrospection.Values(__ifs.___valueAnnotations.get(LIRInstruction.Use.class));
        this.___alives = new LIRIntrospection.Values(__ifs.___valueAnnotations.get(LIRInstruction.Alive.class));
        this.___temps = new LIRIntrospection.Values(__ifs.___valueAnnotations.get(LIRInstruction.Temp.class));
        this.___defs = new LIRIntrospection.Values(__ifs.___valueAnnotations.get(LIRInstruction.Def.class));

        this.___data = new Fields(__ifs.___data);

        this.___opcodeConstant = __ifs.___opcodeConstant;
        if (__ifs.___opcodeField == null)
        {
            this.___opcodeIndex = -1;
        }
        else
        {
            this.___opcodeIndex = __ifs.___data.indexOf(__ifs.___opcodeField);
        }

        this.___isMoveOp = StandardOp.MoveOp.class.isAssignableFrom(__clazz);
        this.___isValueMoveOp = StandardOp.ValueMoveOp.class.isAssignableFrom(__clazz);
        this.___isLoadConstantOp = StandardOp.LoadConstantOp.class.isAssignableFrom(__clazz);
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
    private static final class LIRInstructionFieldsScanner extends LIRIntrospection.LIRFieldsScanner
    {
        // @field
        private String ___opcodeConstant;

        ///
        // Field (if any) annotated by {@link LIROpcode}.
        ///
        // @field
        private FieldsScanner.FieldInfo ___opcodeField;

        // @cons LIRInstructionClass.LIRInstructionFieldsScanner
        LIRInstructionFieldsScanner(FieldsScanner.CalcOffset __calc)
        {
            super(__calc);

            this.___valueAnnotations.put(LIRInstruction.Use.class, new LIRIntrospection.OperandModeAnnotation());
            this.___valueAnnotations.put(LIRInstruction.Alive.class, new LIRIntrospection.OperandModeAnnotation());
            this.___valueAnnotations.put(LIRInstruction.Temp.class, new LIRIntrospection.OperandModeAnnotation());
            this.___valueAnnotations.put(LIRInstruction.Def.class, new LIRIntrospection.OperandModeAnnotation());
        }

        @Override
        protected EnumSet<LIRInstruction.OperandFlag> getFlags(Field __field)
        {
            EnumSet<LIRInstruction.OperandFlag> __result = EnumSet.noneOf(LIRInstruction.OperandFlag.class);
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
            if (__clazz.getAnnotation(LIROpcode.class) != null)
            {
                this.___opcodeConstant = null;
            }
            this.___opcodeField = null;

            super.scan(__clazz, LIRInstruction.class, false);

            if (this.___opcodeConstant == null && this.___opcodeField == null)
            {
                this.___opcodeConstant = __clazz.getSimpleName();
                if (this.___opcodeConstant.endsWith("Op"))
                {
                    this.___opcodeConstant = this.___opcodeConstant.substring(0, this.___opcodeConstant.length() - 2);
                }
            }
        }

        @Override
        protected void scanField(Field __field, long __offset)
        {
            super.scanField(__field, __offset);

            if (__field.getAnnotation(LIROpcode.class) != null)
            {
                this.___opcodeField = this.___data.get(this.___data.size() - 1);
            }
        }
    }

    @Override
    public Fields[] getAllFields()
    {
        return new Fields[] { this.___data, this.___uses, this.___alives, this.___temps, this.___defs };
    }

    LIRIntrospection.Values getValues(LIRInstruction.OperandMode __mode)
    {
        switch (__mode)
        {
            case USE:
                return this.___uses;
            case ALIVE:
                return this.___alives;
            case TEMP:
                return this.___temps;
            case DEF:
                return this.___defs;
            default:
                throw GraalError.shouldNotReachHere("unknown LIRInstruction.OperandMode: " + __mode);
        }
    }

    final String getOpcode(LIRInstruction __obj)
    {
        if (this.___opcodeConstant != null)
        {
            return this.___opcodeConstant;
        }
        return String.valueOf(this.___data.getObject(__obj, this.___opcodeIndex));
    }

    final boolean hasOperands()
    {
        return this.___uses.getCount() > 0 || this.___alives.getCount() > 0 || this.___temps.getCount() > 0 || this.___defs.getCount() > 0;
    }

    final void forEachUse(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, this.___uses, LIRInstruction.OperandMode.USE, __proc);
    }

    final void forEachAlive(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, this.___alives, LIRInstruction.OperandMode.ALIVE, __proc);
    }

    final void forEachTemp(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, this.___temps, LIRInstruction.OperandMode.TEMP, __proc);
    }

    final void forEachDef(LIRInstruction __obj, InstructionValueProcedure __proc)
    {
        forEach(__obj, this.___defs, LIRInstruction.OperandMode.DEF, __proc);
    }

    final void visitEachUse(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, this.___uses, LIRInstruction.OperandMode.USE, __proc);
    }

    final void visitEachAlive(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, this.___alives, LIRInstruction.OperandMode.ALIVE, __proc);
    }

    final void visitEachTemp(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, this.___temps, LIRInstruction.OperandMode.TEMP, __proc);
    }

    final void visitEachDef(LIRInstruction __obj, InstructionValueConsumer __proc)
    {
        visitEach(__obj, this.___defs, LIRInstruction.OperandMode.DEF, __proc);
    }

    final Value forEachRegisterHint(LIRInstruction __obj, LIRInstruction.OperandMode __mode, InstructionValueProcedure __proc)
    {
        LIRIntrospection.Values __hints;
        if (__mode == LIRInstruction.OperandMode.USE)
        {
            __hints = this.___defs;
        }
        else if (__mode == LIRInstruction.OperandMode.DEF)
        {
            __hints = this.___uses;
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
        return this.___isMoveOp;
    }

    final boolean isValueMoveOp()
    {
        return this.___isValueMoveOp;
    }

    final boolean isLoadConstantOp()
    {
        return this.___isLoadConstantOp;
    }
}
