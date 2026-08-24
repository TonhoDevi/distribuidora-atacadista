import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { StandardError } from '../../core/models/standard-error.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 3000, panelClass: 'snackbar-success' });
  }

  // Para mensagens de alerta que não vêm de uma resposta HTTP (ex: dado não encontrado
  // localmente após uma listagem, sem endpoint dedicado de busca por ID).
  warn(message: string): void {
    this.snackBar.open(message, 'Fechar', { duration: 4000, panelClass: 'snackbar-warn' });
  }

  // Extrai a mensagem do StandardError quando disponível; cai para um texto
  // genérico em falhas de rede/infra (ex: Gateway/serviço fora do ar).
  error(err: HttpErrorResponse, fallback = 'Ocorreu um erro. Tente novamente.'): void {
    const body = err.error as StandardError | undefined;
    const message = body?.message ?? fallback;
    this.snackBar.open(message, 'Fechar', { duration: 5000, panelClass: 'snackbar-error' });
  }
}
