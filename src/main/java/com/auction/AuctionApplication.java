package com.auction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuctionApplication {

  private static final Logger log = LoggerFactory.getLogger(AuctionApplication.class);

  public static void main(String[] args) {
    System.out.println("Hello Bob the Builder");
    log.info("Starting AuctionApplication");
    SpringApplication.run(AuctionApplication.class, args);
  }
}
