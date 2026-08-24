import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { CustomerService } from '../../../customers/services/customer.service';
import { ProductService } from '../../../products/services/product.service';
import { Order } from '../../models/order.model';
import { Customer } from '../../../customers/models/customer.model';
import { Product } from '../../../products/models/product.model';
import { NotificationService } from '../../../../shared/services/notification.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss',
})
export class OrderDetailComponent implements OnInit {
  loading = signal(true);
  order = signal<Order | null>(null);
  customer = signal<Customer | null>(null);
  products = signal<Product[]>([]);
  displayedColumns = ['product', 'quantity', 'unitPrice', 'subtotal'];

  constructor(
    private orderService: OrderService,
    private customerService: CustomerService,
    private productService: ProductService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.orderService.getById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        forkJoin({
          customer: this.customerService.getById(order.customerId),
          products: this.productService.getAll(),
        }).subscribe({
          next: ({ customer, products }) => {
            this.customer.set(customer);
            this.products.set(products);
            this.loading.set(false);
          },
          error: () => {
            // Cliente/produtos podem ter sido removidos depois do pedido — não é fatal,
            // a tela continua mostrando os dados do próprio pedido.
            this.loading.set(false);
          },
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Pedido não encontrado.');
        this.router.navigate(['/orders']);
      },
    });
  }

  productName(productId: number): string {
    return this.products().find((p) => p.id === productId)?.name ?? `Produto #${productId}`;
  }

  subtotal(quantity: number, unitPrice: number): number {
    return quantity * unitPrice;
  }
}
