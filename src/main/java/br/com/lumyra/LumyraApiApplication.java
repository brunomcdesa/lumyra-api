package br.com.lumyra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LumyraApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumyraApiApplication.class, args);
    }
}
