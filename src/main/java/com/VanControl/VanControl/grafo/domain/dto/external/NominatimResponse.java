package com.VanControl.VanControl.grafo.domain.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResponse(
        String lat,
        String lon,
        String display_name
) {
}