/**
 * Category type definitions matching Spring Boot backend DTOs & CategoryType enum.
 */

export type CategoryType = 'INCOME' | 'EXPENSE';

export interface CategoryRequest {
  name: string;
  type: CategoryType;
}

export interface CategoryResponse {
  id: string;
  name: string;
  type: CategoryType;
  createdAt: string;
  updatedAt: string;
}
