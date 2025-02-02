/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.operator.aggregation.minmaxby;

import com.google.common.collect.ImmutableList;
import io.trino.metadata.Metadata;
import io.trino.metadata.ResolvedFunction;
import io.trino.operator.aggregation.InternalAggregationFunction;
import io.trino.spi.Page;
import io.trino.spi.TrinoException;
import io.trino.spi.type.ArrayType;
import io.trino.sql.tree.QualifiedName;
import org.testng.annotations.Test;

import java.util.Arrays;

import static io.trino.block.BlockAssertions.createArrayBigintBlock;
import static io.trino.block.BlockAssertions.createBlockOfReals;
import static io.trino.block.BlockAssertions.createDoublesBlock;
import static io.trino.block.BlockAssertions.createLongsBlock;
import static io.trino.block.BlockAssertions.createRLEBlock;
import static io.trino.block.BlockAssertions.createStringsBlock;
import static io.trino.metadata.MetadataManager.createTestMetadataManager;
import static io.trino.operator.aggregation.AggregationTestUtils.assertAggregation;
import static io.trino.operator.aggregation.AggregationTestUtils.groupedAggregation;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static io.trino.sql.analyzer.TypeSignatureProvider.fromTypes;
import static org.testng.Assert.assertEquals;

public class TestMinMaxByNAggregation
{
    private static final Metadata METADATA = createTestMetadataManager();

