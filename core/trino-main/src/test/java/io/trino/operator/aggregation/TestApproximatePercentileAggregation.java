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
package io.trino.operator.aggregation;

import com.google.common.collect.ImmutableList;
import io.trino.metadata.Metadata;
import io.trino.metadata.ResolvedFunction;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.RunLengthEncodedBlock;
import io.trino.spi.type.ArrayType;
import io.trino.sql.tree.QualifiedName;
import org.testng.annotations.Test;

import static io.trino.block.BlockAssertions.createBlockOfReals;
import static io.trino.block.BlockAssertions.createDoubleRepeatBlock;
import static io.trino.block.BlockAssertions.createDoubleSequenceBlock;
import static io.trino.block.BlockAssertions.createDoublesBlock;
import static io.trino.block.BlockAssertions.createLongSequenceBlock;
import static io.trino.block.BlockAssertions.createLongsBlock;
import static io.trino.block.BlockAssertions.createSequenceBlockOfReal;
import static io.trino.metadata.MetadataManager.createTestMetadataManager;
import static io.trino.operator.aggregation.AggregationTestUtils.assertAggregation;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.sql.analyzer.TypeSignatureProvider.fromTypes;

public class TestApproximatePercentileAggregation
{
    private static final Metadata metadata = createTestMetadataManager();

