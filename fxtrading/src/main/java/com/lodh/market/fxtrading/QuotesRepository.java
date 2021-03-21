package com.lodh.market.fxtrading;

import com.lodh.market.domain.Quote;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;

public interface QuotesRepository extends ReactiveSortingRepository<Quote, String> {

    Flux<Quote> findByRfqId(String rfqId);
}
