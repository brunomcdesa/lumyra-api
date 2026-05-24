JAVA_HOME ?= $(HOME)/.jdks/ms-25.0.3
MVNW      := JAVA_HOME=$(JAVA_HOME) ./mvnw

.PHONY: build compile test run clean checkstyle

build:
	$(MVNW) package -DskipTests -B

compile:
	$(MVNW) compile

test:
	$(MVNW) test

run:
	$(MVNW) spring-boot:run -Dspring-boot.run.profiles=dev

clean:
	$(MVNW) clean

checkstyle:
	$(MVNW) checkstyle:check
