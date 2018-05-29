package giraaff.core.common.cfg;

import java.util.function.BiConsumer;

// @iface PropertyConsumable
public interface PropertyConsumable
{
    void forEachProperty(BiConsumer<String, String> action);
}
