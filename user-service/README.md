# User Service
- Stores user profiles & ratings.
- Publishes user-ratings events to Kafka. 

## Technologies used
- Node Backend
- Postgres DB
- NextJS UI
- Typescript
- Docker

## Pre-requisites

- node -> v22 LTS
- docker
- .env file, refer .env.example

## Starting project

Building backend Node Application
- extract data-set.zip file in db-init
- `npm install`
- `npm run prestart`
- `npm run build`
- `npm run start`
- `docker compose up`
- refer README in /client to build NextJS UI