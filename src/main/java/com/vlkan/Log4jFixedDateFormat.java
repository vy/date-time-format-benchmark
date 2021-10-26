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
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Log4jFixedDateFormat implements Formatter {

    private final org.apache.logging.log4j.core.time.Instant[] log4jInstants;

    private final char[] buffer;

    private final FixedDateFormat formatter;

    Log4jFixedDateFormat(TimeZone timeZone, Instant[] instants, String pattern) {
        this.log4jInstants = Stream
                .of(instants)
                .map(instant -> {
                    MutableInstant log4jInstant = new MutableInstant();
                    log4jInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
                    return log4jInstant;
                })
                .toArray(org.apache.logging.log4j.core.time.Instant[]::new);
        this.buffer = new char[pattern.length()];
        this.formatter = Objects.requireNonNull(FixedDateFormat.createIfSupported(pattern, timeZone.getID()));
    }

    @Override
    public void benchmark(Blackhole blackhole) {
        for (org.apache.logging.log4j.core.time.Instant log4jInstant : log4jInstants) {
            blackhole.consume(formatter.formatInstant(log4jInstant, buffer, 0));
        }
    }

    @Override
    public List<String> format() {
        return Arrays
                .stream(log4jInstants)
                .map(log4jInstant -> {
                    int length = formatter.formatInstant(log4jInstant, buffer, 0);
                    return new String(buffer, 0, length);
                })
                .collect(Collectors.toList());
    }

}
