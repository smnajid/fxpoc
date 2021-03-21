package com.lodh.market.fxtrading;

import com.lodh.market.domain.Quote;
import com.lodh.market.domain.RequestForQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@RestController
@RequestMapping("ftm-bridge")
@Slf4j
public class FtmBridgeController {

    private final QuotesRepository quotesRepository;
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public FtmBridgeController(QuotesRepository quotesRepository, ReactiveMongoTemplate reactiveMongoTemplate) {
        this.quotesRepository = quotesRepository;
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }


    @PostMapping(path = "quote")
    public Mono<Void> quoteReply(@RequestBody Quote quote) {
        log.info("quoteReply: {}", quote);
        return reactiveMongoTemplate.insert(quote)
                .then();
    }

    @PutMapping(path = "rfq/{rfqId}")
    public Mono<Void> rfqReply(@PathVariable String rfqId,@RequestBody RequestForQuote rfq) {
        log.info("rfq-reply: {}", rfq);
        return reactiveMongoTemplate.findAndReplace(query(where("rfqId").is(rfqId)),rfq)
                .then();
    }
}
