package cloiure.lang;

public interface IKVReduce
{
    Object kvreduce(IFn f, Object init);
}
