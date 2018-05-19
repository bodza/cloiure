package graalvm.compiler.hotspot;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.graalvm.collections.EconomicMap;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionDescriptors;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.options.OptionsParser;

import jdk.vm.ci.common.InitTimer;

/**
 * The {@link #HOTSPOT_OPTIONS} value contains the options values initialized in a HotSpot VM. The
 * values are set via system properties with the {@value #GRAAL_OPTION_PROPERTY_PREFIX} prefix.
 */
public class HotSpotGraalOptionValues
{
    /**
     * The name of the system property specifying a file containing extra Graal option settings.
     */
    private static final String GRAAL_OPTIONS_FILE_PROPERTY_NAME = "graal.options.file";

    /**
     * The name of the system property specifying the Graal version.
     */
    private static final String GRAAL_VERSION_PROPERTY_NAME = "graal.version";

    /**
     * The prefix for system properties that correspond to {@link Option} annotated fields. A field
     * named {@code MyOption} will have its value set from a system property with the name
     * {@code GRAAL_OPTION_PROPERTY_PREFIX + "MyOption"}.
     */
    public static final String GRAAL_OPTION_PROPERTY_PREFIX = "graal.";

    /**
     * Gets the system property assignment that would set the current value for a given option.
     */
    public static String asSystemPropertySetting(OptionValues options, OptionKey<?> value)
    {
        return GRAAL_OPTION_PROPERTY_PREFIX + value.getName() + "=" + value.getValue(options);
    }

    public static final OptionValues HOTSPOT_OPTIONS = initializeOptions();

    /**
     * Global options. The values for these options are initialized by parsing the file denoted by
     * the {@code VM.getSavedProperty(String) saved} system property named
     * {@value #GRAAL_OPTIONS_FILE_PROPERTY_NAME} if the file exists followed by parsing the options
     * encoded in saved system properties whose names start with
     * {@value #GRAAL_OPTION_PROPERTY_PREFIX}. Key/value pairs are parsed from the aforementioned
     * file with {@link Properties#load(java.io.Reader)}.
     */
    @SuppressWarnings("try")
    private static OptionValues initializeOptions()
    {
        EconomicMap<OptionKey<?>, Object> values = OptionValues.newOptionMap();
        Iterable<OptionDescriptors> loader = OptionsParser.getOptionsLoader();
        Map<String, String> savedProps = jdk.vm.ci.services.Services.getSavedProperties();
        String optionsFile = savedProps.get(GRAAL_OPTIONS_FILE_PROPERTY_NAME);

        if (optionsFile != null)
        {
            File graalOptions = new File(optionsFile);
            if (graalOptions.exists())
            {
                try (FileReader fr = new FileReader(graalOptions))
                {
                    Properties props = new Properties();
                    props.load(fr);
                    EconomicMap<String, String> optionSettings = EconomicMap.create();
                    for (Map.Entry<Object, Object> e : props.entrySet())
                    {
                        optionSettings.put((String) e.getKey(), (String) e.getValue());
                    }
                    try
                    {
                        OptionsParser.parseOptions(optionSettings, values, loader);
                    }
                    catch (Throwable e)
                    {
                        throw new InternalError("Error parsing an option from " + graalOptions, e);
                    }
                }
                catch (IOException e)
                {
                    throw new InternalError("Error reading " + graalOptions, e);
                }
            }
        }

        EconomicMap<String, String> optionSettings = EconomicMap.create();
        for (Map.Entry<String, String> e : savedProps.entrySet())
        {
            String name = e.getKey();
            if (name.startsWith(GRAAL_OPTION_PROPERTY_PREFIX))
            {
                if (name.equals("graal.PrintFlags") || name.equals("graal.ShowFlags"))
                {
                    System.err.println("The " + name + " option has been removed and will be ignored. Use -XX:+JVMCIPrintProperties instead.");
                }
                else if (name.equals(GRAAL_OPTIONS_FILE_PROPERTY_NAME) || name.equals(GRAAL_VERSION_PROPERTY_NAME))
                {
                    // Ignore well known properties that do not denote an option
                }
                else
                {
                    String value = e.getValue();
                    optionSettings.put(name.substring(GRAAL_OPTION_PROPERTY_PREFIX.length()), value);
                }
            }
        }

        OptionsParser.parseOptions(optionSettings, values, loader);
        return new OptionValues(values);
    }
}
