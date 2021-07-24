package com.woolha.example.cacheflux.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;

@RestController
@RequestMapping("/cache-flux")
public class CacheFluxController {

  public enum TYPE {
    FUNCTION,
    MAP,
    CAFFEINE,
  }

  final Map<Integer, List<Integer>> mapIntCache = new HashMap<>();
  final Map<Integer, List> mapCache = new HashMap<>();
  final Cache<Integer, List<Integer>> caffeineCache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(30))
      .recordStats()
      .build();

  private Flux<Integer> handleCacheMiss() {
    System.out.println("Cache miss!");
    final List<Integer> values = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      values.add(i);
    }

    return Flux.fromIterable(values);
  }

  private Flux<Integer> handleCacheMiss(Integer key) {
    System.out.println("Cache miss!");
    final List<Integer> values = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      values.add(i * key);
    }

    return Flux.fromIterable(values);
  }

  @GetMapping
  public Flux<Integer> test(
      @RequestParam Integer key,
      @RequestParam TYPE type
  ) {
    if (type == TYPE.FUNCTION) {
      final Flux<Integer> cachedFlux1 = CacheFlux
          .lookup(
              k -> {
                if (mapIntCache.get(k) != null) {
                  return Flux.fromIterable(mapIntCache.get(k))
                      .map(Signal::next)
                      .collectList();
                } else {
                  return Mono.empty();
                }
              },
              key
          )
//        .onCacheMissResume(this::handleCacheMiss) // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key))
          .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
              mapCache.put(
                  k,
                  sig.stream()
                      .filter(signal -> signal.getType() == SignalType.ON_NEXT)
                      .map(Signal::get)
                      .collect(Collectors.toList())
              )
          ));

      return cachedFlux1
          .doOnNext(res -> System.out.println("Result is " + res));
    } else if (type == TYPE.MAP) {
      final Flux<Integer> cachedFlux2 = CacheFlux
          .lookup(
              mapCache,
              key,
              Integer.class
          )
//        .onCacheMissResume(this::handleCacheMiss); // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key));

      return cachedFlux2
          .doOnNext(res -> System.out.println("Result is " + res));
    } else if (type == TYPE.CAFFEINE) {
      final Flux<Integer> cachedFluxCaffeine = CacheFlux
          .lookup(
              k -> {
                final List<Integer> cached = caffeineCache.getIfPresent(k);

                if (cached == null) {
                  return Mono.empty();
                }

                return Mono.just(cached)
                    .flatMapMany(Flux::fromIterable)
                    .map(Signal::next)
                    .collectList();
              },
              key
          )
//        .onCacheMissResume(this::handleCacheMiss) // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key))
          .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
              caffeineCache.put(
                  k,
                  sig.stream()
                      .filter(signal -> signal.getType() == SignalType.ON_NEXT)
                      .map(Signal::get)
                      .collect(Collectors.toList())
              )
          ));

      return cachedFluxCaffeine
          .doOnNext(res -> System.out.println("Result is " + res));
    }

    return Flux.empty();
  }
}
