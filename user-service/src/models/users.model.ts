import { RegisterPayload } from "validation/auth.validation";

export interface User extends RegisterPayload {
  id: number;
  created_at: Date;
}
