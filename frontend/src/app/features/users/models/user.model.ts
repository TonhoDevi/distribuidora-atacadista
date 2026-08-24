import { Role } from '../../auth/models/auth.model';

export interface UserRequest {
  username: string;
  password: string;
  role: Role;
}

export interface User {
  id: number;
  username: string;
  role: Role;
  createdAt: string;
}

export const ROLES: Role[] = ['ADMIN', 'ANALISTA', 'GERENTE', 'CLIENTE'];
