package giraaff.lir.gen;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp.SaveRegistersOp;

// @iface DiagnosticLIRGeneratorTool
public interface DiagnosticLIRGeneratorTool
{
    ///
    // Creates a {@link SaveRegistersOp} that fills a given set of registers with known garbage value.
    //
    // The set of registers actually touched might be {@link SaveRegistersOp#remove reduced} later.
    //
    // @param zappedRegisters registers to be zapped
    // @param zapValues values used for zapping
    //
    // @see DiagnosticLIRGeneratorTool#createZapRegisters()
    ///
    SaveRegistersOp createZapRegisters(Register[] __zappedRegisters, JavaConstant[] __zapValues);

    ///
    // Creates a {@link SaveRegistersOp} that fills all
    // {@link RegisterConfig#getAllocatableRegisters() allocatable registers} with a
    // {@link LIRGenerator#zapValueForKind known garbage value}.
    //
    // The set of registers actually touched might be {@link SaveRegistersOp#remove reduced} later.
    //
    // @see DiagnosticLIRGeneratorTool#createZapRegisters(Register[], JavaConstant[])
    ///
    SaveRegistersOp createZapRegisters();

    ///
    // Marker interface for {@link LIRInstruction instructions} that should be succeeded with a
    // {@link DiagnosticLIRGeneratorTool#createZapRegisters() ZapRegisterOp} if assertions are enabled.
    ///
    // @iface DiagnosticLIRGeneratorTool.ZapRegistersAfterInstruction
    interface ZapRegistersAfterInstruction
    {
    }

    ///
    // Marker interface for {@link LIRInstruction instructions} that should be preceded with a
    // {@link DiagnosticLIRGeneratorTool#zapArgumentSpace ZapArgumentSpaceOp} if assertions are enabled.
    ///
    // @iface DiagnosticLIRGeneratorTool.ZapStackArgumentSpaceBeforeInstruction
    interface ZapStackArgumentSpaceBeforeInstruction
    {
    }

    LIRInstruction createZapArgumentSpace(StackSlot[] __zappedStack, JavaConstant[] __zapValues);

    LIRInstruction zapArgumentSpace();
}
