package org.os4all;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Os4AllApplication {

    public static void main(String[] args) {
        SpringApplication.run(Os4AllApplication.class, args);
    }
}
