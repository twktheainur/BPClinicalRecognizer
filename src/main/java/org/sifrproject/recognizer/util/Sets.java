package org.sifrproject.recognizer.util;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public enum Sets {
;
    public static Set<Long> intersection(final Collection<Long> setA, final Set<Long> setB) {
        return setA
                .stream()
                .filter(setB::contains)
                .collect(Collectors.toSet());

    }

}
