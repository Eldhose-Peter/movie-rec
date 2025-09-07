## Movie Recommendation System

> [!NOTE]
> Distributed microservice-based recommender with Kafka event streaming

### Services

1. User Service
    - Stores user profiles & ratings.
    - Publishes user-ratings events to Kafka.
2. Similarity Service
    - Consumes user-ratings events.
    - Updates similarity graph / DB (Neo4j, Postgres, etc.).
    - Publishes similarity-updated events.    
3. Recommendation Service
    - Consumes similarity-updated events.
    - Precomputes Top-K recs per user.
    - Stores in Redis for real-time serving.
    
4. API Gateway / UI Service (Next.js)
    - Fetches recommendations from Redis.
    - Sends new ratings → User Service → Kafka.