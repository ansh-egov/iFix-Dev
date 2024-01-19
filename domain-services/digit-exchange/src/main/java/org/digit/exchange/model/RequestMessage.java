package org.digit.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Setter
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class RequestMessage {

    @JsonProperty("signature")
    private String signature;
    @JsonProperty("header")
    @NotNull
    private RequestHeader header;
    @NotNull
    @JsonProperty("message")
    private String message;
}
