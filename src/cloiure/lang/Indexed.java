package cloiure.lang;

public interface Indexed extends Counted
{
    Object nth(int i);

    Object nth(int i, Object notFound);
}
