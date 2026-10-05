import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';

// Trata 401 globalmente: o Gateway rejeita token ausente/expirado/inválido
// com 401 (ver JwtGlobalFilter). Em vez de cada tela tratar isso na mão,
// o interceptor esquece a sessão local e manda pro login.
// /auth/login (credenciais erradas) e /auth/me (checagem inicial) são exceções:
// o 401 deles é esperado e tratado por quem chamou.
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/me')) {
        authService.clearSession();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
