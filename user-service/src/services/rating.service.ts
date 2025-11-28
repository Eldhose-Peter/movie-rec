import { RatingRepository } from "repositories/rating.repository";
import { CreateRatingPayload, UpdateRatingPayload } from "validation/rating.validation";

export class RatingService {
  private ratingRepository = new RatingRepository();

  public async createRating(userId: number, payload: CreateRatingPayload) {
    return this.ratingRepository.createRating(userId, payload);
  }

  public async getUserRatings(userId: number) {
    return this.ratingRepository.findRatingsByUserId(userId);
  }

  public async updateRating(userId: number, payload: UpdateRatingPayload) {
    return this.ratingRepository.updateRating(userId, payload);
  }

  public async deleteRating(userId: number, movieId: number) {
    return this.ratingRepository.deleteRating(userId, movieId);
  }
}
