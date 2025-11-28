import { Request, Response, NextFunction } from "express";
import { RatingService } from "services/rating.service";
import { CreateRatingPayload } from "validation/rating.validation";

class RatingController {
  private ratingService = new RatingService();

  public createRating = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const payload: CreateRatingPayload = req.body;
      const userId = req.userId as number;

      const createdRating = await this.ratingService.createRating(userId, payload);
      res.status(201).json(createdRating);
    } catch (error) {
      next(error);
    }
  };

  public getUserRatings = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.userId as number;
      const ratings = await this.ratingService.getUserRatings(userId);
      res.json(ratings);
    } catch (error) {
      next(error);
    }
  };

  public updateRating = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.userId as number;
      const payload = req.body;
      const updatedRating = await this.ratingService.updateRating(userId, payload);
      res.json(updatedRating);
    } catch (error) {
      next(error);
    }
  };

  public deleteRating = async (req: Request, res: Response, next: NextFunction) => {
    try {
      const userId = req.userId as number;
      const movieId = req.params.movieId as unknown as number;
      await this.ratingService.deleteRating(userId, movieId);
      res.status(204).end();
    } catch (error) {
      next(error);
    }
  };
}

export default RatingController;
