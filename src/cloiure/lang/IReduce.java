package cloiure.lang;

public interface IReduce extends IReduceInit
{
    Object reduce(IFn f);
}
