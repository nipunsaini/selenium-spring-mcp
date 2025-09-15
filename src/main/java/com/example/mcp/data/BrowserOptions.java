package com.example.mcp.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrowserOptions(
        @ToolParam(description = "headless browser option e.g true, false")
        @JsonProperty("headless")
        boolean headless,
        @ToolParam(description = "browser arguments e.g --start-maximized, --start-fullscreen etc.")
        @JsonProperty("arguments")
        List<String> arguments
) {
}
