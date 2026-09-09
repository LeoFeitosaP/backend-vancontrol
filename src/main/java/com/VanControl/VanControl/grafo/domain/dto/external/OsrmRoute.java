package com.VanControl.VanControl.grafo.domain.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRoute(
        double distance,
        double duration
) {
}