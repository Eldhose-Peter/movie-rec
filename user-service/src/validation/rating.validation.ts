import { z } from "zod";

export const createRatingSchema = z.object({
  body: z.object({
    movieId: z.number().positive(),
    rating: z.number().min(1).max(10),
    comment: z.string().optional()
  })
});

export type CreateRatingPayload = z.infer<typeof createRatingSchema>["body"];

export const updateRatingSchema = z.object({
  body: z.object({
    movieId: z.number().positive(),
    rating: z.number().min(1).max(10).optional(),
    comment: z.string().optional()
  })
});

export const deleteRatingSchema = z.object({
  params: z.object({
    movieId: z
      .string()
      .transform((val) => parseInt(val))
      .pipe(z.number().positive())
  })
});

export type UpdateRatingPayload = z.infer<typeof updateRatingSchema>["body"];
