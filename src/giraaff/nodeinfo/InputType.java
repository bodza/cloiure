package giraaff.nodeinfo;

// @enum InputType
public enum InputType
{
    /**
     * Inputs that consume an actual value generated by the referenced node.
     */
    Value,
    /**
     * Inputs that consume the memory state of the referenced node.
     */
    Memory,
    /**
     * Inputs that reference a condition.
     */
    Condition,
    /**
     * Inputs that reference a frame state.
     */
    State,
    /**
     * Inputs that reference a guard (guards, begin nodes).
     */
    Guard,
    /**
     * Inputs that reference an anchor (begin nodes, value anchors).
     */
    Anchor,
    /**
     * Inputs that represent an association between nodes, e.g. a phi and the merge or a loop begin
     * and loop exits and ends.
     */
    Association,
    /**
     * Inputs that connect tightly coupled nodes, e.g. an InvokeNode and its CallTargetNode.
     */
    Extension,
    /**
     * Inputs of this type are temporarily exempt from type checking. This should only be used in
     * exceptional cases and should never survive to later stages of compilation.
     */
    Unchecked
}
