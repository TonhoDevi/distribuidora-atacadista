import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CustomerService } from '../../services/customer.service';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './customer-form.component.html',
  styleUrl: './customer-form.component.scss',
})
export class CustomerFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  loading = signal(false);
  saving = signal(false);
  customerId: number | null = null;

  form = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    document: ['', Validators.required],
  });

  get isEditMode(): boolean {
    return this.customerId !== null;
  }

  constructor(
    private customerService: CustomerService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    this.customerId = Number(idParam);
    this.loading.set(true);
    this.customerService.getById(this.customerId).subscribe({
      next: (customer) => {
        this.form.patchValue(customer);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível carregar o cliente.');
        this.router.navigate(['/customers']);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const dto = this.form.getRawValue() as { name: string; email: string; document: string };
    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.customerService.update(this.customerId!, dto)
      : this.customerService.create(dto);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.notification.success(
          this.isEditMode ? 'Cliente atualizado com sucesso.' : 'Cliente criado com sucesso.'
        );
        this.router.navigate(['/customers']);
      },
      error: (err) => {
        this.saving.set(false);
        this.notification.error(err, 'Não foi possível salvar o cliente.');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/customers']);
  }
}
