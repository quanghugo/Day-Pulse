import axios from 'axios';
import { useAuthStore } from '@/store';

const api = axios.create({
  baseURL: 'https://api.daypulse.app', // Placeholder
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const tokens = useAuthStore.getState().tokens;
    if (tokens?.accessToken) {
      config.headers.Authorization = `Bearer ${tokens.accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = useAuthStore.getState().tokens?.refreshToken;
        // Mock refresh call
        // const { data } = await axios.post('/auth/refresh', { refreshToken });
        // useAuthStore.getState().setAuth(useAuthStore.getState().user!, data.tokens);
        // return api(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().logout();
      }
    }
    return Promise.reject(error);
  }
);

export default api;
