import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { OrderService } from '../../../orders/services/order.service';
import { CustomerService } from '../../../customers/services/customer.service';
import { ProductService } from '../../../products/services/product.service';
import { Order, STATUS_LABELS } from '../../../orders/models/order.model';
import { Customer } from '../../../customers/models/customer.model';
import { Product } from '../../../products/models/product.model';

// Abaixo disso o produto aparece na lista de reposição do painel.
export const LOW_STOCK_THRESHOLD = 100;

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  // `authService` via inject() para existir antes dos demais field initializers.
  authService = inject(AuthService);
  private orderService = inject(OrderService);
  private customerService = inject(CustomerService);
  private productService = inject(ProductService);

  readonly lowStockThreshold = LOW_STOCK_THRESHOLD;
  readonly statusLabels = STATUS_LABELS;

  loading = signal(true);
  failed = signal(false);
  orders = signal<Order[]>([]);
  customers = signal<Customer[]>([]);
  products = signal<Product[]>([]);

  // Pedidos que ainda pedem uma ação da equipe (criados ou confirmados), mais antigos primeiro.
  queue = computed(() =>
    this.orders()
      .filter((o) => o.status === 'CREATED' || o.status === 'CONFIRMED')
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt))
  );
  openValue = computed(() => this.queue().reduce((sum, o) => sum + o.total, 0));
  lowStock = computed(() =>
    this.products()
      .filter((p) => p.stockQuantity < LOW_STOCK_THRESHOLD)
      .sort((a, b) => a.stockQuantity - b.stockQuantity)
  );
  customerById = computed(() => new Map(this.customers().map((c) => [c.id, c.name])));

  today = new Date();

  ngOnInit(): void {
    forkJoin({
      orders: this.orderService.getAll(),
      customers: this.customerService.getAll(),
      products: this.productService.getAll(),
    }).subscribe({
      next: ({ orders, customers, products }) => {
        this.orders.set(orders);
        this.customers.set(customers);
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  customerName(id: number): string {
    return this.customerById().get(id) ?? `Cliente ${id}`;
  }
}
