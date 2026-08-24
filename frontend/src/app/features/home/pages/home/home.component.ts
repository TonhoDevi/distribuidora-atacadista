import { Component } from '@angular/core';
import { AuthService } from '../../../auth/services/auth.service';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatButtonModule, MatToolbarModule],
  template: `
    <mat-toolbar color="primary">
      <span>Distribuidora Atacadista — AtlasTT</span>
      <span class="spacer"></span>
      <span>{{ authService.getUsername() }} ({{ authService.getRole() }})</span>
      <button mat-button (click)="logout()">Sair</button>
    </mat-toolbar>
    <div style="padding: 24px;">
      <p>
        Logado com sucesso. Próximos módulos a construir aqui:
        Clientes, Produtos, Pedidos.
      </p>
    </div>
  `,
  styles: [
    `
      .spacer {
        flex: 1 1 auto;
      }
    `,
  ],
})
export class HomeComponent {
  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
