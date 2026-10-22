package com.VanControl.VanControl.grafo.domain.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmResponse(
        String code,
        List<OsrmRoute> routes
) {
}