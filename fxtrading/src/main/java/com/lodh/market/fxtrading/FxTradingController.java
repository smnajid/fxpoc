package com.lodh.market.fxtrading;

import com.lodh.market.domain.Quote;
import com.lodh.market.domain.RequestForQuote;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Objects;
import java.util.UUID;

import static com.lodh.market.domain.RfqStatus.COMPLETED;
import static com.lodh.market.domain.RfqStatus.PENDING;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@RestController
@RequestMapping("fxtrading")
@Slf4j
public class FxTradingController {

    private final WebClient ftmClient = WebClient.create("http://localhost:8081");

    private final QuotesRepository quotesRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;


    public FxTradingController(
            QuotesRepository quotesRepository,
            ReactiveMongoTemplate reactiveMongoTemplate
    ) {
        this.quotesRepository = quotesRepository;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @GetMapping(path = "quotes/{symbol}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Quote> getQuoteStream(@PathVariable String symbol) {
        log.info("Processing quotes request ...");
        final String rfqId = UUID.randomUUID().toString();

        RequestForQuote requestForQuote = new RequestForQuote(rfqId, symbol, PENDING);

        Mono<Void> startRfq = ftmClient.post()
                .uri("/ftm/rfq")
//                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(requestForQuote), RequestForQuote.class)
                .retrieve()
                .bodyToMono(Void.class);

//        Flux<Quote> quotes = ftmClient.get().uri("/ftm/quotes/{symbol}/{rfqId}", symbol, rfqId)
//                .retrieve()
//                .bodyToFlux(Quote.class)
//                .flatMap(quotesRepository::save);

        reactiveMongoTemplate.save(requestForQuote)
                .then(startRfq)
                .subscribe();

        Flux<RequestForQuote> rfqFromDb = Mono.just(new RequestForQuote())
                .concatWith(
                        reactiveMongoTemplate.changeStream(RequestForQuote.class)
                                .watchCollection(RequestForQuote.class)
                                .filter(where("rfqId").is(rfqId))
                                .listen()
                                .log()
                                .map(ChangeStreamEvent::getBody)
                );


        Flux<Quote> quoteFromDb = reactiveMongoTemplate.changeStream(Quote.class)
                .watchCollection(Quote.class)
                .filter(where("rfqId").is(rfqId))
                .listen()
                .log()
                .map(ChangeStreamEvent::getBody);

        return Flux.combineLatest(
                quoteFromDb,
                rfqFromDb,
                QuoteWithRfq::new)
                .takeUntil(quoteRfq -> Objects.equals(quoteRfq.getRfq().getStatus(), COMPLETED))
                .map(QuoteWithRfq::getQuote);
    }

    @Data
    @AllArgsConstructor
    class QuoteWithRfq {
        private final Quote quote;
        private final RequestForQuote rfq;
    }
}
