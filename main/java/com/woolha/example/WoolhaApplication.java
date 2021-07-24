package com.woolha.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
public class WoolhaApplication {

  public static void main(String[] args) {
    new SpringApplicationBuilder(WoolhaApplication.class).run(args);
  }
}
