import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, Role } from '../models/auth.model';

const TOKEN_KEY = 'auth_token';
const USERNAME_KEY = 'auth_username';
const ROLE_KEY = 'auth_role';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Aponta pro Gateway, não direto pro auth-service — tudo passa por 8080.
  private readonly apiUrl = 'http://localhost:8080/auth';

  isAuthenticated = signal<boolean>(this.hasToken());

  constructor(private http: HttpClient) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(USERNAME_KEY, response.username);
        localStorage.setItem(ROLE_KEY, response.role);
        this.isAuthenticated.set(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.isAuthenticated.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUsername(): string | null {
    return localStorage.getItem(USERNAME_KEY);
  }

  getRole(): Role | null {
    return localStorage.getItem(ROLE_KEY) as Role | null;
  }

  hasRole(...roles: Role[]): boolean {
    const current = this.getRole();
    return current !== null && roles.includes(current);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  }
}
