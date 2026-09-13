import { useState, useCallback, useEffect } from 'react';
import { CategoryResponse, CategoryRequest, CategoryType } from '../types/category';
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
} from '../api/endpoints/categoryApi';
import { AppError } from '../types/api';

export const useCategories = (typeFilter?: CategoryType) => {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getCategories();
      setCategories(data);
    } catch (err: any) {
      const appErr = err as AppError;
      setError(appErr.message || 'Failed to load categories.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const filteredCategories = typeFilter
    ? categories.filter((cat) => cat.type === typeFilter)
    : categories;

  const addCategory = async (request: CategoryRequest): Promise<CategoryResponse> => {
    const newCategory = await createCategory(request);
    await refresh();
    return newCategory;
  };

  const editCategory = async (id: string, request: CategoryRequest): Promise<CategoryResponse> => {
    const updated = await updateCategory(id, request);
    await refresh();
    return updated;
  };

  const removeCategory = async (id: string): Promise<void> => {
    await deleteCategory(id);
    await refresh();
  };

  return {
    categories: filteredCategories,
    allCategories: categories,
    loading,
    error,
    refresh,
    addCategory,
    editCategory,
    removeCategory,
  };
};
