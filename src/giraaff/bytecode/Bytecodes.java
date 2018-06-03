package giraaff.bytecode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import giraaff.bytecode.Bytecodes.Flags;

///
// Definitions of the standard Java bytecodes defined by
// <a href= "http://java.sun.com/docs/books/jvms/second_edition/html/VMSpecTOC.doc.html"> Java Virtual Machine Specification</a>.
///
// @class Bytecodes
public final class Bytecodes
{
    // @cons
    private Bytecodes()
    {
        super();
    }

    // @defs
    public static final int
        NOP             =   0, // 0x00
        ACONST_NULL     =   1, // 0x01
        ICONST_M1       =   2, // 0x02
        ICONST_0        =   3, // 0x03
        ICONST_1        =   4, // 0x04
        ICONST_2        =   5, // 0x05
        ICONST_3        =   6, // 0x06
        ICONST_4        =   7, // 0x07
        ICONST_5        =   8, // 0x08
        LCONST_0        =   9, // 0x09
        LCONST_1        =  10, // 0x0A
        FCONST_0        =  11, // 0x0B
        FCONST_1        =  12, // 0x0C
        FCONST_2        =  13, // 0x0D
        DCONST_0        =  14, // 0x0E
        DCONST_1        =  15, // 0x0F
        BIPUSH          =  16, // 0x10
        SIPUSH          =  17, // 0x11
        LDC             =  18, // 0x12
        LDC_W           =  19, // 0x13
        LDC2_W          =  20, // 0x14
        ILOAD           =  21, // 0x15
        LLOAD           =  22, // 0x16
        FLOAD           =  23, // 0x17
        DLOAD           =  24, // 0x18
        ALOAD           =  25, // 0x19
        ILOAD_0         =  26, // 0x1A
        ILOAD_1         =  27, // 0x1B
        ILOAD_2         =  28, // 0x1C
        ILOAD_3         =  29, // 0x1D
        LLOAD_0         =  30, // 0x1E
        LLOAD_1         =  31, // 0x1F
        LLOAD_2         =  32, // 0x20
        LLOAD_3         =  33, // 0x21
        FLOAD_0         =  34, // 0x22
        FLOAD_1         =  35, // 0x23
        FLOAD_2         =  36, // 0x24
        FLOAD_3         =  37, // 0x25
        DLOAD_0         =  38, // 0x26
        DLOAD_1         =  39, // 0x27
        DLOAD_2         =  40, // 0x28
        DLOAD_3         =  41, // 0x29
        ALOAD_0         =  42, // 0x2A
        ALOAD_1         =  43, // 0x2B
        ALOAD_2         =  44, // 0x2C
        ALOAD_3         =  45, // 0x2D
        IALOAD          =  46, // 0x2E
        LALOAD          =  47, // 0x2F
        FALOAD          =  48, // 0x30
        DALOAD          =  49, // 0x31
        AALOAD          =  50, // 0x32
        BALOAD          =  51, // 0x33
        CALOAD          =  52, // 0x34
        SALOAD          =  53, // 0x35
        ISTORE          =  54, // 0x36
        LSTORE          =  55, // 0x37
        FSTORE          =  56, // 0x38
        DSTORE          =  57, // 0x39
        ASTORE          =  58, // 0x3A
        ISTORE_0        =  59, // 0x3B
        ISTORE_1        =  60, // 0x3C
        ISTORE_2        =  61, // 0x3D
        ISTORE_3        =  62, // 0x3E
        LSTORE_0        =  63, // 0x3F
        LSTORE_1        =  64, // 0x40
        LSTORE_2        =  65, // 0x41
        LSTORE_3        =  66, // 0x42
        FSTORE_0        =  67, // 0x43
        FSTORE_1        =  68, // 0x44
        FSTORE_2        =  69, // 0x45
        FSTORE_3        =  70, // 0x46
        DSTORE_0        =  71, // 0x47
        DSTORE_1        =  72, // 0x48
        DSTORE_2        =  73, // 0x49
        DSTORE_3        =  74, // 0x4A
        ASTORE_0        =  75, // 0x4B
        ASTORE_1        =  76, // 0x4C
        ASTORE_2        =  77, // 0x4D
        ASTORE_3        =  78, // 0x4E
        IASTORE         =  79, // 0x4F
        LASTORE         =  80, // 0x50
        FASTORE         =  81, // 0x51
        DASTORE         =  82, // 0x52
        AASTORE         =  83, // 0x53
        BASTORE         =  84, // 0x54
        CASTORE         =  85, // 0x55
        SASTORE         =  86, // 0x56
        POP             =  87, // 0x57
        POP2            =  88, // 0x58
        DUP             =  89, // 0x59
        DUP_X1          =  90, // 0x5A
        DUP_X2          =  91, // 0x5B
        DUP2            =  92, // 0x5C
        DUP2_X1         =  93, // 0x5D
        DUP2_X2         =  94, // 0x5E
        SWAP            =  95, // 0x5F
        IADD            =  96, // 0x60
        LADD            =  97, // 0x61
        FADD            =  98, // 0x62
        DADD            =  99, // 0x63
        ISUB            = 100, // 0x64
        LSUB            = 101, // 0x65
        FSUB            = 102, // 0x66
        DSUB            = 103, // 0x67
        IMUL            = 104, // 0x68
        LMUL            = 105, // 0x69
        FMUL            = 106, // 0x6A
        DMUL            = 107, // 0x6B
        IDIV            = 108, // 0x6C
        LDIV            = 109, // 0x6D
        FDIV            = 110, // 0x6E
        DDIV            = 111, // 0x6F
        IREM            = 112, // 0x70
        LREM            = 113, // 0x71
        FREM            = 114, // 0x72
        DREM            = 115, // 0x73
        INEG            = 116, // 0x74
        LNEG            = 117, // 0x75
        FNEG            = 118, // 0x76
        DNEG            = 119, // 0x77
        ISHL            = 120, // 0x78
        LSHL            = 121, // 0x79
        ISHR            = 122, // 0x7A
        LSHR            = 123, // 0x7B
        IUSHR           = 124, // 0x7C
        LUSHR           = 125, // 0x7D
        IAND            = 126, // 0x7E
        LAND            = 127, // 0x7F
        IOR             = 128, // 0x80
        LOR             = 129, // 0x81
        IXOR            = 130, // 0x82
        LXOR            = 131, // 0x83
        IINC            = 132, // 0x84
        I2L             = 133, // 0x85
        I2F             = 134, // 0x86
        I2D             = 135, // 0x87
        L2I             = 136, // 0x88
        L2F             = 137, // 0x89
        L2D             = 138, // 0x8A
        F2I             = 139, // 0x8B
        F2L             = 140, // 0x8C
        F2D             = 141, // 0x8D
        D2I             = 142, // 0x8E
        D2L             = 143, // 0x8F
        D2F             = 144, // 0x90
        I2B             = 145, // 0x91
        I2C             = 146, // 0x92
        I2S             = 147, // 0x93
        LCMP            = 148, // 0x94
        FCMPL           = 149, // 0x95
        FCMPG           = 150, // 0x96
        DCMPL           = 151, // 0x97
        DCMPG           = 152, // 0x98
        IFEQ            = 153, // 0x99
        IFNE            = 154, // 0x9A
        IFLT            = 155, // 0x9B
        IFGE            = 156, // 0x9C
        IFGT            = 157, // 0x9D
        IFLE            = 158, // 0x9E
        IF_ICMPEQ       = 159, // 0x9F
        IF_ICMPNE       = 160, // 0xA0
        IF_ICMPLT       = 161, // 0xA1
        IF_ICMPGE       = 162, // 0xA2
        IF_ICMPGT       = 163, // 0xA3
        IF_ICMPLE       = 164, // 0xA4
        IF_ACMPEQ       = 165, // 0xA5
        IF_ACMPNE       = 166, // 0xA6
        GOTO            = 167, // 0xA7
        JSR             = 168, // 0xA8
        RET             = 169, // 0xA9
        TABLESWITCH     = 170, // 0xAA
        LOOKUPSWITCH    = 171, // 0xAB
        IRETURN         = 172, // 0xAC
        LRETURN         = 173, // 0xAD
        FRETURN         = 174, // 0xAE
        DRETURN         = 175, // 0xAF
        ARETURN         = 176, // 0xB0
        RETURN          = 177, // 0xB1
        GETSTATIC       = 178, // 0xB2
        PUTSTATIC       = 179, // 0xB3
        GETFIELD        = 180, // 0xB4
        PUTFIELD        = 181, // 0xB5
        INVOKEVIRTUAL   = 182, // 0xB6
        INVOKESPECIAL   = 183, // 0xB7
        INVOKESTATIC    = 184, // 0xB8
        INVOKEINTERFACE = 185, // 0xB9
        INVOKEDYNAMIC   = 186, // 0xBA
        NEW             = 187, // 0xBB
        NEWARRAY        = 188, // 0xBC
        ANEWARRAY       = 189, // 0xBD
        ARRAYLENGTH     = 190, // 0xBE
        ATHROW          = 191, // 0xBF
        CHECKCAST       = 192, // 0xC0
        INSTANCEOF      = 193, // 0xC1
        MONITORENTER    = 194, // 0xC2
        MONITOREXIT     = 195, // 0xC3
        WIDE            = 196, // 0xC4
        MULTIANEWARRAY  = 197, // 0xC5
        IFNULL          = 198, // 0xC6
        IFNONNULL       = 199, // 0xC7
        GOTO_W          = 200, // 0xC8
        JSR_W           = 201, // 0xC9

