package cloiure.lang;

public interface IPersistentStack extends IPersistentCollection
{
    Object peek();

    IPersistentStack pop();
}
