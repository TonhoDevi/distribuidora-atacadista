import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../features/auth/services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // A rota de login não deve levar o header Authorization
  // (ainda não existe token nesse momento).
  if (!token || req.url.includes('/auth/login')) {
    return next(req);
  }

  const clonedRequest = req.clone({
    setHeaders: {
      Authorization: \`Bearer \${token}\`,
    },
  });

  return next(clonedRequest);
};
