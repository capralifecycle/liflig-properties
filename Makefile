.PHONY: all
all: build

.PHONY: build
build:
	mvn clean verify

.PHONY: test
test:
	mvn test

.PHONY: lint
lint:
	mvn spotless:check

.PHONY: lint-fix
lint-fix:
	mvn spotless:apply

.PHONY: clean
clean:
	mvn clean
