help: ## Prints help for targets with comments
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

build: ## Build the mod (all loaders)
	./gradlew build

refresh: ## Refresh dependencies
	./gradlew --refresh-dependencies

clean: ## Clean build artifacts
	./gradlew clean

run-forge-client: ## Run Forge client
	./gradlew :forge:runClient

run-forge-server: ## Run Forge server
	./gradlew :forge:runServer

run-fabric-client: ## Run Fabric client
	./gradlew :fabric:runClient

run-fabric-server: ## Run Fabric server
	./gradlew :fabric:runServer

stop: ## Stop all Gradle daemons
	./gradlew --stop
