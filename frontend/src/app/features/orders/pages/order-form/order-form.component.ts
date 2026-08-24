import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { CustomerService } from '../../../customers/services/customer.service';
import { ProductService } from '../../../products/services/product.service';
import { Customer } from '../../../customers/models/customer.model';
import { Product } from '../../../products/models/product.model';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './order-form.component.html',
  styleUrl: './order-form.component.scss',
})
export class OrderFormComponent implements OnInit {
  private fb = inject(FormBuilder);

  loading = signal(true);
  saving = signal(false);
  customers = signal<Customer[]>([]);
  products = signal<Product[]>([]);

  form = this.fb.group({
    customerId: this.fb.control<number | null>(null, Validators.required),
    items: this.fb.array([this.createItemGroup()]),
  });

  get items(): FormArray<OrderItemFormGroup> {
    return this.form.get('items') as FormArray<OrderItemFormGroup>;
  }

  constructor(
    private orderService: OrderService,
    private customerService: CustomerService,
    private productService: ProductService,
    private notification: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    forkJoin({
      customers: this.customerService.getAll(),
      products: this.productService.getAll(),
    }).subscribe({
      next: ({ customers, products }) => {
        this.customers.set(customers);
        this.products.set(products);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível carregar clientes/produtos.');
      },
    });
  }

  // Construído com `new FormGroup` (em vez de `this.fb.group`) porque o overload
  // do FormBuilder acaba inferindo `quantity` como nullable ao misturar controles
  // no mesmo objeto literal — isso preserva os tipos exatos declarados acima.
  private createItemGroup(): OrderItemFormGroup {
    return new FormGroup({
      productId: new FormControl<number | null>(null, { validators: Validators.required }),
      quantity: new FormControl<number>(1, {
        nonNullable: true,
        validators: [Validators.required, Validators.min(1)],
      }),
    });
  }

  addItem(): void {
    this.items.push(this.createItemGroup());
  }

  removeItem(index: number): void {
    if (this.items.length === 1) return;
    this.items.removeAt(index);
  }

  productOf(productId: number | null): Product | undefined {
    return this.products().find((p) => p.id === productId);
  }

  // Preview no cliente — o total real é sempre recalculado pelo order-service
  // a partir do preço vigente no product-service (ver decisões de design do order-service).
  subtotalOf(index: number): number {
    const item = this.items.at(index).getRawValue();
    const product = this.productOf(item.productId);
    if (!product || !item.quantity) return 0;
    return product.price * item.quantity;
  }

  get estimatedTotal(): number {
    return this.items.controls.reduce((sum, _, index) => sum + this.subtotalOf(index), 0);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const dto = {
      customerId: raw.customerId!,
      items: raw.items.map((item) => ({
        productId: item.productId!,
        quantity: item.quantity!,
      })),
    };

    this.saving.set(true);
    this.orderService.create(dto).subscribe({
      next: (order) => {
        this.saving.set(false);
        this.notification.success('Pedido criado com sucesso.');
        this.router.navigate(['/orders', order.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.notification.error(err, 'Não foi possível criar o pedido.');
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/orders']);
  }
}

type OrderItemFormGroup = FormGroup<{
  productId: FormControl<number | null>;
  quantity: FormControl<number>;
}>;
