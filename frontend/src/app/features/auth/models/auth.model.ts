export type Role = 'ADMIN' | 'ANALISTA' | 'GERENTE' | 'CLIENTE';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: Role;
}