    @Test
    public void testMaxDoubleDouble()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(DOUBLE, DOUBLE, BIGINT));
        assertAggregation(
                METADATA,
                function,
                Arrays.asList((Double) null),
                createDoublesBlock(1.0, null),
                createDoublesBlock(3.0, 5.0),
                createRLEBlock(1L, 2));

        assertAggregation(
                METADATA,
                function,
                null,
                createDoublesBlock(null, null),
                createDoublesBlock(null, null),
                createRLEBlock(1L, 2));

        assertAggregation(
                METADATA,
                function,
                Arrays.asList(1.0),
                createDoublesBlock(null, 1.0, null, null),
                createDoublesBlock(null, 0.0, null, null),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                Arrays.asList(1.0),
                createDoublesBlock(1.0),
                createDoublesBlock(0.0),
                createRLEBlock(2L, 1));

        assertAggregation(
                METADATA,
                function,
                null,
                createDoublesBlock(),
                createDoublesBlock(),
                createRLEBlock(2L, 0));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(2.5),
                createDoublesBlock(2.5, 2.0, 5.0, 3.0),
                createDoublesBlock(4.0, 1.5, 2.0, 3.0),
                createRLEBlock(1L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(2.5, 3.0),
                createDoublesBlock(2.5, 2.0, 5.0, 3.0),
                createDoublesBlock(4.0, 1.5, 2.0, 3.0),
                createRLEBlock(2L, 4));
    }

    @Test
    public void testMinDoubleDouble()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("min_by"), fromTypes(DOUBLE, DOUBLE, BIGINT));
        assertAggregation(
                METADATA,
                function,
                Arrays.asList((Double) null),
                createDoublesBlock(1.0, null),
                createDoublesBlock(5.0, 3.0),
                createRLEBlock(1L, 2));

        assertAggregation(
                METADATA,
                function,
                null,
                createDoublesBlock(null, null),
                createDoublesBlock(null, null),
                createRLEBlock(1L, 2));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(2.0),
                createDoublesBlock(2.5, 2.0, 5.0, 3.0),
                createDoublesBlock(4.0, 1.5, 2.0, 3.0),
                createRLEBlock(1L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(2.0, 5.0),
                createDoublesBlock(2.5, 2.0, 5.0, 3.0),
                createDoublesBlock(4.0, 1.5, 2.0, 3.0),
                createRLEBlock(2L, 4));
    }

    @Test
    public void testMinDoubleVarchar()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("min_by"), fromTypes(VARCHAR, DOUBLE, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("z", "a"),
                createStringsBlock("z", "a", "x", "b"),
                createDoublesBlock(1.0, 2.0, 2.0, 3.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "zz"),
                createStringsBlock("zz", "hi", "bb", "a"),
                createDoublesBlock(0.0, 1.0, 2.0, -1.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "zz"),
                createStringsBlock("zz", "hi", null, "a"),
                createDoublesBlock(0.0, 1.0, null, -1.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("b", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(Double.NaN, 2.0, 3.0, 4.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(1.0, Double.NaN, 3.0, 4.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(1.0, 2.0, Double.NaN, 4.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(1.0, 2.0, 3.0, Double.NaN),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b"),
                createDoublesBlock(1.0, Double.NaN),
                createRLEBlock(2L, 2));
    }

    @Test
    public void testMaxDoubleVarchar()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(VARCHAR, DOUBLE, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "z"),
                createStringsBlock("z", "a", null),
                createDoublesBlock(1.0, 2.0, null),
                createRLEBlock(2L, 3));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("bb", "hi"),
                createStringsBlock("zz", "hi", "bb", "a"),
                createDoublesBlock(0.0, 1.0, 2.0, -1.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("hi", "zz"),
                createStringsBlock("zz", "hi", null, "a"),
                createDoublesBlock(0.0, 1.0, null, -1.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("d", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(Double.NaN, 2.0, 3.0, 4.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("d", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(1.0, Double.NaN, 3.0, 4.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("d", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(1.0, 2.0, Double.NaN, 4.0),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("c", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createDoublesBlock(1.0, 2.0, 3.0, Double.NaN),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b"),
                createDoublesBlock(1.0, Double.NaN),
                createRLEBlock(2L, 2));
    }

    @Test
    public void testMinRealVarchar()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("min_by"), fromTypes(VARCHAR, REAL, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("z", "a"),
                createStringsBlock("z", "a", "x", "b"),
                createBlockOfReals(1.0f, 2.0f, 2.0f, 3.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "zz"),
                createStringsBlock("zz", "hi", "bb", "a"),
                createBlockOfReals(0.0f, 1.0f, 2.0f, -1.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "zz"),
                createStringsBlock("zz", "hi", null, "a"),
                createBlockOfReals(0.0f, 1.0f, null, -1.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("b", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(Float.NaN, 2.0f, 3.0f, 4.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(1.0f, Float.NaN, 3.0f, 4.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(1.0f, 2.0f, Float.NaN, 4.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(1.0f, 2.0f, 3.0f, Float.NaN),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b"),
                createBlockOfReals(1.0f, Float.NaN),
                createRLEBlock(2L, 2));
    }

    @Test
    public void testMaxRealVarchar()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(VARCHAR, REAL, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "z"),
                createStringsBlock("z", "a", null),
                createBlockOfReals(1.0f, 2.0f, null),
                createRLEBlock(2L, 3));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("bb", "hi"),
                createStringsBlock("zz", "hi", "bb", "a"),
                createBlockOfReals(0.0f, 1.0f, 2.0f, -1.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("hi", "zz"),
                createStringsBlock("zz", "hi", null, "a"),
                createBlockOfReals(0.0f, 1.0f, null, -1.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("d", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(Float.NaN, 2.0f, 3.0f, 4.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("d", "c"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(1.0f, Float.NaN, 3.0f, 4.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("d", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(1.0f, 2.0f, Float.NaN, 4.0f),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("c", "b"),
                createStringsBlock("a", "b", "c", "d"),
                createBlockOfReals(1.0f, 2.0f, 3.0f, Float.NaN),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "b"),
                createStringsBlock("a", "b"),
                createBlockOfReals(1.0f, Float.NaN),
                createRLEBlock(2L, 2));
    }

    @Test
    public void testMinVarcharDouble()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("min_by"), fromTypes(DOUBLE, VARCHAR, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(2.0, 3.0),
                createDoublesBlock(1.0, 2.0, 2.0, 3.0),
                createStringsBlock("z", "a", "x", "b"),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(-1.0, 2.0),
                createDoublesBlock(0.0, 1.0, 2.0, -1.0),
                createStringsBlock("zz", "hi", "bb", "a"),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(-1.0, 1.0),
                createDoublesBlock(0.0, 1.0, null, -1.0),
                createStringsBlock("zz", "hi", null, "a"),
                createRLEBlock(2L, 4));
    }

    @Test
    public void testMaxVarcharDouble()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(DOUBLE, VARCHAR, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(1.0, 2.0),
                createDoublesBlock(1.0, 2.0, null),
                createStringsBlock("z", "a", null),
                createRLEBlock(2L, 3));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(0.0, 1.0),
                createDoublesBlock(0.0, 1.0, 2.0, -1.0),
                createStringsBlock("zz", "hi", "bb", "a"),
                createRLEBlock(2L, 4));

        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(0.0, 1.0),
                createDoublesBlock(0.0, 1.0, null, -1.0),
                createStringsBlock("zz", "hi", null, "a"),
                createRLEBlock(2L, 4));
    }

    @Test
    public void testMinVarcharArray()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("min_by"), fromTypes(new ArrayType(BIGINT), VARCHAR, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(ImmutableList.of(2L, 3L), ImmutableList.of(4L, 5L)),
                createArrayBigintBlock(ImmutableList.of(ImmutableList.of(1L, 2L), ImmutableList.of(2L, 3L), ImmutableList.of(3L, 4L), ImmutableList.of(4L, 5L))),
                createStringsBlock("z", "a", "x", "b"),
                createRLEBlock(2L, 4));
    }

    @Test
    public void testMaxVarcharArray()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(new ArrayType(BIGINT), VARCHAR, BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of(ImmutableList.of(1L, 2L), ImmutableList.of(3L, 4L)),
                createArrayBigintBlock(ImmutableList.of(ImmutableList.of(1L, 2L), ImmutableList.of(2L, 3L), ImmutableList.of(3L, 4L), ImmutableList.of(4L, 5L))),
                createStringsBlock("z", "a", "x", "b"),
                createRLEBlock(2L, 4));
    }

    @Test
    public void testMinArrayVarchar()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("min_by"), fromTypes(VARCHAR, new ArrayType(BIGINT), BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("b", "x", "z"),
                createStringsBlock("z", "a", "x", "b"),
                createArrayBigintBlock(ImmutableList.of(ImmutableList.of(1L, 2L), ImmutableList.of(2L, 3L), ImmutableList.of(0L, 3L), ImmutableList.of(0L, 2L))),
                createRLEBlock(3L, 4));
    }

    @Test
    public void testMaxArrayVarchar()
    {
        ResolvedFunction function = METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(VARCHAR, new ArrayType(BIGINT), BIGINT));
        assertAggregation(
                METADATA,
                function,
                ImmutableList.of("a", "z", "x"),
                createStringsBlock("z", "a", "x", "b"),
                createArrayBigintBlock(ImmutableList.of(ImmutableList.of(1L, 2L), ImmutableList.of(2L, 3L), ImmutableList.of(0L, 3L), ImmutableList.of(0L, 2L))),
                createRLEBlock(3L, 4));
    }

    @Test
    public void testOutOfBound()
    {
        InternalAggregationFunction function = METADATA.getAggregateFunctionImplementation(
                METADATA.resolveFunction(QualifiedName.of("max_by"), fromTypes(VARCHAR, BIGINT, BIGINT)));
        try {
            groupedAggregation(function, new Page(createStringsBlock("z"), createLongsBlock(0), createLongsBlock(10001)));
        }
        catch (TrinoException e) {
            assertEquals(e.getMessage(), "third argument of max_by/min_by must be less than or equal to 10000; found 10001");
        }
    }
}
