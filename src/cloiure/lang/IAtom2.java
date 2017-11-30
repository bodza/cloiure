package cloiure.lang;

public interface IAtom2 extends IAtom
{
    IPersistentVector swapVals(IFn f);

    IPersistentVector swapVals(IFn f, Object arg);

    IPersistentVector swapVals(IFn f, Object arg1, Object arg2);

    IPersistentVector swapVals(IFn f, Object x, Object y, ISeq args);

    IPersistentVector resetVals(Object newv);
}
