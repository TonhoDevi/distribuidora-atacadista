import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductService } from '../../services/product.service';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss',
})
export class ProductFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  loading = signal(false);
  saving = signal(false);
  productId: number | null = null;

  form = this.fb.group({
    name: ['', Validators.required],
    sku: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    stockQuantity: [0, [Validators.required, Validators.min(0)]],
  });

  get isEditMode(): boolean {
    return this.productId !== null;
  }

  constructor(
    private productService: ProductService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return;

    this.productId = Number(idParam);
    this.loading.set(true);
    this.productService.getById(this.productId).subscribe({
      next: (product) => {
        this.form.patchValue(product);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível carregar o produto.');
        this.router.navigate(['/products']);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const dto = {
      name: raw.name!,
      sku: raw.sku!,
      description: raw.description ?? null,
      price: Number(raw.price),
      stockQuantity: Number(raw.stockQuantity),
    };
    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.productService.update(this.productId!, dto)
      : this.productService.create(dto);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.notification.success(
          this.isEditMode ? 'Produto atualizado com sucesso.' : 'Produto criado com sucesso.'
        );
        this.router.navigate(['/products']);
      },
      error: (err) => {
        this.saving.set(false);
        this.notification.error(err, 'Não foi possível salvar o produto.');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/products']);
  }
}
