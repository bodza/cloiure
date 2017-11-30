package cloiure.lang;

public interface IChunk extends Indexed
{
    IChunk dropFirst();

    Object reduce(IFn f, Object start);
}
