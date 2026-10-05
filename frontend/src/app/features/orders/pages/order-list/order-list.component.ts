import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { CustomerService } from '../../../customers/services/customer.service';
import { Customer } from '../../../customers/models/customer.model';
import { Order, STATUS_LABELS } from '../../models/order.model';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
})
export class OrderListComponent implements OnInit {
  statusLabels = STATUS_LABELS;
  displayedColumns = ['id', 'customer', 'total', 'status', 'createdAt', 'actions'];
  orders = signal<Order[]>([]);
  customers = signal<Customer[]>([]);
  customerNames = computed(() => new Map(this.customers().map((c) => [c.id, c.name])));
  loading = signal(false);

  constructor(
    private orderService: OrderService,
    private customerService: CustomerService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    forkJoin({ orders: this.orderService.getAll(), customers: this.customerService.getAll() }).subscribe({
      next: ({ orders, customers }) => {
        this.orders.set([...orders].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        this.customers.set(customers);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível carregar os pedidos.');
      },
    });
  }

  customerName(id: number): string {
    return this.customerNames().get(id) ?? `Cliente ${id}`;
  }
}
