/*
 * Copyright 2021 Volkan Yazıcı <volkan@yazi.ci>
 *
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
package com.vlkan;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.openjdk.jmh.infra.BenchmarkParams;

import java.util.List;

class FormatterBenchmarkTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "HH:mm:ss,SSS",
            "HH:mm:ss.SSS",
            "yyyyMMddHHmmssSSS",
            "dd MMM yyyy HH:mm:ss,SSS",
            "dd MMM yyyy HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss,SSS",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyyMMdd'T'HHmmss,SSS",
            "yyyyMMdd'T'HHmmss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss,SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
    })
    void all_formatters_should_produce_identical_output(String pattern) {

        // Instantiate and set up the benchmark.
        FormatterBenchmark benchmark = new FormatterBenchmark();
        BenchmarkParams benchmarkParams = Mockito.mock(BenchmarkParams.class);
        Mockito.when(benchmarkParams.getParam(Mockito.eq("pattern"))).thenReturn(pattern);
        benchmark.setupFormatters(benchmarkParams);

        // Compare Log4j FixedDateFormat against Java DateTimeFormatter.
        List<String> javaDtfOutput = benchmark.javaDtf.format();
        List<String> log4jFdfOutput = benchmark.log4jFdf.format();
        Assertions.assertThat(log4jFdfOutput).isEqualTo(javaDtfOutput);

        // Compare Commons FastDateFormat against Java DateTimeFormatter.
        List<String> commonsFdfOutput = benchmark.commonsFdf.format();
        Assertions.assertThat(commonsFdfOutput).isEqualTo(javaDtfOutput);

    }

}
