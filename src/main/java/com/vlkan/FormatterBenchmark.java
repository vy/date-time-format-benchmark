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

import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.IntStream;

@State(Scope.Thread)
public class FormatterBenchmark {

    static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * Does (should?) have no effect, since {@link #pattern}s must be supported by {@link FixedDateFormat}, which doesn't have any locale support.
     */
    static final Locale LOCALE = Locale.US;

    static final Instant[] INSTANTS = createInstants();

    private static Instant[] createInstants() {
        Instant loInstant = Instant.EPOCH;
        Instant hiInstant = Instant.parse("2021-10-25T19:50:00Z");
        long maxOffsetNanos = Duration.between(loInstant, hiInstant).toNanos();
        Random random = new Random(0);
        return IntStream
                .range(0, 1_000)
                .mapToObj(ignored -> {
                    long offsetNanos = random.nextLong(maxOffsetNanos);
                    return loInstant.plus(offsetNanos, ChronoUnit.NANOS);
                })
                .toArray(Instant[]::new);
    }

    /**
     * Date & time format patterns supported by all formatters and produce the same output.
     */
    @Param({
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
    public String pattern;

    Formatter log4jFdf;

    Formatter commonsFdf;

    Formatter javaDtf;

    @Setup
    public void setupFormatters(BenchmarkParams params) {
        String pattern = params.getParam("pattern");
        log4jFdf = Log4jFixedDateFormatFactory.INSTANCE.create(TIME_ZONE, LOCALE, INSTANTS, pattern);
        commonsFdf = CommonsFastDateFormatFactory.INSTANCE.create(TIME_ZONE, LOCALE, INSTANTS, pattern);
        javaDtf = JavaDateTimeFormatterFactory.INSTANCE.create(TIME_ZONE, LOCALE, INSTANTS, pattern);
    }

    @Benchmark
    public void log4jFdf(Blackhole blackhole) {
        log4jFdf.benchmark(blackhole);
    }

    @Benchmark
    public void commonsFdf(Blackhole blackhole) {
        commonsFdf.benchmark(blackhole);
    }

    @Benchmark
    public void javaDtf(Blackhole blackhole) {
        javaDtf.benchmark(blackhole);
    }

    public static void main(String[] args) throws RunnerException {
        fixJavaClassPath();
        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder()
                .include(FormatterBenchmark.class.getSimpleName())
                .warmupTime(TimeValue.seconds(20))
                .warmupIterations(3)
                .measurementTime(TimeValue.seconds(30))
                .measurementIterations(4)
                .forks(3);
        configJmhQuickRun(optionsBuilder);
        Options options = optionsBuilder.build();
        new Runner(options).run();
    }

    /**
     * Add project dependencies to <code>java.class.path</code> property used by JMH.
     *
     * @see <a href="https://stackoverflow.com/q/35574688/1278899">How to Run a JMH Benchmark in Maven Using exec:java Instead of exec:exec</a>
     */
    private static void fixJavaClassPath() {
        URLClassLoader classLoader = (URLClassLoader) FormatterBenchmark.class.getClassLoader();
        StringBuilder classpathBuilder = new StringBuilder();
        for (URL url : classLoader.getURLs()) {
            String urlPath = url.getPath();
            classpathBuilder.append(urlPath).append(File.pathSeparator);
        }
        String classpath = classpathBuilder.toString();
        System.setProperty("java.class.path", classpath);
    }

    private static void configJmhQuickRun(ChainedOptionsBuilder optionsBuilder) {
        String quick = System.getProperty("benchmark.quick");
        if (quick != null) {
            optionsBuilder
                    .forks(0)
                    .warmupIterations(0)
                    .measurementIterations(1)
                    .measurementTime(TimeValue.seconds(3));
        }
    }

}
