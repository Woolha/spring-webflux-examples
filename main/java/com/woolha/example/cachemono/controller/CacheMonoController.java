package com.woolha.example.cachemono.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

@RestController
@RequestMapping("/cache-mono")
public class CacheMonoController {

  public enum TYPE {
    FUNCTION,
    MAP,
    MAP_CLASS,
    CAFFEINE,
  }

  final Map<String, String> mapStringCache = new HashMap<>();
  final Map<String, Signal<? extends String>> mapStringSignalCache = new HashMap<>();
  final Map<String, Signal<?>> mapObjectSignalCache = new HashMap<>();
  final Cache<String, String> caffeineCache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(30))
      .recordStats()
      .build();

  private Mono<String> handleCacheMiss() {
    System.out.println("Cache miss!");

    return Mono.just(ZonedDateTime.now().toString());
  }

  private Mono<String> handleCacheMiss(String key) {
    System.out.println("Cache miss!");

    return Mono.just(key + ": " + Instant.now().toString());
  }

  @GetMapping
  public Mono<String> test(
      @RequestParam String key,
      @RequestParam TYPE type
  ) {

    if (type == TYPE.FUNCTION) {
      final Mono<String> cachedMono1 = CacheMono
          .lookup(
              k -> Mono.justOrEmpty(mapStringCache.get(key)).map(Signal::next),
              key
          )
//        .onCacheMissResume(this::handleCacheMiss) // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key))
          .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
              mapStringCache.put(k, Objects.requireNonNull(sig.get()))
          ));

      return cachedMono1
          .doOnNext(result -> System.out.println("Result is " + result));
    } else if (type == TYPE.MAP) {
      final Mono<String> cachedMono2 = CacheMono
          .lookup(mapStringSignalCache, key)
//        .onCacheMissResume(this::handleCacheMiss) // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key));

      return cachedMono2
          .doOnNext(result -> System.out.println("Result is " + result));
    } else if (type == TYPE.MAP_CLASS) {
      final Mono<String> cachedMono3 = CacheMono
          .lookup(mapObjectSignalCache, key, String.class)
//        .onCacheMissResume(this::handleCacheMiss) // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key));

    return cachedMono3
        .doOnNext(result -> System.out.println("Result is " + result));
    } else if (type == TYPE.CAFFEINE) {
      final Mono<String> cachedMonoCaffeine = CacheMono
          .lookup(
              k -> Mono.justOrEmpty(caffeineCache.getIfPresent(k)).map(Signal::next),
              key
          )
//        .onCacheMissResume(this::handleCacheMiss) // Uncomment this if you want to pass a Supplier
          .onCacheMissResume(this.handleCacheMiss(key))
          .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
              caffeineCache.put(k, Objects.requireNonNull(sig.get()))
          ));

      return cachedMonoCaffeine
          .doOnNext(result -> System.out.println("Result is " + result));
    }

    return Mono.empty();
  }
}
