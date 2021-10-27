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

import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@State(Scope.Thread)
public class FormatterBenchmark {

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * Does (should?) have no effect, since {@link #pattern}s must be supported by {@link FixedDateFormat}, which doesn't have any locale support.
     */
    private static final Locale LOCALE = Locale.US;

    private static final int INSTANT_COUNT = 1_000;

    /**
     * Date & time format patterns supported by all formatters and produce the same output.
     */
    @Param({
            "HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
    })
    public String pattern;

    @Param({
            "shuffled",     // raw speed
            "monoincr"      // to exploit implicit caching
    })
    public String timeFlow;

    private Formatter log4jFdf;

    private Formatter commonsFdf;

    private Formatter javaDtf;

    @Setup
    public void setupFormatters(final BenchmarkParams params) {
        final String pattern = params.getParam("pattern");
        final String timeFlow = params.getParam("timeFlow");
        final Instant[] instants = createInstants(timeFlow);
        log4jFdf = new Log4jFixedDateFormat(instants, pattern);
        commonsFdf = new CommonsFastDateFormat(instants, pattern);
        javaDtf = new JavaDateTimeFormatter(instants, pattern);
    }

    private static Instant[] createInstants(final String timeFlow) {
        if ("shuffled".equalsIgnoreCase(timeFlow)) {
            return createShuffledInstants();
        } else if ("monoincr".equalsIgnoreCase(timeFlow)) {
            return createMonotonicallyIncreasingInstants();
        }
        throw new IllegalArgumentException("unknown instant collection pattern: " + timeFlow);
    }

    private static Instant[] createShuffledInstants() {
        final Instant loInstant = Instant.EPOCH;
        final Instant hiInstant = loInstant.plus(Duration.ofDays(1));           // This is necessary to avoid choking at `FixedDateTime#millisSinceMidnight(long)`, which is supposed to be executed once a day in practice.
        final long maxOffsetNanos = Duration.between(loInstant, hiInstant).toNanos();
        final Random random = new Random(0);
        return IntStream
                .range(0, INSTANT_COUNT)
                .mapToObj(ignored -> {
                    final long offsetNanos = (long) Math.floor(random.nextDouble() * maxOffsetNanos);
                    return loInstant.plus(offsetNanos, ChronoUnit.NANOS);
                })
                .toArray(Instant[]::new);
    }

    private static Instant[] createMonotonicallyIncreasingInstants() {
        Random random = new Random(0);
        Instant[] instants = new Instant[INSTANT_COUNT];
        instants[0] = Instant.EPOCH;
        for (int instantIndex = 1; instantIndex < INSTANT_COUNT; instantIndex++) {
            boolean repeating = random.nextDouble() < 0.3D;                     // 30% of the instants supposed to be repeating.
            long offsetNanos = !repeating
                    ? ((long) Math.floor(random.nextDouble() * 1e9))            // Max. 1 seconds between adjacent and distinct instants.
                                                                                // This is necessary to avoid choking at `FixedDateTime#millisSinceMidnight(long)`, which is supposed to be executed once a day in practice.
                    : 0;
            instants[instantIndex] = instants[instantIndex - 1].plus(offsetNanos, ChronoUnit.NANOS);
        }
        return instants;
    }

    @FunctionalInterface
    interface Formatter {

        void benchmark(Blackhole blackhole);

    }

    private static final class Log4jFixedDateFormat implements Formatter {

        private final org.apache.logging.log4j.core.time.Instant[] log4jInstants;

        private final char[] buffer;

        private final FixedDateFormat formatter;

        private Log4jFixedDateFormat(final Instant[] instants, final String pattern) {
            this.log4jInstants = Stream
                    .of(instants)
                    .map(instant -> {
                        final MutableInstant log4jInstant = new MutableInstant();
                        log4jInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
                        return log4jInstant;
                    })
                    .toArray(org.apache.logging.log4j.core.time.Instant[]::new);
            this.buffer = new char[pattern.length()];
            this.formatter = Objects.requireNonNull(FixedDateFormat.createIfSupported(pattern, TIME_ZONE.getID()));
        }

        @Override
        public void benchmark(final Blackhole blackhole) {
            for (final org.apache.logging.log4j.core.time.Instant log4jInstant : log4jInstants) {
                blackhole.consume(formatter.formatInstant(log4jInstant, buffer, 0));
            }
        }

    }

    private static final class CommonsFastDateFormat implements Formatter {

        private final Calendar[] calendars;

        private final StringBuilder stringBuilder = new StringBuilder();

        private final FastDateFormat fastDateFormat;

        private CommonsFastDateFormat(final Instant[] instants, final String pattern) {
            this.calendars = Arrays
                    .stream(instants)
                    .map(instant -> {
                        final Calendar calendar = Calendar.getInstance(TIME_ZONE, LOCALE);
                        calendar.setTimeInMillis(instant.toEpochMilli());
                        return calendar;
                    })
                    .toArray(Calendar[]::new);
            this.fastDateFormat = FastDateFormat.getInstance(pattern, TIME_ZONE, LOCALE);
        }

        @Override
        public void benchmark(final Blackhole blackhole) {
            for (final Calendar calendar : calendars) {
                stringBuilder.setLength(0);
                fastDateFormat.format(calendar, stringBuilder);
                blackhole.consume(stringBuilder.length());
            }
        }

    }

    private static final class JavaDateTimeFormatter implements Formatter {

        private final Instant[] instants;

        private final StringBuilder stringBuilder = new StringBuilder();

        private final DateTimeFormatter dateTimeFormatter;

        private JavaDateTimeFormatter(final Instant[] instants, final String pattern) {
            this.instants = instants;
            this.dateTimeFormatter = DateTimeFormatter
                    .ofPattern(pattern, LOCALE)
                    .withZone(TIME_ZONE.toZoneId());
        }

        @Override
        public void benchmark(final Blackhole blackhole) {
            for (final Instant instant : instants) {
                stringBuilder.setLength(0);
                dateTimeFormatter.formatTo(instant, stringBuilder);
                blackhole.consume(stringBuilder.length());
            }
        }

    }

    @Benchmark
    public void log4jFdf(final Blackhole blackhole) {
        log4jFdf.benchmark(blackhole);
    }

    @Benchmark
    public void commonsFdf(final Blackhole blackhole) {
        commonsFdf.benchmark(blackhole);
    }

    @Benchmark
    public void javaDtf(final Blackhole blackhole) {
        javaDtf.benchmark(blackhole);
    }

}
