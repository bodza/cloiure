package cloiure.lang;

/**
 * Interface for exceptions that carry data (a map) as additional payload. Cloiure
 * programs that need richer semantics for exceptions should use this in lieu of
 * defining project-specific exception classes.
 */
public interface IExceptionInfo
{
    public IPersistentMap getData();
}
