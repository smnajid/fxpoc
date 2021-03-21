package com.lodh.market.ftm;

import com.lodh.market.domain.Quote;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("ftm")
public class FtmController {

    public static final int SIZE = 10;

    @GetMapping(path = "quotes/{symbol}/{rfqId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Quote> getQuoteStream(@PathVariable String symbol, @PathVariable String rfqId) {
        Random r = new Random();
        return Flux.interval(Duration.ofSeconds(1))
                .map(i -> Quote.builder()
                        .rfqId(rfqId)
                        .quoteId(String.valueOf(i))
                        .symbol(symbol)
                        .price(100 * r.nextDouble())
                        .build()
                )
                .take(10);

    }


}