        ILLEGAL         = 255,
        END             = 256;

    ///
    // The last opcode defined by the JVM specification. To iterate over all JVM bytecodes:
    //
    // <pre>
    // for (int opcode = 0; opcode &lt;= Bytecodes.LAST_JVM_OPCODE; ++opcode) {
    // }
    // </pre>
    ///
    // @def
    public static final int LAST_JVM_OPCODE = JSR_W;

    ///
    // A collection of flags describing various bytecode attributes.
    ///
    // @class Bytecodes.Flags
    static final class Flags
    {
        ///
        // Denotes an instruction that ends a basic block and does not let control flow fall through
        // to its lexical successor.
        ///
        // @def
        static final int STOP = 0x00000001;

        ///
        // Denotes an instruction that ends a basic block and may let control flow fall through to
        // its lexical successor. In practice this means it is a conditional branch.
        ///
        // @def
        static final int FALL_THROUGH = 0x00000002;

        ///
        // Denotes an instruction that has a 2 or 4 byte operand that is an offset to another
        // instruction in the same method. This does not include the {@link Bytecodes#TABLESWITCH}
        // or {@link Bytecodes#LOOKUPSWITCH} instructions.
        ///
        // @def
        static final int BRANCH = 0x00000004;

        ///
        // Denotes an instruction that reads the value of a static or instance field.
        ///
        // @def
        static final int FIELD_READ = 0x00000008;

