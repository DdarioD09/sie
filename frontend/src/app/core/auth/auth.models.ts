/** Mirrors the backend's auth.dto.LoginRequest / LoginResponse records exactly - keep them in sync. */
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: 'ADMIN' | 'VIEWER';
}
