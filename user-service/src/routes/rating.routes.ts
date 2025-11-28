import RatingController from "controllers/rating.controller";
import { RouterInterface } from "interfaces/routes.interfaces";
import authMiddleware from "middleware/auth.middleware";
import validationMiddleware from "middleware/validation.middleware";
import {
  createRatingSchema,
  deleteRatingSchema,
  updateRatingSchema
} from "validation/rating.validation";

export class RatingRoutes extends RouterInterface {
  private controller = new RatingController();
  constructor() {
    super("/ratings");
    this.initializeRoutes();
  }

  protected initializeRoutes() {
    this.router.post(
      `${this.path}/`,
      authMiddleware,
      validationMiddleware(createRatingSchema),
      this.controller.createRating
    );
    this.router.get(`${this.path}/`, authMiddleware, this.controller.getUserRatings);
    this.router.put(
      `${this.path}/`,
      authMiddleware,
      validationMiddleware(updateRatingSchema),
      this.controller.updateRating
    );
    this.router.delete(
      `${this.path}/:movieId`,
      authMiddleware,
      validationMiddleware(deleteRatingSchema),
      this.controller.deleteRating
    );
  }
}