        ///
        // Denotes an instruction that writes the value of a static or instance field.
        ///
        // @def
        static final int FIELD_WRITE = 0x00000010;

        ///
        // Denotes an instruction that can cause a trap.
        ///
        // @def
        static final int TRAP = 0x00000080;
        ///
        // Denotes an instruction that is commutative.
        ///
        // @def
        static final int COMMUTATIVE = 0x00000100;
        ///
        // Denotes an instruction that is associative.
        ///
        // @def
        static final int ASSOCIATIVE = 0x00000200;
        ///
        // Denotes an instruction that loads an operand.
        ///
        // @def
        static final int LOAD = 0x00000400;
        ///
        // Denotes an instruction that stores an operand.
        ///
        // @def
        static final int STORE = 0x00000800;
        ///
        // Denotes the 4 INVOKE* instructions.
        ///
        // @def
        static final int INVOKE = 0x00001000;
    }

    // Performs a sanity check that none of the flags overlap.
    static
    {
        int __allFlags = 0;
        try
        {
            for (Field __field : Flags.class.getDeclaredFields())
            {
                int __flagsFilter = Modifier.FINAL | Modifier.STATIC;
                if ((__field.getModifiers() & __flagsFilter) == __flagsFilter && !__field.isSynthetic())
                {
                    final int __flag = __field.getInt(null);
                    __allFlags |= __flag;
                }
            }
        }
        catch (Exception __e)
        {
            throw new InternalError(__e);
        }
    }

    ///
    // An array that maps from a bytecode value to a {@link String} for the corresponding
    // instruction mnemonic.
    ///
    // @def
    private static final String[] nameArray = new String[256];

    ///
    // An array that maps from a bytecode value to the set of {@link Flags} for the corresponding instruction.
    ///
    // @def
    private static final int[] flagsArray = new int[256];

    ///
    // An array that maps from a bytecode value to the length in bytes for the corresponding instruction.
    ///
    // @def
    private static final int[] lengthArray = new int[256];

    ///
    // An array that maps from a bytecode value to the number of slots pushed on the stack by the
    // corresponding instruction.
    ///
    // @def
    private static final int[] stackEffectArray = new int[256];

