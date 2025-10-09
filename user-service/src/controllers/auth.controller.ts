import { Request, Response, NextFunction } from "express";
import { AuthService } from "services/auth.service";

class AuthController {
  private authService = new AuthService();

  public register = async (request: Request, response: Response, next: NextFunction) => {
    try {
      const createdUser = await this.authService.registerUser(request.body);
      if (!createdUser) return next("No user created");

      response.cookie(
        "Authentication",
        this.authService.createToken(createdUser.id),
        this.authService.createCookieOptions()
      );

      response.status(201).json(createdUser);
    } catch (error) {
      next(error);
    }
  };

  public login = async (request: Request, response: Response, next: NextFunction) => {
    try {
      const user = await this.authService.login(request.body);
      if (!user) return next("No user found");

      response.cookie(
        "Authentication",
        this.authService.createToken(user.id),
        this.authService.createCookieOptions()
      );

      response.json(user);
    } catch (error) {
      next(error);
    }
  };

  public isLoggedIn = async (request: Request, response: Response, next: NextFunction) => {
    try {
      const loggedUser = await this.authService.isLoggedIn(request.userId);
      if (!loggedUser) return next();

      response.json(loggedUser);
    } catch (error) {
      next(error);
    }
  };

  public logout = (_: Request, response: Response) => {
    response
      .setHeader("Set-Cookie", ["Authentication=; Max-Age=0; Path=/; HttpOnly"])
      .status(204)
      .end();
  };
}

export default AuthController;
