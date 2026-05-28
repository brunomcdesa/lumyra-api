# Aponte para uma JDK 21+ (Spring Boot 3.5 baseline = 21 LTS).
JAVA_HOME ?= $(HOME)/.jdks/ms-21
MVNW      := JAVA_HOME=$(JAVA_HOME) ./mvnw

.PHONY: build compile test verify run clean checkstyle up down

build:
	$(MVNW) package -DskipTests -B

compile:
	$(MVNW) compile

test:
	$(MVNW) test

verify:
	$(MVNW) verify

run:
	$(MVNW) spring-boot:run -Dspring-boot.run.profiles=local

clean:
	$(MVNW) clean

checkstyle:
	$(MVNW) checkstyle:check

up:
	docker compose up -d

down:
	docker compose down
