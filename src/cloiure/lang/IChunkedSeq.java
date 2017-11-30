package cloiure.lang;

public interface IChunkedSeq extends ISeq, Sequential
{
    IChunk chunkedFirst();

    ISeq chunkedNext();

    ISeq chunkedMore();
}
