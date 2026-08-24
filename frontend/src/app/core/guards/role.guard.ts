import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';
import { Role } from '../../features/auth/models/auth.model';

// Factory: gera um guard configurado para as roles permitidas naquela rota.
// Uso nas rotas: canActivate: [roleGuard(['ADMIN'])]
//
// Isso é só camada de UX (esconder telas que o usuário não deveria acessar).
// A autorização de verdade continua sendo aplicada no Gateway
// (DELETE e POST /users restritos a ADMIN, ver JwtGlobalFilter) —
// esse guard nunca deve ser tratado como a fonte de verdade de segurança.
export function roleGuard(allowedRoles: Role[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.hasRole(...allowedRoles)) {
      return true;
    }

    router.navigate(['/']);
    return false;
  };
}
