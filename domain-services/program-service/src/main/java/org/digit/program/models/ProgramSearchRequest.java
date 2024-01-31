package org.digit.program.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgramSearchRequest {

    @JsonProperty("signature")
    private String signature;

    @NotNull
    @JsonProperty("header")
    RequestHeader header;

    @NotNull
    @JsonProperty("message")
    ProgramSearch programSearch;

}
