import { HttpInterceptorFn } from '@angular/common/http';

const API_URL = 'http://localhost:48080';

// O JWT vive num cookie HttpOnly: o JavaScript nunca o vê nem o coloca na requisição.
// Quem anexa o cookie é o navegador, mas em chamadas cross-origin (44200 -> 48080) ele só
// faz isso se a requisição for marcada com withCredentials. Esse interceptor faz essa marcação.
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith(API_URL)) {
    return next(req.clone({ withCredentials: true }));
  }
  return next(req);
};
