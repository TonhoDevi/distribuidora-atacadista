import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { LoginRequest, Role, SessionUser } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Aponta pro Gateway, não direto pro auth-service — tudo passa por 48080.
  private readonly apiUrl = 'http://localhost:48080/auth';

  // O token JWT está num cookie HttpOnly, invisível para o JS. O que guardamos aqui é só
  // "quem está logado" (em memória), obtido do login ou de GET /auth/me.
  private readonly user = signal<SessionUser | null>(null);
  readonly isAuthenticated = computed(() => this.user() !== null);

  constructor(private http: HttpClient) {}

  login(credentials: LoginRequest): Observable<SessionUser> {
    return this.http
      .post<SessionUser>(`${this.apiUrl}/login`, credentials)
      .pipe(tap((user) => this.user.set(user)));
  }

  // Chamado na inicialização do app (ver app.config.ts): se o cookie ainda for válido,
  // o backend responde com o usuário e a sessão é restaurada após um F5.
  loadSession(): Observable<SessionUser | null> {
    return this.http.get<SessionUser>(`${this.apiUrl}/me`).pipe(
      tap((user) => this.user.set(user)),
      catchError(() => {
        this.user.set(null);
        return of(null);
      })
    );
  }

  // Pede ao backend para apagar o cookie (só ele consegue, por ser HttpOnly).
  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}).pipe(
      tap(() => this.clearSession()),
      catchError(() => {
        this.clearSession();
        return of(undefined);
      })
    );
  }

  // Só esquece o usuário localmente (usado quando o servidor já respondeu 401).
  clearSession(): void {
    this.user.set(null);
  }

  getUsername(): string | null {
    return this.user()?.username ?? null;
  }

  getRole(): Role | null {
    return this.user()?.role ?? null;
  }

  hasRole(...roles: Role[]): boolean {
    const current = this.getRole();
    return current !== null && roles.includes(current);
  }
}
