package com.lodh.market.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestForQuote {
    private String rfqId;
    private String symbol;
    private RfqStatus status;
}
