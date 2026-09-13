import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { getApiBaseUrl, ENV } from '../config/env';
import { parseApiError } from '../utils/errorHandler';
import { getSecureToken } from '../storage/secureStorage';

type UnauthorizedHandler = () => void;

class ApiClient {
  private instance: AxiosInstance;
  private unauthorizedHandler: UnauthorizedHandler | null = null;
  private activeToken: string | null = null;

  constructor() {
    this.instance = axios.create({
      baseURL: getApiBaseUrl(),
      timeout: ENV.API_TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    });

    this.setupInterceptors();
  }

  public setAuthToken(token: string | null): void {
    this.activeToken = token;
    if (token) {
      this.instance.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      delete this.instance.defaults.headers.common['Authorization'];
    }
  }

  public setUnauthorizedHandler(handler: UnauthorizedHandler): void {
    this.unauthorizedHandler = handler;
  }

  private setupInterceptors(): void {
    // Request Interceptor: Attach Bearer token from storage if available
    this.instance.interceptors.request.use(
      async (config: InternalAxiosRequestConfig) => {
        const token = this.activeToken || (await getSecureToken());
        if (token && config.headers && !config.headers.Authorization) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response Interceptor: Parse errors & handle 401 Unauthorized
    this.instance.interceptors.response.use(
      (response: AxiosResponse) => response,
      (error) => {
        const parsed = parseApiError(error);
        if (parsed.status === 401 && this.unauthorizedHandler) {
          this.unauthorizedHandler();
        }
        return Promise.reject(parsed);
      }
    );
  }

  public setBaseUrl(url: string): void {
    this.instance.defaults.baseURL = url;
  }

  public getBaseUrl(): string {
    return this.instance.defaults.baseURL || getApiBaseUrl();
  }

  public async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.get<T>(url, config);
    return response.data;
  }

  public async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.post<T>(url, data, config);
    return response.data;
  }

  public async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.put<T>(url, data, config);
    return response.data;
  }

  public async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.delete<T>(url, config);
    return response.data;
  }
}

export const apiClient = new ApiClient();
