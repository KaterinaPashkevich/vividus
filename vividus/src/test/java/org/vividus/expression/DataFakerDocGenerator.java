package org.vividus.expression;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;

import net.datafaker.Faker;
import net.datafaker.providers.base.Address;
import net.datafaker.providers.base.Aviation;
import net.datafaker.providers.base.Compass;
import net.datafaker.providers.base.Domain;
import net.datafaker.providers.base.DrivingLicense;
import net.datafaker.providers.base.FakeDuration;
import net.datafaker.providers.base.File;
import net.datafaker.providers.base.Finance;
import net.datafaker.providers.base.Internet;
import net.datafaker.providers.base.Locality;
import net.datafaker.providers.base.ProviderRegistration;
import net.datafaker.providers.base.Unique;
import net.datafaker.providers.base.Vehicle;
import net.datafaker.sequence.FakeSequence;
import net.datafaker.service.FakerContext;
import net.datafaker.service.RandomService;

public class DataFakerDocGenerator
{
    private static final Faker FAKER = new Faker();

    public static void main(String[] args) throws IllegalAccessException
    {
        System.out.println("[%autowidth.stretch, cols=\".^~,~\"]");
        System.out.println("|===");
        System.out.println("|Expression `#{generate(<expression>)}`");
        System.out.println("|Result");

        List<Method> ignoredMethods = new ArrayList<>();

        for (Method providerMethod : getMethods(Faker.class, true))
        {
            Class<?> providerClass = providerMethod.getReturnType();
            if (providerClass == FakeSequence.Builder.class
                    || providerClass == FakeDuration.class
                    || providerClass == FakerContext.class
                    || providerClass == Locale.class
                    || providerClass == Locality.class
                    || providerClass == ProviderRegistration.class
                    || providerClass == RandomService.class
                    || providerClass == Unique.class)
            {
                continue;
            }
            String providerName = providerClass.getSimpleName();

            if ("Cannabis".equals(providerName) // Bad reputation
                    // These providers are ignored because they should be fixed on data-faker side
                    || "DateAndTime".equals(providerName)
                    || "Relationship".equals(providerName))
            {
                continue;
            }
            for (Method expressionMethod : getMethods(providerClass, false))
            {
                if (expressionMethod.getReturnType().equals(ProviderRegistration.class))
                {
                    continue;
                }
                String parameters = generateInputParameterValues(expressionMethod);
                if (parameters == null)
                {
                    ignoredMethods.add(expressionMethod);
                    continue;
                }
                String expression = providerName + "." + expressionMethod.getName() + parameters;
                String expressionResult = FAKER.expression("#{" + expression  + "}");
                String styleOperator;
                if (expressionResult.indexOf('\n') > 0)
                {
                    expressionResult = expressionResult.replaceAll("\n\r?", " +\n");
                    styleOperator = "a";
                }
                else if (expressionResult.indexOf(' ') == -1 && expressionResult.length() > 70)
                {
                    expressionResult = StreamSupport.stream(Splitter.fixedLength(70).split(expressionResult)
                            .spliterator(), false).collect(Collectors.joining(" +\n"));
                    styleOperator = "a";
                }
                else
                {
                    styleOperator = "";
                }
                System.out.println("\n|" + expression + "\n" + styleOperator + "|" + expressionResult);
            }

        }

        System.out.println();
        System.out.println("|===");

        System.out.println();
        System.out.println("////");
        System.out.println("The following methods has been ignored, because they require complex input parameters:");
        ignoredMethods.forEach(System.out::println);
        System.out.println("////");
    }

