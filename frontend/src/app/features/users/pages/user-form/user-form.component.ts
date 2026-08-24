import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../services/user.service';
import { ROLES } from '../../models/user.model';
import { Role } from '../../../auth/models/auth.model';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './user-form.component.html',
  styleUrl: './user-form.component.scss',
})
export class UserFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  loading = signal(false);
  saving = signal(false);
  userId: number | null = null;
  roles = ROLES;

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
    role: this.fb.control<Role | null>(null, Validators.required),
  });

  get isEditMode(): boolean {
    return this.userId !== null;
  }

  constructor(
    private userService: UserService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    this.userId = Number(idParam);
    this.loading.set(true);
    // O auth-service não expõe GET /users/{id}: buscamos a lista e filtramos
    // (ver decisão de design — findAll é o único endpoint de leitura disponível).
    this.userService.getAll().subscribe({
      next: (users) => {
        const user = users.find((u) => u.id === this.userId);
        if (!user) {
          this.notification.warn('Usuário não encontrado.');
          this.router.navigate(['/users']);
          return;
        }
        this.form.patchValue({ username: user.username, role: user.role });
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível carregar o usuário.');
        this.router.navigate(['/users']);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const dto = { username: raw.username!, password: raw.password!, role: raw.role! };
    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.userService.update(this.userId!, dto)
      : this.userService.create(dto);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.notification.success(
          this.isEditMode ? 'Usuário atualizado com sucesso.' : 'Usuário criado com sucesso.'
        );
        this.router.navigate(['/users']);
      },
      error: (err) => {
        this.saving.set(false);
        this.notification.error(err, 'Não foi possível salvar o usuário.');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/users']);
  }
}
