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

import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

final class CommonsFastDateFormat implements Formatter {

    private final Calendar[] calendars;

    private final StringBuilder stringBuilder = new StringBuilder();

    private final FastDateFormat fastDateFormat;

    CommonsFastDateFormat(TimeZone timeZone, Locale locale, Instant[] instants, String pattern) {
        this.calendars = Arrays
                .stream(instants)
                .map(instant -> {
                    Calendar calendar = Calendar.getInstance(timeZone, locale);
                    calendar.setTimeInMillis(instant.toEpochMilli());
                    return calendar;
                })
                .toArray(Calendar[]::new);
        this.fastDateFormat = FastDateFormat.getInstance(pattern, timeZone, locale);
    }

    @Override
    public void benchmark(Blackhole blackhole) {
        for (Calendar calendar : calendars) {
            stringBuilder.setLength(0);
            fastDateFormat.format(calendar, stringBuilder);
            blackhole.consume(stringBuilder.length());
        }
    }

    @Override
    public List<String> format() {
        return Arrays
                .stream(calendars)
                .map(calendar -> {
                    stringBuilder.setLength(0);
                    fastDateFormat.format(calendar, stringBuilder);
                    return stringBuilder.toString();
                })
                .collect(Collectors.toList());
    }

}
