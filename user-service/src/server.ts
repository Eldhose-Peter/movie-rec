import App from "app";
import { AuthRoutes } from "routes/auth.routes";
import { RatingRoutes } from "routes/rating.routes";

const app = new App([new AuthRoutes(), new RatingRoutes()]);
app.listen();
