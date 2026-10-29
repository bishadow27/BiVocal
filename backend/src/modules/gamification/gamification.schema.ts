import { z } from 'zod';

export const limitQuerySchema = z.object({
  limit: z.string().regex(/^[1-9]\d*$/).transform(Number)
    .pipe(z.number().int().min(1).max(100)).optional().transform((value) => value ?? 20),
});
