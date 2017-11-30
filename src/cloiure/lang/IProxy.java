package cloiure.lang;

public interface IProxy
{
    public void __initCloiureFnMappings(IPersistentMap m);
    public void __updateCloiureFnMappings(IPersistentMap m);
    public IPersistentMap __getCloiureFnMappings();
}