    static
    {
        def(NOP             , "nop"             , "b"    ,  0);
        def(ACONST_NULL     , "aconst_null"     , "b"    ,  1);
        def(ICONST_M1       , "iconst_m1"       , "b"    ,  1);
        def(ICONST_0        , "iconst_0"        , "b"    ,  1);
        def(ICONST_1        , "iconst_1"        , "b"    ,  1);
        def(ICONST_2        , "iconst_2"        , "b"    ,  1);
        def(ICONST_3        , "iconst_3"        , "b"    ,  1);
        def(ICONST_4        , "iconst_4"        , "b"    ,  1);
        def(ICONST_5        , "iconst_5"        , "b"    ,  1);
        def(LCONST_0        , "lconst_0"        , "b"    ,  2);
        def(LCONST_1        , "lconst_1"        , "b"    ,  2);
        def(FCONST_0        , "fconst_0"        , "b"    ,  1);
        def(FCONST_1        , "fconst_1"        , "b"    ,  1);
        def(FCONST_2        , "fconst_2"        , "b"    ,  1);
        def(DCONST_0        , "dconst_0"        , "b"    ,  2);
        def(DCONST_1        , "dconst_1"        , "b"    ,  2);
        def(BIPUSH          , "bipush"          , "bc"   ,  1);
        def(SIPUSH          , "sipush"          , "bcc"  ,  1);
        def(LDC             , "ldc"             , "bi"   ,  1, Flags.TRAP);
        def(LDC_W           , "ldc_w"           , "bii"  ,  1, Flags.TRAP);
        def(LDC2_W          , "ldc2_w"          , "bii"  ,  2, Flags.TRAP);
        def(ILOAD           , "iload"           , "bi"   ,  1, Flags.LOAD);
        def(LLOAD           , "lload"           , "bi"   ,  2, Flags.LOAD);
        def(FLOAD           , "fload"           , "bi"   ,  1, Flags.LOAD);
        def(DLOAD           , "dload"           , "bi"   ,  2, Flags.LOAD);
        def(ALOAD           , "aload"           , "bi"   ,  1, Flags.LOAD);
        def(ILOAD_0         , "iload_0"         , "b"    ,  1, Flags.LOAD);
        def(ILOAD_1         , "iload_1"         , "b"    ,  1, Flags.LOAD);
        def(ILOAD_2         , "iload_2"         , "b"    ,  1, Flags.LOAD);
        def(ILOAD_3         , "iload_3"         , "b"    ,  1, Flags.LOAD);
        def(LLOAD_0         , "lload_0"         , "b"    ,  2, Flags.LOAD);
        def(LLOAD_1         , "lload_1"         , "b"    ,  2, Flags.LOAD);
        def(LLOAD_2         , "lload_2"         , "b"    ,  2, Flags.LOAD);
        def(LLOAD_3         , "lload_3"         , "b"    ,  2, Flags.LOAD);
        def(FLOAD_0         , "fload_0"         , "b"    ,  1, Flags.LOAD);
        def(FLOAD_1         , "fload_1"         , "b"    ,  1, Flags.LOAD);
        def(FLOAD_2         , "fload_2"         , "b"    ,  1, Flags.LOAD);
        def(FLOAD_3         , "fload_3"         , "b"    ,  1, Flags.LOAD);
        def(DLOAD_0         , "dload_0"         , "b"    ,  2, Flags.LOAD);
        def(DLOAD_1         , "dload_1"         , "b"    ,  2, Flags.LOAD);
        def(DLOAD_2         , "dload_2"         , "b"    ,  2, Flags.LOAD);
        def(DLOAD_3         , "dload_3"         , "b"    ,  2, Flags.LOAD);
        def(ALOAD_0         , "aload_0"         , "b"    ,  1, Flags.LOAD);
        def(ALOAD_1         , "aload_1"         , "b"    ,  1, Flags.LOAD);
        def(ALOAD_2         , "aload_2"         , "b"    ,  1, Flags.LOAD);
        def(ALOAD_3         , "aload_3"         , "b"    ,  1, Flags.LOAD);
        def(IALOAD          , "iaload"          , "b"    , -1, Flags.TRAP);
        def(LALOAD          , "laload"          , "b"    ,  0, Flags.TRAP);
        def(FALOAD          , "faload"          , "b"    , -1, Flags.TRAP);
        def(DALOAD          , "daload"          , "b"    ,  0, Flags.TRAP);
        def(AALOAD          , "aaload"          , "b"    , -1, Flags.TRAP);
        def(BALOAD          , "baload"          , "b"    , -1, Flags.TRAP);
        def(CALOAD          , "caload"          , "b"    , -1, Flags.TRAP);
        def(SALOAD          , "saload"          , "b"    , -1, Flags.TRAP);
        def(ISTORE          , "istore"          , "bi"   , -1, Flags.STORE);
        def(LSTORE          , "lstore"          , "bi"   , -2, Flags.STORE);
        def(FSTORE          , "fstore"          , "bi"   , -1, Flags.STORE);
        def(DSTORE          , "dstore"          , "bi"   , -2, Flags.STORE);
        def(ASTORE          , "astore"          , "bi"   , -1, Flags.STORE);
        def(ISTORE_0        , "istore_0"        , "b"    , -1, Flags.STORE);
        def(ISTORE_1        , "istore_1"        , "b"    , -1, Flags.STORE);
        def(ISTORE_2        , "istore_2"        , "b"    , -1, Flags.STORE);
        def(ISTORE_3        , "istore_3"        , "b"    , -1, Flags.STORE);
        def(LSTORE_0        , "lstore_0"        , "b"    , -2, Flags.STORE);
        def(LSTORE_1        , "lstore_1"        , "b"    , -2, Flags.STORE);
        def(LSTORE_2        , "lstore_2"        , "b"    , -2, Flags.STORE);
        def(LSTORE_3        , "lstore_3"        , "b"    , -2, Flags.STORE);
        def(FSTORE_0        , "fstore_0"        , "b"    , -1, Flags.STORE);
        def(FSTORE_1        , "fstore_1"        , "b"    , -1, Flags.STORE);
        def(FSTORE_2        , "fstore_2"        , "b"    , -1, Flags.STORE);
        def(FSTORE_3        , "fstore_3"        , "b"    , -1, Flags.STORE);
        def(DSTORE_0        , "dstore_0"        , "b"    , -2, Flags.STORE);
        def(DSTORE_1        , "dstore_1"        , "b"    , -2, Flags.STORE);
        def(DSTORE_2        , "dstore_2"        , "b"    , -2, Flags.STORE);
        def(DSTORE_3        , "dstore_3"        , "b"    , -2, Flags.STORE);
        def(ASTORE_0        , "astore_0"        , "b"    , -1, Flags.STORE);
        def(ASTORE_1        , "astore_1"        , "b"    , -1, Flags.STORE);
        def(ASTORE_2        , "astore_2"        , "b"    , -1, Flags.STORE);
        def(ASTORE_3        , "astore_3"        , "b"    , -1, Flags.STORE);
        def(IASTORE         , "iastore"         , "b"    , -3, Flags.TRAP);
        def(LASTORE         , "lastore"         , "b"    , -4, Flags.TRAP);
        def(FASTORE         , "fastore"         , "b"    , -3, Flags.TRAP);
        def(DASTORE         , "dastore"         , "b"    , -4, Flags.TRAP);
        def(AASTORE         , "aastore"         , "b"    , -3, Flags.TRAP);
        def(BASTORE         , "bastore"         , "b"    , -3, Flags.TRAP);
        def(CASTORE         , "castore"         , "b"    , -3, Flags.TRAP);
        def(SASTORE         , "sastore"         , "b"    , -3, Flags.TRAP);
        def(POP             , "pop"             , "b"    , -1);
        def(POP2            , "pop2"            , "b"    , -2);
        def(DUP             , "dup"             , "b"    ,  1);
        def(DUP_X1          , "dup_x1"          , "b"    ,  1);
        def(DUP_X2          , "dup_x2"          , "b"    ,  1);
        def(DUP2            , "dup2"            , "b"    ,  2);
        def(DUP2_X1         , "dup2_x1"         , "b"    ,  2);
        def(DUP2_X2         , "dup2_x2"         , "b"    ,  2);
        def(SWAP            , "swap"            , "b"    ,  0);
        def(IADD            , "iadd"            , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(LADD            , "ladd"            , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(FADD            , "fadd"            , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(DADD            , "dadd"            , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(ISUB            , "isub"            , "b"    , -1);
        def(LSUB            , "lsub"            , "b"    , -2);
        def(FSUB            , "fsub"            , "b"    , -1);
        def(DSUB            , "dsub"            , "b"    , -2);
        def(IMUL            , "imul"            , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(LMUL            , "lmul"            , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(FMUL            , "fmul"            , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(DMUL            , "dmul"            , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(IDIV            , "idiv"            , "b"    , -1, Flags.TRAP);
        def(LDIV            , "ldiv"            , "b"    , -2, Flags.TRAP);
        def(FDIV            , "fdiv"            , "b"    , -1);
        def(DDIV            , "ddiv"            , "b"    , -2);
        def(IREM            , "irem"            , "b"    , -1, Flags.TRAP);
        def(LREM            , "lrem"            , "b"    , -2, Flags.TRAP);
        def(FREM            , "frem"            , "b"    , -1);
        def(DREM            , "drem"            , "b"    , -2);
        def(INEG            , "ineg"            , "b"    ,  0);
        def(LNEG            , "lneg"            , "b"    ,  0);
        def(FNEG            , "fneg"            , "b"    ,  0);
        def(DNEG            , "dneg"            , "b"    ,  0);
        def(ISHL            , "ishl"            , "b"    , -1);
        def(LSHL            , "lshl"            , "b"    , -1);
        def(ISHR            , "ishr"            , "b"    , -1);
        def(LSHR            , "lshr"            , "b"    , -1);
        def(IUSHR           , "iushr"           , "b"    , -1);
        def(LUSHR           , "lushr"           , "b"    , -1);
        def(IAND            , "iand"            , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(LAND            , "land"            , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(IOR             , "ior"             , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(LOR             , "lor"             , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(IXOR            , "ixor"            , "b"    , -1, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(LXOR            , "lxor"            , "b"    , -2, Flags.COMMUTATIVE | Flags.ASSOCIATIVE);
        def(IINC            , "iinc"            , "bic"  ,  0, Flags.LOAD | Flags.STORE);
        def(I2L             , "i2l"             , "b"    ,  1);
        def(I2F             , "i2f"             , "b"    ,  0);
        def(I2D             , "i2d"             , "b"    ,  1);
        def(L2I             , "l2i"             , "b"    , -1);
        def(L2F             , "l2f"             , "b"    , -1);
        def(L2D             , "l2d"             , "b"    ,  0);
        def(F2I             , "f2i"             , "b"    ,  0);
        def(F2L             , "f2l"             , "b"    ,  1);
        def(F2D             , "f2d"             , "b"    ,  1);
        def(D2I             , "d2i"             , "b"    , -1);
        def(D2L             , "d2l"             , "b"    ,  0);
        def(D2F             , "d2f"             , "b"    , -1);
        def(I2B             , "i2b"             , "b"    ,  0);
        def(I2C             , "i2c"             , "b"    ,  0);
        def(I2S             , "i2s"             , "b"    ,  0);
        def(LCMP            , "lcmp"            , "b"    , -3);
        def(FCMPL           , "fcmpl"           , "b"    , -1);
        def(FCMPG           , "fcmpg"           , "b"    , -1);
        def(DCMPL           , "dcmpl"           , "b"    , -3);
        def(DCMPG           , "dcmpg"           , "b"    , -3);
        def(IFEQ            , "ifeq"            , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IFNE            , "ifne"            , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IFLT            , "iflt"            , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IFGE            , "ifge"            , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IFGT            , "ifgt"            , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IFLE            , "ifle"            , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ICMPEQ       , "if_icmpeq"       , "boo"  , -2, Flags.COMMUTATIVE | Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ICMPNE       , "if_icmpne"       , "boo"  , -2, Flags.COMMUTATIVE | Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ICMPLT       , "if_icmplt"       , "boo"  , -2, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ICMPGE       , "if_icmpge"       , "boo"  , -2, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ICMPGT       , "if_icmpgt"       , "boo"  , -2, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ICMPLE       , "if_icmple"       , "boo"  , -2, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ACMPEQ       , "if_acmpeq"       , "boo"  , -2, Flags.COMMUTATIVE | Flags.FALL_THROUGH | Flags.BRANCH);
        def(IF_ACMPNE       , "if_acmpne"       , "boo"  , -2, Flags.COMMUTATIVE | Flags.FALL_THROUGH | Flags.BRANCH);
        def(GOTO            , "goto"            , "boo"  ,  0, Flags.STOP | Flags.BRANCH);
        def(JSR             , "jsr"             , "boo"  ,  0, Flags.STOP | Flags.BRANCH);
        def(RET             , "ret"             , "bi"   ,  0, Flags.STOP);
        def(TABLESWITCH     , "tableswitch"     , ""     , -1, Flags.STOP);
        def(LOOKUPSWITCH    , "lookupswitch"    , ""     , -1, Flags.STOP);
        def(IRETURN         , "ireturn"         , "b"    , -1, Flags.TRAP | Flags.STOP);
        def(LRETURN         , "lreturn"         , "b"    , -2, Flags.TRAP | Flags.STOP);
        def(FRETURN         , "freturn"         , "b"    , -1, Flags.TRAP | Flags.STOP);
        def(DRETURN         , "dreturn"         , "b"    , -2, Flags.TRAP | Flags.STOP);
        def(ARETURN         , "areturn"         , "b"    , -1, Flags.TRAP | Flags.STOP);
        def(RETURN          , "return"          , "b"    ,  0, Flags.TRAP | Flags.STOP);
        def(GETSTATIC       , "getstatic"       , "bjj"  ,  1, Flags.TRAP | Flags.FIELD_READ);
        def(PUTSTATIC       , "putstatic"       , "bjj"  , -1, Flags.TRAP | Flags.FIELD_WRITE);
        def(GETFIELD        , "getfield"        , "bjj"  ,  0, Flags.TRAP | Flags.FIELD_READ);
        def(PUTFIELD        , "putfield"        , "bjj"  , -2, Flags.TRAP | Flags.FIELD_WRITE);
        def(INVOKEVIRTUAL   , "invokevirtual"   , "bjj"  , -1, Flags.TRAP | Flags.INVOKE);
        def(INVOKESPECIAL   , "invokespecial"   , "bjj"  , -1, Flags.TRAP | Flags.INVOKE);
        def(INVOKESTATIC    , "invokestatic"    , "bjj"  ,  0, Flags.TRAP | Flags.INVOKE);
        def(INVOKEINTERFACE , "invokeinterface" , "bjja_", -1, Flags.TRAP | Flags.INVOKE);
        def(INVOKEDYNAMIC   , "invokedynamic"   , "bjjjj",  0, Flags.TRAP | Flags.INVOKE);
        def(NEW             , "new"             , "bii"  ,  1, Flags.TRAP);
        def(NEWARRAY        , "newarray"        , "bc"   ,  0, Flags.TRAP);
        def(ANEWARRAY       , "anewarray"       , "bii"  ,  0, Flags.TRAP);
        def(ARRAYLENGTH     , "arraylength"     , "b"    ,  0, Flags.TRAP);
        def(ATHROW          , "athrow"          , "b"    , -1, Flags.TRAP | Flags.STOP);
        def(CHECKCAST       , "checkcast"       , "bii"  ,  0, Flags.TRAP);
        def(INSTANCEOF      , "instanceof"      , "bii"  ,  0, Flags.TRAP);
        def(MONITORENTER    , "monitorenter"    , "b"    , -1, Flags.TRAP);
        def(MONITOREXIT     , "monitorexit"     , "b"    , -1, Flags.TRAP);
        def(WIDE            , "wide"            , ""     ,  0);
        def(MULTIANEWARRAY  , "multianewarray"  , "biic" ,  1, Flags.TRAP);
        def(IFNULL          , "ifnull"          , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(IFNONNULL       , "ifnonnull"       , "boo"  , -1, Flags.FALL_THROUGH | Flags.BRANCH);
        def(GOTO_W          , "goto_w"          , "boooo",  0, Flags.STOP | Flags.BRANCH);
        def(JSR_W           , "jsr_w"           , "boooo",  0, Flags.STOP | Flags.BRANCH);
    }

    ///
    // Determines if an opcode is commutative.
    //
    // @param opcode the opcode to check
    // @return {@code true} iff commutative
    ///
    public static boolean isCommutative(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.COMMUTATIVE) != 0;
    }

    ///
    // Gets the length of an instruction denoted by a given opcode.
    //
    // @param opcode an instruction opcode
    // @return the length of the instruction denoted by {@code opcode}. If {@code opcode} is an
    //         illegal instruction or denotes a variable length instruction (e.g.
    //         {@link #TABLESWITCH}), then 0 is returned.
    ///
    public static int lengthOf(int __opcode)
    {
        return lengthArray[__opcode & 0xff];
    }

    ///
    // Gets the effect on the depth of the expression stack of an instruction denoted by a given opcode.
    //
    // @param opcode an instruction opcode
    // @return the change in the stack caused by the instruction denoted by {@code opcode}. If
    //         {@code opcode} is an illegal instruction then 0 is returned. Note that invoke
    //         instructions may pop more arguments so this value is a minimum stack effect.
    ///
    public static int stackEffectOf(int __opcode)
    {
        return stackEffectArray[__opcode & 0xff];
    }

    ///
    // Gets the lower-case mnemonic for a given opcode.
    //
    // @param opcode an opcode
    // @return the mnemonic for {@code opcode} or {@code "<illegal opcode: " + opcode + ">"} if
    //         {@code opcode} is not a legal opcode
    ///
    public static String nameOf(int __opcode)
    {
        String __name = nameArray[__opcode & 0xff];
        if (__name == null)
        {
            return "<illegal opcode: " + __opcode + ">";
        }
        return __name;
    }

    ///
    // Allocation-free version of {@linkplain #nameOf(int)}.
    //
    // @param opcode an opcode.
    // @return the mnemonic for {@code opcode} or {@code "<illegal opcode>"} if {@code opcode} is
    //         not a legal opcode.
    ///
    public static String baseNameOf(int __opcode)
    {
        String __name = nameArray[__opcode & 0xff];
        if (__name == null)
        {
            return "<illegal opcode>";
        }
        return __name;
    }

    ///
    // Gets the opcode corresponding to a given mnemonic.
    //
    // @param name an opcode mnemonic
    // @return the opcode corresponding to {@code mnemonic}
    // @throws IllegalArgumentException if {@code name} does not denote a valid opcode
    ///
    public static int valueOf(String __name)
    {
        for (int __opcode = 0; __opcode < nameArray.length; ++__opcode)
        {
            if (__name.equalsIgnoreCase(nameArray[__opcode]))
            {
                return __opcode;
            }
        }
        throw new IllegalArgumentException("No opcode for " + __name);
    }

    ///
    // Determines if a given opcode denotes an instruction that can cause an implicit exception.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} can cause an implicit exception, {@code false} otherwise
    ///
    public static boolean canTrap(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.TRAP) != 0;
    }

    ///
    // Determines if a given opcode denotes an instruction that loads a local variable to the
    // operand stack.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} loads a local variable to the operand stack,
    //         {@code false} otherwise
    ///
    public static boolean isLoad(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.LOAD) != 0;
    }

    ///
    // Determines if a given opcode denotes an instruction that ends a basic block and does not let
    // control flow fall through to its lexical successor.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} properly ends a basic block
    ///
    public static boolean isStop(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.STOP) != 0;
    }

    ///
    // Determines if a given opcode denotes an instruction that stores a value to a local variable
    // after popping it from the operand stack.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} stores a value to a local variable, {@code false} otherwise
    ///
    public static boolean isInvoke(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.INVOKE) != 0;
    }

    ///
    // Determines if a given opcode denotes an instruction that stores a value to a local variable
    // after popping it from the operand stack.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} stores a value to a local variable, {@code false} otherwise
    ///
    public static boolean isStore(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.STORE) != 0;
    }

    ///
    // Determines if a given opcode is an instruction that delimits a basic block.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} delimits a basic block
    ///
    public static boolean isBlockEnd(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & (Flags.STOP | Flags.FALL_THROUGH)) != 0;
    }

    ///
    // Determines if a given opcode is an instruction that has a 2 or 4 byte operand that is an
    // offset to another instruction in the same method. This does not include the
    // {@linkplain #TABLESWITCH switch} instructions.
    //
    // @param opcode an opcode to test
    // @return {@code true} iff {@code opcode} is a branch instruction with a single operand
    ///
    public static boolean isBranch(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.BRANCH) != 0;
    }

    ///
    // Determines if a given opcode denotes a conditional branch.
    //
    // @return {@code true} iff {@code opcode} is a conditional branch
    ///
    public static boolean isConditionalBranch(int __opcode)
    {
        return (flagsArray[__opcode & 0xff] & Flags.FALL_THROUGH) != 0;
    }

    ///
    // Gets the arithmetic operator name for a given opcode. If {@code opcode} does not denote an
    // arithmetic instruction, then the {@linkplain #nameOf(int) name} of the opcode is returned instead.
    //
    // @param op an opcode
    // @return the arithmetic operator name
    ///
    public static String operator(int __op)
    {
        switch (__op)
        {
            // arithmetic ops
            case IADD: // fall through
            case LADD: // fall through
            case FADD: // fall through
            case DADD:
                return "+";
            case ISUB: // fall through
            case LSUB: // fall through
            case FSUB: // fall through
            case DSUB:
                return "-";
            case IMUL: // fall through
            case LMUL: // fall through
            case FMUL: // fall through
            case DMUL:
                return "*";
            case IDIV: // fall through
            case LDIV: // fall through
            case FDIV: // fall through
            case DDIV:
                return "/";
            case IREM: // fall through
            case LREM: // fall through
            case FREM: // fall through
            case DREM:
                return "%";
            // shift ops
            case ISHL: // fall through
            case LSHL:
                return "<<";
            case ISHR: // fall through
            case LSHR:
                return ">>";
            case IUSHR: // fall through
            case LUSHR:
                return ">>>";
            // logic ops
            case IAND: // fall through
            case LAND:
                return "&";
            case IOR: // fall through
            case LOR:
                return "|";
            case IXOR: // fall through
            case LXOR:
                return "^";
        }
        return nameOf(__op);
    }

    ///
    // Defines a bytecode by entering it into the arrays that record its name, length and flags.
    //
    // @param name instruction name (should be lower case)
    // @param format encodes the length of the instruction
    ///
    private static void def(int __opcode, String __name, String __format, int __stackEffect)
    {
        def(__opcode, __name, __format, __stackEffect, 0);
    }

    ///
    // Defines a bytecode by entering it into the arrays that record its name, length and flags.
    //
    // @param name instruction name (lower case)
    // @param format encodes the length of the instruction
    // @param flags the set of {@link Flags} associated with the instruction
    ///
    private static void def(int __opcode, String __name, String __format, int __stackEffect, int __flags)
    {
        nameArray[__opcode] = __name;
        int __instructionLength = __format.length();
        lengthArray[__opcode] = __instructionLength;
        stackEffectArray[__opcode] = __stackEffect;
        Bytecodes.flagsArray[__opcode] = __flags;
    }

    public static boolean isIfBytecode(int __bytecode)
    {
        switch (__bytecode)
        {
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case IFNULL:
            case IFNONNULL:
                return true;
        }
        return false;
    }
}
