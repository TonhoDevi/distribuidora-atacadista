export type Role = 'ADMIN' | 'ANALISTA' | 'GERENTE' | 'CLIENTE';

export interface LoginRequest {
  username: string;
  password: string;
}

// Resposta do login e de GET /auth/me. O token não vem aqui: ele só trafega no cookie HttpOnly.
export interface SessionUser {
  username: string;
  role: Role;
}
