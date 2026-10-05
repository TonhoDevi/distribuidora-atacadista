import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { routes } from './app.routes';
import { credentialsInterceptor } from './core/interceptors/credentials.interceptor';
import { AuthService } from './features/auth/services/auth.service';
import { firstValueFrom } from 'rxjs';
import { errorInterceptor } from './core/interceptors/error.interceptor';

registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([credentialsInterceptor, errorInterceptor])),
    // Antes de qualquer rota/guard rodar, descobre se já existe sessão (cookie válido).
    provideAppInitializer(() => firstValueFrom(inject(AuthService).loadSession())),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
  ]
};