    private static String generateInputParameterValues(Method expressionMethod) throws IllegalAccessException
    {
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { }))
        {
            return "";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { boolean.class }))
        {
            return " 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { int.class }))
        {
            return " '7'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { int.class, int.class }) || Arrays.equals(
                expressionMethod.getParameterTypes(), new Class[] { long.class, long.class }))
        {
            return " '5' '10'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { double.class, double.class }))
        {
            return " '5.5' '10.10'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { boolean.class, boolean.class }))
        {
            return " 'true' 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { int.class, boolean.class }))
        {
            return " '5' 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { int.class, int.class, int.class }))
        {
            return " '3' '5' '10'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { int.class, long.class, long.class }))
        {
            return " '3' '5' '10'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { int.class, int.class, boolean.class }))
        {
            return " '5' '10' 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(),
                new Class[] { int.class, boolean.class, boolean.class }))
        {
            return " '5' 'true' 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(),
                new Class[] { int.class, int.class, boolean.class, boolean.class }))
        {
            return " '5' '10' 'true' 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(),
                new Class[] { int.class, boolean.class, boolean.class, boolean.class }))
        {
            return " '5' 'true' 'true' 'true'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(),
                new Class[] { int.class, int.class, boolean.class, boolean.class, boolean.class }))
        {
            return " '5' '10' 'true' 'true' 'true'";
        }
        if (expressionMethod.getDeclaringClass() == File.class && "fileName".equals(expressionMethod.getName()))
        {
            return " 'dir' 'filename' 'txt' '/'";
        }
        if (expressionMethod.getDeclaringClass() == Internet.class && "image".equals(expressionMethod.getName()))
        {
            return " '100' '200' 'imageName'";
        }
        // Part of configuration, to be ignored
        if (expressionMethod.getDeclaringClass() == Compass.class && "compassPoint".equals(expressionMethod.getName()))
        {
            return null;
        }
        if (expressionMethod.getParameterCount() == 1 && expressionMethod.getParameterTypes()[0].isEnum())
        {
            return " '" + ((Enum<?>) (expressionMethod.getParameterTypes()[0]).getEnumConstants()[0]).name() + "'";
        }
        if (Arrays.equals(expressionMethod.getParameterTypes(), new Class[] { String.class }))
        {
            if (expressionMethod.getDeclaringClass() == Address.class)
            {
                if ("countyByZipCode".equals(expressionMethod.getName()))
                {
                    // Does not work
                    return null;
                }
                if ("latLon".equals(expressionMethod.getName()))
                {
                    return " '; '";
                }
                if ("lonLat".equals(expressionMethod.getName()))
                {
                    return " '; '";
                }
                if ("zipCodeByState".equals(expressionMethod.getName()))
                {
                    return " '" + FAKER.address().stateAbbr() + "'";
                }
            }
            if (expressionMethod.getDeclaringClass() == Aviation.class && "flight".equals(expressionMethod.getName()))
            {
                return " 'ICAO'";
            }
            if (expressionMethod.getDeclaringClass() == Domain.class)
            {
                return " 'companyname'";
            }
            if (expressionMethod.getDeclaringClass() == DrivingLicense.class)
            {
                return " '" + FAKER.address().stateAbbr() + "'";
            }
            if (expressionMethod.getDeclaringClass() == Finance.class && "iban".equals(expressionMethod.getName()))
            {
                return " 'LV'";
            }
            if (expressionMethod.getDeclaringClass() == Internet.class)
            {
                if ("macAddress".equals(expressionMethod.getName()))
                {
                    return " 'aa:bb'";
                }
                if ("emailAddress".equals(expressionMethod.getName()) || "safeEmailAddress".equals(
                        expressionMethod.getName()))
                {
                    return " 'myemail'";
                }
            }
            if (expressionMethod.getDeclaringClass() == Vehicle.class)
            {
                if ("licensePlate".equals(expressionMethod.getName()))
                {
                    return " '" + FAKER.address().stateAbbr() + "'";
                }
                if ("model".equals(expressionMethod.getName()))
                {
                    return " '" + FAKER.vehicle().make() + "'";
                }
            }
        }
        return null;
    }

    private static List<Method> getMethods(Class<?> clazz, boolean noInputParameters)
    {
        return Stream.of(clazz.getMethods())
                .filter(m -> m.getDeclaringClass() != Object.class)
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()))
                .filter(m -> !noInputParameters || m.getParameterCount() == 0)
                .filter(m -> m.getReturnType() != Void.TYPE && m.getReturnType() != Faker.class)
                .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
                .collect(Collectors.toList());
    }
}
