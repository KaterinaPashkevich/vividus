/*
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.bdd.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jbehave.core.model.ExamplesTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.bdd.context.IBddVariableContext;
import org.vividus.bdd.variable.VariableScope;
import org.vividus.reporter.event.IAttachmentPublisher;
import org.vividus.softassert.ISoftAssert;
import org.vividus.util.comparison.ComparisonUtils.EntryComparisonResult;
import org.vividus.util.freemarker.FreemarkerProcessor;

import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class BddVariableStepsTests
{
    private static final String HEADER = "header";
    private static final String TABLES_ARE_EQUAL = "Tables are equal";
    private static final String KEY_1 = "k1";
    private static final String VALUE_1 = "v1";
    private static final String KEY_2 = "k2";
    private static final String VALUE_2 = "v2";
    private static final ExamplesTable SINGLE_VALUE_TABLE = new ExamplesTable("|k1|\n|v1|");

    @Mock private FreemarkerProcessor freemarkerProcessor;
    @Mock private IBddVariableContext bddVariableContext;
    @Mock private ISoftAssert softAssert;
    @Mock private IAttachmentPublisher attachmentPublisher;
    @InjectMocks private BddVariableSteps bddVariableSteps;

    @BeforeEach
    void beforeEach()
    {
        bddVariableSteps.setFreemarkerProcessor(freemarkerProcessor);
    }

    @SuppressWarnings({ "checkstyle:MultipleStringLiterals", "checkstyle:MultipleStringLiteralsExtended" })
    static Stream<Arguments> stringsAsNumbers()
    {
        return Stream.of(
                arguments("9", "10"),
                arguments(9,   "10"),
                arguments("9", 10),
                arguments(9,   10)
        );
    }

    @ParameterizedTest
    @MethodSource("stringsAsNumbers")
    void shouldCompareStringsAsNumbers(Object variable1, Object variable2)
    {
        bddVariableSteps.compareVariables(variable1, ComparisonRule.LESS_THAN_OR_EQUAL_TO, variable2);
        verify(softAssert).assertThat(eq("Checking if \"9\" is less than or equal to \"10\""),
                eq(BigDecimal.valueOf(9)), argThat(m -> "a value less than or equal to <10>".equals(m.toString())));
    }

    @ParameterizedTest
    @CsvSource({
            "9,       string",
            "string1, string2"
    })
    void testCompareSimpleVariablesStrings(String variable1, String variable2)
    {
        String description = "Checking if \"" + variable1 + "\" is equal to \"" + variable2 + "\"";
        when(softAssert.assertThat(eq(description), eq(variable1), any())).thenReturn(false);
        assertFalse(bddVariableSteps.compareVariables(variable1, ComparisonRule.EQUAL_TO, variable2));
    }

    @Test
    void testInitVariableWithGivenValue()
    {
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        bddVariableSteps.initVariableWithGivenValue(scopes, VALUE_1, VALUE_1);
        verify(bddVariableContext).putVariable(scopes, VALUE_1, VALUE_1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCompareEqualToListsOfMapsAndPublishResultTable()
    {
        List<Map<String, Object>> listOfMaps1 = List.of(Map.of(KEY_1, VALUE_1));
        List<Map<String, Object>> listOfMaps2 = List.of(Map.of(KEY_1, VALUE_2));
        assertFalse(bddVariableSteps.compareVariables(listOfMaps1, ComparisonRule.EQUAL_TO, listOfMaps2));
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, false);
        EntryComparisonResult result = captureComparisonResults().get(0).get(0);
        assertResult(KEY_1, VALUE_1, VALUE_2, false, result);
        verify(softAssert, never()).assertThat(any(), any(), any());
    }

    @Test
    void shouldCompareEqualToListsOfMapsAndPublishResultTableWhenListsAreEmpty()
    {
        List<Map<String, Object>> listOfMaps1 = List.of();
        List<Map<String, Object>> listOfMaps2 = List.of();
        assertFalse(bddVariableSteps.compareVariables(listOfMaps1, ComparisonRule.EQUAL_TO, listOfMaps2));
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, true);
        assertEquals(List.of(), captureComparisonResults());
        verify(softAssert, never()).assertThat(any(), any(), any());
    }

    @Test
    void shouldCompareEqualToMapsAndPublishResultTable()
    {
        Map<String, Object> map1 = Map.of(KEY_1, 2);
        Map<String, Object> map2 = Map.of(KEY_1, 2L);
        BigDecimal twoBD = new BigDecimal("2");
        assertFalse(bddVariableSteps.compareVariables(map1, ComparisonRule.EQUAL_TO, map2));
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, true);

        EntryComparisonResult result = captureComparisonResults().get(0).get(0);
        assertResult(KEY_1, twoBD, twoBD, true, result);

        verify(softAssert, never()).assertThat(any(), any(), any());
    }

    @Test
    void shouldCompareEqualToNotAlignedListsRightIsLongerOfMapsAndPublishResultTable()
    {
        List<Map<String, Object>> listOfMaps1 = List.of(Map.of(KEY_1, VALUE_1));
        List<Map<String, Object>> listOfMaps2 = List.of(Map.of(KEY_1, VALUE_1), Map.of(KEY_1, VALUE_2));
        assertFalse(bddVariableSteps.compareVariables(listOfMaps1, ComparisonRule.EQUAL_TO, listOfMaps2));
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, false);

        List<List<EntryComparisonResult>> results = captureComparisonResults();
        assertResult(KEY_1, VALUE_1, VALUE_1, true, results.get(0).get(0));
        assertResult(KEY_1, null, VALUE_2, false, results.get(1).get(0));

        verify(softAssert, never()).assertThat(any(), any(), any());
    }

    @Test
    void shouldCompareEqualToNotAlignedListsLeftIsLongerOfMapsAndPublishResultTable()
    {
        List<Map<String, Object>> listOfMaps1 = List.of(Map.of(KEY_1, VALUE_1), Map.of(KEY_1, VALUE_2));
        List<Map<String, Object>> listOfMaps2 = List.of(Map.of(KEY_1, VALUE_1));
        assertFalse(bddVariableSteps.compareVariables(listOfMaps1, ComparisonRule.EQUAL_TO, listOfMaps2));
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, false);

        List<List<EntryComparisonResult>> results = captureComparisonResults();
        assertResult(KEY_1, VALUE_1, VALUE_1, true, results.get(0).get(0));
        assertResult(KEY_1, VALUE_2, null, false, results.get(1).get(0));

        verify(softAssert, never()).assertThat(any(), any(), any());
    }

    @Test
    void shouldTreatNullForBothValuesAsEqual()
    {
        List<Map<String, Object>> listOfMaps = List.of(Collections.singletonMap(KEY_1, null));
        bddVariableSteps.compareVariables(listOfMaps, ComparisonRule.EQUAL_TO, listOfMaps);
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, true);
    }

    @SuppressWarnings("checkstyle:NoWhitespaceBefore")
    static Stream<Arguments> parametersProvider()
    {
        return Stream.of(
                arguments(VALUE_1,                         ComparisonRule.EQUAL_TO , VALUE_2),
                arguments(List.of(Map.of(KEY_1, VALUE_1)), ComparisonRule.LESS_THAN, List.of(Map.of(KEY_1, VALUE_1))),
                arguments(List.of(Map.of(KEY_1, VALUE_1)), ComparisonRule.EQUAL_TO,  Map.of(KEY_1, VALUE_1)),
                arguments(Map.of(KEY_1, VALUE_1),          ComparisonRule.EQUAL_TO,  List.of(Map.of(KEY_1, VALUE_1))),
                arguments(Map.of(KEY_1, VALUE_1),          ComparisonRule.LESS_THAN, Map.of(KEY_1, VALUE_1)),
                arguments(List.of(KEY_1),                  ComparisonRule.EQUAL_TO,  List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void shouldFallBackToStringsComparisonIfOneVariableIsNotMapTable(Object var1, ComparisonRule rule, Object var2)
    {
        assertFalse(bddVariableSteps.compareVariables(var1, rule, var2));
        verifyNoInteractions(attachmentPublisher);
    }

    static Stream<Arguments> tablesIgnoringExtraColumns()
    {
        return Stream.of(
                arguments(Map.of(KEY_1, VALUE_1)),
                arguments(Map.of(KEY_1, List.of(VALUE_1)))
        );
    }

    @ParameterizedTest
    @MethodSource("tablesIgnoringExtraColumns")
    void shouldCompareTablesIgnoringExtraColumns(Map<String, Object> actualMap)
    {
        ExamplesTable table = new ExamplesTable("|k1|k2|\n|v1|v2|");
        bddVariableSteps.tablesAreEqualIgnoringExtraColumns(actualMap, table);
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, false);
        List<EntryComparisonResult> results = captureComparisonResults().get(0);
        assertResult(KEY_1, VALUE_1, VALUE_1, true, results.get(0));
        assertResult(KEY_2, VALUE_2, null, false, results.get(1));
    }

    @Test
    void shouldFailOnComparisonOfJaggedTablesIgnoringExtraColumns()
    {
        Map<String, Object> actualMap = Map.of(KEY_1, List.of(VALUE_1, VALUE_2));
        bddVariableSteps.tablesAreEqualIgnoringExtraColumns(actualMap, SINGLE_VALUE_TABLE);
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, false);
        EntryComparisonResult result = captureComparisonResults().get(0).get(0);
        assertResult(KEY_1, VALUE_1, List.of(VALUE_1, VALUE_2), false, result);
    }

    @Test
    void shouldFailOnCompareTablesIgnoringExtraColumnsIfMapNotProvided()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bddVariableSteps.tablesAreEqualIgnoringExtraColumns("stringValue", null));
        assertEquals("'variable' should be an instance of map", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "''",
            "'|k1|\n|v1|\n|v2|'"
    })
    void shouldFailOnCompareTablesIgnoringExtraColumnsIfExamplesTableIsOfUnexpectedSize(ExamplesTable examplesTable)
    {
        Map<Object, Object> variable = Map.of();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bddVariableSteps.tablesAreEqualIgnoringExtraColumns(variable, examplesTable));
        assertEquals("ExamplesTable should contain single row with values", exception.getMessage());
    }

    @Test
    void shouldCompareTables()
    {
        List<Map<String, Object>> listOfMaps = List.of(Map.of(KEY_1, VALUE_1));
        bddVariableSteps.tablesAreEqual(listOfMaps, SINGLE_VALUE_TABLE);
        verify(softAssert).assertTrue(TABLES_ARE_EQUAL, true);
        EntryComparisonResult result = captureComparisonResults().get(0).get(0);
        assertResult(KEY_1, VALUE_1, VALUE_1, true, result);
    }

    @SuppressWarnings("unchecked")
    private List<List<EntryComparisonResult>> captureComparisonResults()
    {
        ArgumentCaptor<Object> dataModelCaptor = ArgumentCaptor.forClass(Object.class);
        verify(attachmentPublisher).publishAttachment(eq("/templates/maps-comparison-table.ftl"),
                dataModelCaptor.capture(), eq("Tables comparison result"));

        return ((Map<String, List<List<EntryComparisonResult>>>) dataModelCaptor.getValue()).get("results");
    }

    private void assertResult(String expectedKey, Object expectedLeft, Object expectedRight, boolean expectedPassed,
            EntryComparisonResult actualResult)
    {
        assertEquals(expectedKey, actualResult.getKey());
        assertEquals(expectedLeft, actualResult.getLeft());
        assertEquals(expectedRight, actualResult.getRight());
        assertEquals(expectedPassed, actualResult.isPassed());
    }

    @Test
    void shouldFailOnCompareTablesIfListOfMapsNotProvided()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> bddVariableSteps.tablesAreEqual("", null));
        assertEquals("'variable' should be empty list or list of maps structure", exception.getMessage());
    }

    static Stream<Arguments> mapProvider()
    {
        final String parameters = "parameters";
        final String data = "data";
        final List<String> dataList = List.of(data);
        return Stream.of(
            arguments(Map.of(HEADER, data),     Map.of(KEY_1, VALUE_1, HEADER, dataList, parameters,
                    Map.of(HEADER, dataList, KEY_1, VALUE_1))),
            arguments(Map.of(parameters, data), Map.of(parameters, Map.of(parameters, dataList, KEY_1, VALUE_1,
                    HEADER, VALUE_2)))
        );
    }

    @ParameterizedTest
    @MethodSource("mapProvider")
    void testInitVariableUsingTemplate(Map<String, String> dataModel, Map<String, ?> resultMap)
        throws IOException, TemplateException
    {
        Map<String, Object> variables = new HashMap<>(Map.of(KEY_1, VALUE_1, HEADER, VALUE_2));
        when(bddVariableContext.getVariables()).thenReturn(variables);
        String templatePath = "/templatePath";
        when(freemarkerProcessor.process(templatePath, resultMap, StandardCharsets.UTF_8)).thenReturn(VALUE_1);
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        String variableName = "variableName";
        bddVariableSteps.initVariableUsingTemplate(scopes, variableName, templatePath,
                new ExamplesTable("").withRows(List.of(dataModel)));
        verify(bddVariableContext).putVariable(scopes, variableName, VALUE_1);
    }

    @Test
    void testValueMatchesPattern()
    {
        String pattern = ".*";
        bddVariableSteps.valueMatchesPattern(VALUE_1, Pattern.compile(pattern));
        verify(softAssert).assertThat(eq(String.format("Value '%s' matches pattern '%s'", VALUE_1, pattern)),
                eq(VALUE_1), argThat(m -> "a string matching the pattern '.*'".equals(m.toString())));
    }

    @Test
    void testInitVariableWithGivenValues()
    {
        Set<VariableScope> scopes = Set.of(VariableScope.SCENARIO);
        List<Map<String, String>> listOfMaps = List.of(Map.of("key0", "value0", "key1", "value1"));
        bddVariableSteps.initVariableWithGivenValues(scopes, VALUE_1, listOfMaps);
        verify(bddVariableContext).putVariable(scopes, VALUE_1, listOfMaps);
    }
}
