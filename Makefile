# Default to dev if no target is specified
ENV ?= dev

# Set variables based on the environment
ifeq ($(ENV), prod)
    ENV_FILE := .env.prod
    PROJECT_NAME := movie-prod
else
    ENV_FILE := .env.dev
    PROJECT_NAME := movie-dev
endif

# Docker Compose command shorthand
COMPOSE := docker-compose -p $(PROJECT_NAME) --env-file $(ENV_FILE)

.PHONY: up down restart build logs ps fresh_start deploy

up:
	@echo "Starting $(ENV) environment..."
	$(COMPOSE) up -d

down:
	@echo "Stopping $(ENV) environment..."
	$(COMPOSE) down

restart:
	@echo "Restarting $(ENV) environment..."
	$(COMPOSE) restart

build:
	@echo "Building images for $(ENV)..."
	$(COMPOSE) build

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

fresh_start: 
	@echo "Performing a fresh start for $(ENV) environment..."
	$(COMPOSE) down -v
	rm -rf pgdata
	rm -rf db-init
	mkdir -p db-init
	cp data/*.sql db-init/
	unzip -o data/data-set.zip -d db-init/
	$(COMPOSE) up -d --build


# Deployment command for CI/CD
deploy:
	@echo "Deploying $(ENV) update..."
	git pull
	$(COMPOSE) pull
	$(COMPOSE) up -d --build