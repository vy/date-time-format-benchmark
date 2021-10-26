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

import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

final class JavaDateTimeFormatter implements Formatter {

    private final Instant[] instants;

    private final StringBuilder stringBuilder = new StringBuilder();

    private final DateTimeFormatter dateTimeFormatter;

    JavaDateTimeFormatter(TimeZone timeZone, Locale locale, Instant[] instants, String pattern) {
        this.instants = instants;
        this.dateTimeFormatter = DateTimeFormatter
                .ofPattern(pattern, locale)
                .withZone(timeZone.toZoneId());
    }

    @Override
    public void benchmark(Blackhole blackhole) {
        for (Instant instant : instants) {
            stringBuilder.setLength(0);
            dateTimeFormatter.formatTo(instant, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Override
    public List<String> format() {
        return Arrays
                .stream(instants)
                .map(instant -> {
                    stringBuilder.setLength(0);
                    dateTimeFormatter.formatTo(instant, stringBuilder);
                    return stringBuilder.toString();
                })
                .collect(Collectors.toList());
    }

}
