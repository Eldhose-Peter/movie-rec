import { AuthController } from "auth/auth.controller";
import App from "../app";
import UsersController from "users/users.controller";
import MovieController from "movies/movies.controller";

const app = new App([
  new AuthController(),
  new UsersController(),
  new MovieController(),
]);
app.listen();
