package com.lodh.market.ftm;

import com.lodh.market.domain.Quote;
import com.lodh.market.domain.RequestForQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;

import static com.lodh.market.domain.RfqStatus.ACCEPTED;
import static com.lodh.market.domain.RfqStatus.COMPLETED;

@RestController
@RequestMapping("ftm")
@Slf4j
public class FtmController {

    private final WebClient fxTradingClient = WebClient.create("http://localhost:8080/ftm-bridge");

    public static final int SIZE = 10;
    Random r = new Random();

    @GetMapping(path = "quotes/{symbol}/{rfqId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Quote> getQuoteStream(@PathVariable String symbol, @PathVariable String rfqId) {

        return Flux.interval(Duration.ofSeconds(1))
                .map(quoteId -> buildQuote(symbol, rfqId, quoteId)
                )
                .log()
                .take(Duration.ofSeconds(SIZE));

    }

    @PostMapping(path = "rfq")
    public Mono<Void> startRfq(@RequestBody RequestForQuote rfq) {
        log.info("startRfq: {}", rfq);
        String symbol = rfq.getSymbol();
        String rfqId = rfq.getRfqId();

        rfq.setStatus(ACCEPTED);

        Flux<Void> sendTenQuotes = Flux.interval(Duration.ofSeconds(1))
                .map(quoteId -> buildQuote(symbol, rfqId, quoteId))
                .take(SIZE)
                .log()
                .flatMapSequential(this::replyWithQuote);

        return replyWithRfq(rfq)
                .thenMany(sendTenQuotes)
                .then(replyWithRfq(new RequestForQuote(rfqId, symbol, COMPLETED)))
                .thenMany(sendTenQuotes)
                .log()
                .then();
    }

    private Mono<Void> replyWithRfq(RequestForQuote rfq) {
        return fxTradingClient.put()
                .uri("/rfq/{rfqId}", rfq.getRfqId())
//                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(rfq), RequestForQuote.class)
                .retrieve()
                .bodyToMono(Void.class);
    }

    private Mono<Void> replyWithQuote(Quote quote) {
        return fxTradingClient.post()
                .uri("/quote")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(quote), RequestForQuote.class)
                .retrieve()
                .bodyToMono(Void.class);
    }


    private Quote buildQuote(String symbol, String rfqId, Long quoteId) {
        return Quote.builder()
                .rfqId(rfqId)
                .quoteId(String.valueOf(quoteId))
                .symbol(symbol)
                .price(100 * r.nextDouble())
                .build();
    }

}
