import pool from "config/database";
import { Rating } from "models/ratings.model";
import { CreateRatingPayload, UpdateRatingPayload } from "validation/rating.validation";

export class RatingRepository {
  public async createRating(userId: number, payload: CreateRatingPayload) {
    const result = await pool.query<Rating>(
      "INSERT INTO ratings (rater_id, movie_id, rating) VALUES ($1, $2, $3) RETURNING *",
      [userId, payload.movieId, payload.rating]
    );
    return result.rows[0];
  }

  public async findRatingsByUserId(userId: number) {
    const result = await pool.query<Rating>("SELECT * FROM ratings WHERE rater_id = $1", [userId]);
    return result.rows;
  }

  public async updateRating(userId: number, payload: UpdateRatingPayload) {
    const result = await pool.query<Rating>(
      "UPDATE ratings SET rating = COALESCE($1, rating) WHERE rater_id = $2 AND movie_id = $3 RETURNING *",
      [payload.rating, userId, payload.movieId]
    );
    return result.rows[0];
  }

  public async deleteRating(userId: number, movieId: number) {
    await pool.query("DELETE FROM ratings WHERE rater_id = $1 AND movie_id = $2", [
      userId,
      movieId
    ]);
  }
}
