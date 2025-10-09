import AuthController from "controllers/auth.controller";
import { RouterInterface } from "interfaces/routes.interfaces";
import authMiddleware from "middleware/auth.middleware";
import validationMiddleware from "middleware/validation.middleware";
import { registerSchema, loginSchema } from "validation/auth.validation";

export class AuthRoutes extends RouterInterface {
  private controller = new AuthController();
  constructor() {
    super("/auth");
    this.initializeRoutes();
  }

  protected initializeRoutes() {
    this.router.post(
      `${this.path}/register`,
      validationMiddleware(registerSchema),
      this.controller.register
    );
    this.router.post(
      `${this.path}/login`,
      validationMiddleware(loginSchema),
      this.controller.login
    );
    this.router.get(`${this.path}/me`, authMiddleware, this.controller.isLoggedIn);
    this.router.delete(`${this.path}/logout`, authMiddleware, this.controller.logout);
  }
}