    private static final ResolvedFunction DOUBLE_APPROXIMATE_PERCENTILE_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(DOUBLE, DOUBLE));
    private static final ResolvedFunction DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(DOUBLE, BIGINT, DOUBLE));
    private static final ResolvedFunction DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION_WITH_ACCURACY = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(DOUBLE, BIGINT, DOUBLE, DOUBLE));

    private static final ResolvedFunction LONG_APPROXIMATE_PERCENTILE_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(BIGINT, DOUBLE));
    private static final ResolvedFunction LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(BIGINT, BIGINT, DOUBLE));
    private static final ResolvedFunction LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION_WITH_ACCURACY = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(BIGINT, BIGINT, DOUBLE, DOUBLE));

    private static final ResolvedFunction DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(DOUBLE, new ArrayType(DOUBLE)));
    private static final ResolvedFunction DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_WEIGHTED_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(DOUBLE, BIGINT, new ArrayType(DOUBLE)));

    private static final ResolvedFunction LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(BIGINT, new ArrayType(DOUBLE)));
    private static final ResolvedFunction LONG_APPROXIMATE_PERCENTILE_ARRAY_WEIGHTED_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(BIGINT, BIGINT, new ArrayType(DOUBLE)));

    private static final ResolvedFunction FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(REAL, DOUBLE));
    private static final ResolvedFunction FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(REAL, BIGINT, DOUBLE));
    private static final ResolvedFunction FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION_WITH_ACCURACY = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(REAL, BIGINT, DOUBLE, DOUBLE));

    private static final ResolvedFunction FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(REAL, new ArrayType(DOUBLE)));
    private static final ResolvedFunction FLOAT_APPROXIMATE_PERCENTILE_ARRAY_WEIGHTED_AGGREGATION = metadata.resolveFunction(QualifiedName.of("approx_percentile"), fromTypes(REAL, BIGINT, new ArrayType(DOUBLE)));

    @Test
    public void testLongPartialStep()
    {
        // regular approx_percentile
        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_AGGREGATION,
                null,
                createLongsBlock(null, null),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_AGGREGATION,
                1L,
                createLongsBlock(null, 1L),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_AGGREGATION,
                2L,
                createLongsBlock(null, 1L, 2L, 3L),
                createRLEBlock(0.5, 4));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_AGGREGATION,
                2L,
                createLongsBlock(1L, 2L, 3L),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_AGGREGATION,
                3L,
                createLongsBlock(1L, null, 2L, 2L, null, 2L, 2L, null, 2L, 2L, null, 3L, 3L, null, 3L, null, 3L, 4L, 5L, 6L, 7L),
                createRLEBlock(0.5, 21));

        // array of approx_percentile
        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                null,
                createLongsBlock(null, null),
                createRLEBlock(ImmutableList.of(0.5), 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                null,
                createLongsBlock(null, null),
                createRLEBlock(ImmutableList.of(0.5, 0.99), 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1L, 1L),
                createLongsBlock(null, 1L),
                createRLEBlock(ImmutableList.of(0.5, 0.5), 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1L, 2L, 3L),
                createLongsBlock(null, 1L, 2L, 3L),
                createRLEBlock(ImmutableList.of(0.2, 0.5, 0.8), 4));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(2L, 3L),
                createLongsBlock(1L, 2L, 3L),
                createRLEBlock(ImmutableList.of(0.5, 0.99), 3));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1L, 3L),
                createLongsBlock(1L, null, 2L, 2L, null, 2L, 2L, null, 2L, 2L, null, 3L, 3L, null, 3L, null, 3L, 4L, 5L, 6L, 7L),
                createRLEBlock(ImmutableList.of(0.01, 0.5), 21));

        // unsorted percentiles
        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(3L, 1L, 2L),
                createLongsBlock(null, 1L, 2L, 3L),
                createRLEBlock(ImmutableList.of(0.8, 0.2, 0.5), 4));

        // weighted approx_percentile
        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                null,
                createLongsBlock(null, null),
                createLongsBlock(1L, 1L),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                1L,
                createLongsBlock(null, 1L),
                createDoublesBlock(1.0, 1.0),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2L,
                createLongsBlock(null, 1L, 2L, 3L),
                createDoublesBlock(1.0, 1.0, 1.0, 1.0),
                createRLEBlock(0.5, 4));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2L,
                createLongsBlock(1L, 2L, 3L),
                createDoublesBlock(1.0, 1.0, 1.0),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2L,
                createLongsBlock(1L, 2L, 3L),
                createDoublesBlock(23.4, 23.4, 23.4),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                3L,
                createLongsBlock(1L, null, 2L, null, 2L, null, 2L, null, 3L, null, 3L, null, 3L, 4L, 5L, 6L, 7L),
                createDoublesBlock(1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                createRLEBlock(0.5, 17));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                3L,
                createLongsBlock(1L, null, 2L, null, 2L, null, 2L, null, 3L, null, 3L, null, 3L, 4L, 5L, 6L, 7L),
                createDoublesBlock(1.1, 1.1, 2.2, 1.1, 2.2, 1.1, 2.2, 1.1, 2.2, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1),
                createRLEBlock(0.5, 17));

        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION_WITH_ACCURACY,
                9900L,
                createLongSequenceBlock(0, 10000),
                createDoubleRepeatBlock(1.0, 10000),
                createRLEBlock(0.99, 10000),
                createRLEBlock(0.001, 10000));

        // weighted + array of approx_percentile
        assertAggregation(
                metadata,
                LONG_APPROXIMATE_PERCENTILE_ARRAY_WEIGHTED_AGGREGATION,
                ImmutableList.of(2L, 3L),
                createLongsBlock(1L, 2L, 3L),
                createDoublesBlock(4.0, 2.0, 1.0),
                createRLEBlock(ImmutableList.of(0.5, 0.8), 3));
    }

    @Test
    public void testFloatPartialStep()
    {
        // regular approx_percentile
        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                null,
                createBlockOfReals(null, null),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                1.0f,
                createBlockOfReals(null, 1.0f),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                2.0f,
                createBlockOfReals(null, 1.0f, 2.0f, 3.0f),
                createRLEBlock(0.5, 4));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                1.0f,
                createBlockOfReals(-1.0f, 1.0f),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                -1.0f,
                createBlockOfReals(-2.0f, 3.0f, -1.0f),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                2.0f,
                createBlockOfReals(1.0f, 2.0f, 3.0f),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_AGGREGATION,
                3.0f,
                createBlockOfReals(1.0f, null, 2.0f, 2.0f, null, 2.0f, 2.0f, null, 2.0f, 2.0f, null, 3.0f, 3.0f, null, 3.0f, null, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f),
                createRLEBlock(0.5, 21));

        // array of approx_percentile
        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                null,
                createBlockOfReals(null, null),
                createRLEBlock(ImmutableList.of(0.5), 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                null,
                createBlockOfReals(null, null),
                createRLEBlock(ImmutableList.of(0.5, 0.5), 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1.0f, 1.0f),
                createBlockOfReals(null, 1.0f),
                createRLEBlock(ImmutableList.of(0.5, 0.5), 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1.0f, 2.0f, 3.0f),
                createBlockOfReals(null, 1.0f, 2.0f, 3.0f),
                createRLEBlock(ImmutableList.of(0.2, 0.5, 0.8), 4));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(2.0f, 3.0f),
                createBlockOfReals(1.0f, 2.0f, 3.0f),
                createRLEBlock(ImmutableList.of(0.5, 0.99), 3));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1.0f, 3.0f),
                createBlockOfReals(1.0f, null, 2.0f, 2.0f, null, 2.0f, 2.0f, null, 2.0f, 2.0f, null, 3.0f, 3.0f, null, 3.0f, null, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f),
                createRLEBlock(ImmutableList.of(0.01, 0.5), 21));

        // unsorted percentiles
        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(3.0f, 1.0f, 2.0f),
                createBlockOfReals(null, 1.0f, 2.0f, 3.0f),
                createRLEBlock(ImmutableList.of(0.8, 0.2, 0.5), 4));

        // weighted approx_percentile
        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                null,
                createBlockOfReals(null, null),
                createLongsBlock(1L, 1L),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                1.0f,
                createBlockOfReals(null, 1.0f),
                createDoublesBlock(1.0, 1.0),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.0f,
                createBlockOfReals(null, 1.0f, 2.0f, 3.0f),
                createDoublesBlock(1.0, 1.0, 1.0, 1.0),
                createRLEBlock(0.5, 4));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.0f,
                createBlockOfReals(1.0f, 2.0f, 3.0f),
                createDoublesBlock(1.0, 1.0, 1.0),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.75f,
                createBlockOfReals(1.0f, null, 2.0f, null, 2.0f, null, 2.0f, null, 3.0f, null, 3.0f, null, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f),
                createDoublesBlock(1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                createRLEBlock(0.5, 17));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.75f,
                createBlockOfReals(1.0f, null, 2.0f, null, 2.0f, null, 2.0f, null, 3.0f, null, 3.0f, null, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f),
                createDoublesBlock(1.1, 1.1, 2.2, 1.1, 2.2, 1.1, 2.2, 1.1, 2.2, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1),
                createRLEBlock(0.5, 17));

        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION_WITH_ACCURACY,
                9900.0f,
                createSequenceBlockOfReal(0, 10000),
                createDoubleRepeatBlock(1, 10000),
                createRLEBlock(0.99, 10000),
                createRLEBlock(0.001, 10000));

        // weighted + array of approx_percentile
        assertAggregation(
                metadata,
                FLOAT_APPROXIMATE_PERCENTILE_ARRAY_WEIGHTED_AGGREGATION,
                ImmutableList.of(1.5f, 2.6f),
                createBlockOfReals(1.0f, 2.0f, 3.0f),
                createDoublesBlock(4.0, 2.0, 1.0),
                createRLEBlock(ImmutableList.of(0.5, 0.8), 3));
    }

    @Test
    public void testDoublePartialStep()
    {
        // regular approx_percentile
        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_AGGREGATION,
                null,
                createDoublesBlock(null, null),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_AGGREGATION,
                1.0,
                createDoublesBlock(null, 1.0),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_AGGREGATION,
                2.0,
                createDoublesBlock(null, 1.0, 2.0, 3.0),
                createRLEBlock(0.5, 4));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_AGGREGATION,
                2.0,
                createDoublesBlock(1.0, 2.0, 3.0),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_AGGREGATION,
                3.0,
                createDoublesBlock(1.0, null, 2.0, 2.0, null, 2.0, 2.0, null, 2.0, 2.0, null, 3.0, 3.0, null, 3.0, null, 3.0, 4.0, 5.0, 6.0, 7.0),
                createRLEBlock(0.5, 21));

        // array of approx_percentile
        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                null,
                createDoublesBlock(null, null),
                createRLEBlock(ImmutableList.of(0.5), 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                null,
                createDoublesBlock(null, null),
                createRLEBlock(ImmutableList.of(0.5, 0.5), 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1.0, 1.0),
                createDoublesBlock(null, 1.0),
                createRLEBlock(ImmutableList.of(0.5, 0.5), 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1.0, 2.0, 3.0),
                createDoublesBlock(null, 1.0, 2.0, 3.0),
                createRLEBlock(ImmutableList.of(0.2, 0.5, 0.8), 4));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(2.0, 3.0),
                createDoublesBlock(1.0, 2.0, 3.0),
                createRLEBlock(ImmutableList.of(0.5, 0.99), 3));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(1.0, 3.0),
                createDoublesBlock(1.0, null, 2.0, 2.0, null, 2.0, 2.0, null, 2.0, 2.0, null, 3.0, 3.0, null, 3.0, null, 3.0, 4.0, 5.0, 6.0, 7.0),
                createRLEBlock(ImmutableList.of(0.01, 0.5), 21));

        // unsorted percentiles
        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_AGGREGATION,
                ImmutableList.of(3.0, 1.0, 2.0),
                createDoublesBlock(null, 1.0, 2.0, 3.0),
                createRLEBlock(ImmutableList.of(0.8, 0.2, 0.5), 4));

        // weighted approx_percentile
        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                null,
                createDoublesBlock(null, null),
                createLongsBlock(1L, 1L),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                1.0,
                createDoublesBlock(null, 1.0),
                createDoublesBlock(1.0, 1.0),
                createRLEBlock(0.5, 2));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.0,
                createDoublesBlock(null, 1.0, 2.0, 3.0),
                createDoublesBlock(1.0, 1.0, 1.0, 1.0),
                createRLEBlock(0.5, 4));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.0,
                createDoublesBlock(1.0, 2.0, 3.0),
                createDoublesBlock(1.0, 1.0, 1.0),
                createRLEBlock(0.5, 3));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.75,
                createDoublesBlock(1.0, null, 2.0, null, 2.0, null, 2.0, null, 3.0, null, 3.0, null, 3.0, 4.0, 5.0, 6.0, 7.0),
                createDoublesBlock(1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                createRLEBlock(0.5, 17));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION,
                2.75,
                createDoublesBlock(1.0, null, 2.0, null, 2.0, null, 2.0, null, 3.0, null, 3.0, null, 3.0, 4.0, 5.0, 6.0, 7.0),
                createDoublesBlock(1.1, 1.1, 2.2, 1.1, 2.2, 1.1, 2.2, 1.1, 2.2, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1),
                createRLEBlock(0.5, 17));

        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_WEIGHTED_AGGREGATION_WITH_ACCURACY,
                9900.0,
                createDoubleSequenceBlock(0, 10000),
                createDoubleRepeatBlock(1.0, 10000),
                createRLEBlock(0.99, 10000),
                createRLEBlock(0.001, 10000));

        // weighted + array of approx_percentile
        assertAggregation(
                metadata,
                DOUBLE_APPROXIMATE_PERCENTILE_ARRAY_WEIGHTED_AGGREGATION,
                ImmutableList.of(1.5, 2.6000000000000005),
                createDoublesBlock(1.0, 2.0, 3.0),
                createDoublesBlock(4.0, 2.0, 1.0),
                createRLEBlock(ImmutableList.of(0.5, 0.8), 3));
    }

    private static RunLengthEncodedBlock createRLEBlock(double percentile, int positionCount)
    {
        BlockBuilder blockBuilder = DOUBLE.createBlockBuilder(null, 1);
        DOUBLE.writeDouble(blockBuilder, percentile);
        return new RunLengthEncodedBlock(blockBuilder.build(), positionCount);
    }

    private static RunLengthEncodedBlock createRLEBlock(Iterable<Double> percentiles, int positionCount)
    {
        BlockBuilder rleBlockBuilder = new ArrayType(DOUBLE).createBlockBuilder(null, 1);
        BlockBuilder arrayBlockBuilder = rleBlockBuilder.beginBlockEntry();

        for (double percentile : percentiles) {
            DOUBLE.writeDouble(arrayBlockBuilder, percentile);
        }

        rleBlockBuilder.closeEntry();

        return new RunLengthEncodedBlock(rleBlockBuilder.build(), positionCount);
    }
}
