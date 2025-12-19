import amqp, { Channel, ChannelModel } from "amqplib";

class QueueService {
  private connection: ChannelModel | null = null;
  private channel: Channel | null = null;
  private readonly queueUrl = process.env.RABBITMQ_URL || "amqp://guest:guest@localhost:5672";
  private readonly exchangeName = "rating.exchange";

  async connect() {
    try {
      // Connect to RabbitMQ container
      this.connection = await amqp.connect(this.queueUrl);
      this.channel = await this.connection.createChannel();

      // Assert the exchange exists (Topic Exchange)
      await this.channel.assertExchange(this.exchangeName, "topic", {
        durable: true
      });

      console.log("✅ Connected to RabbitMQ");
    } catch (error) {
      console.error("❌ RabbitMQ Connection Failed:", error);
      // specific retry logic can be added here
    }
  }

  async publishRating(raterId: number, movieId: number, rating: number) {
    if (!this.channel) {
      await this.connect();
    }

    const routingKey = "rating.created";
    const message = {
      raterId,
      movieId,
      rating
    };

    // Buffer the JSON string
    const buffer = Buffer.from(JSON.stringify(message));

    // Publish to the exchange
    this.channel!.publish(this.exchangeName, routingKey, buffer);
    console.log(`📨 Event Published: User ${raterId} rated Movie ${movieId}`);
  }
}

export const queueService = new QueueService();
