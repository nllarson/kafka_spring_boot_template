package com.improving.example.demo;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.stream.Stream;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	NewTopic hobbitQuotes() {
		return TopicBuilder.name("hobbit-quotes").partitions(6).replicas(3).build();
	}

	@RequiredArgsConstructor
	@Component
	class Producer {

		private final KafkaTemplate<Integer, Hobbit> template;

		Faker faker;

		@EventListener(ApplicationStartedEvent.class)
		public void generate() {

			faker = Faker.instance();
			final Flux<Long> interval = Flux.interval(Duration.ofMillis(1_000));

			final Flux<Hobbit> quotes = Flux.fromStream(Stream.generate(() -> new Hobbit(faker.hobbit().quote())));

			Flux.zip(interval, quotes)
					.map(it -> template.send("hobbit-quotes", faker.random().nextInt(42), it.getT2())).blockLast();
		}

	}


	@Component
	class Consumer {

		@KafkaListener(topics = {"hobbit-quotes"}, groupId = "spring-boot-kafka")
		public void consume(ConsumerRecord<Integer, Hobbit> record) {
			System.out.println("received = " + record.value() + " with key " + record.key());
		}
	}


}
