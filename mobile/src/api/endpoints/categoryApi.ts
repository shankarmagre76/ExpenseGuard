import { apiClient } from '../client';
import { CategoryRequest, CategoryResponse } from '../../types/category';

export const getCategories = async (): Promise<CategoryResponse[]> => {
  return apiClient.get<CategoryResponse[]>('/api/v1/categories');
};

export const getCategoryById = async (id: string): Promise<CategoryResponse> => {
  return apiClient.get<CategoryResponse>(`/api/v1/categories/${id}`);
};

export const createCategory = async (data: CategoryRequest): Promise<CategoryResponse> => {
  return apiClient.post<CategoryResponse>('/api/v1/categories', data);
};

export const updateCategory = async (id: string, data: CategoryRequest): Promise<CategoryResponse> => {
  return apiClient.put<CategoryResponse>(`/api/v1/categories/${id}`, data);
};

export const deleteCategory = async (id: string): Promise<void> => {
  return apiClient.delete<void>(`/api/v1/categories/${id}`);
};
