import axios from 'axios';
import type {AxiosInstance,AxiosRequestConfig,AxiosResponse,AxiosError,} from 'axios';

class ApiClient {
  private client: AxiosInstance;
  private baseUrl: string;

  constructor() {
    this.baseUrl = 'http://localhost:8080/api/';
    this.client = axios.create({
      baseURL: this.baseUrl,
      timeout: 10000,
      headers: { 'Content-Type': 'application/json' },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('authToken');
        if (token) {
          if (!config.headers) config.headers = {};
          (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error: unknown) => Promise.reject(this.handleError(error))
    );

    this.client.interceptors.response.use(
      (resp) => resp,
      (error: unknown) => {
        const err = this.handleError(error);

        if (axios.isAxiosError(error) && error.response?.status === 401) {
          localStorage.removeItem('authToken');
        }

        return Promise.reject(err);
      }
    );
  }


  private handleError(error: unknown): Error {
    if (axios.isAxiosError(error)) {
      const message =
        (error.response?.data as { message?: string } | undefined)?.message ??
        error.message ??
        'An error occurred (axios)';
      return new Error(message);
    }

    if (error instanceof Error) {
      return error;
    }

    try {
      return new Error(String(error));
    } catch {
      return new Error('An unknown error occurred');
    }
  }

  async get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await this.client.get(url, config);
    return response.data;
  }

  async post<T = unknown, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response: AxiosResponse<T> = await this.client.post(url, data as unknown, config);
    return response.data;
  }

  async put<T = unknown, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response: AxiosResponse<T> = await this.client.put(url, data as unknown, config);
    return response.data;
  }

  async delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response: AxiosResponse<T> = await this.client.delete(url, config);
    return response.data;
  }
}

export const apiClient = new ApiClient();
export default apiClient;
