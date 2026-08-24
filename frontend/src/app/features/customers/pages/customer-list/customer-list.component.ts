import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { CustomerService } from '../../services/customer.service';
import { Customer } from '../../models/customer.model';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './customer-list.component.html',
  styleUrl: './customer-list.component.scss',
})
export class CustomerListComponent implements OnInit {
  displayedColumns = ['name', 'email', 'document', 'createdAt', 'actions'];
  customers = signal<Customer[]>([]);
  loading = signal(false);
  searchTerm = '';

  constructor(
    private customerService: CustomerService,
    private notification: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.customerService.getAll().subscribe({
      next: (customers) => {
        this.customers.set(customers);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível carregar os clientes.');
      },
    });
  }

  search(): void {
    const term = this.searchTerm.trim();
    if (!term) {
      this.load();
      return;
    }
    this.loading.set(true);
    this.customerService.search(term).subscribe({
      next: (customers) => {
        this.customers.set(customers);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notification.error(err, 'Não foi possível buscar clientes.');
      },
    });
  }

  remove(customer: Customer): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Excluir cliente',
        message: `Tem certeza que deseja excluir "${customer.name}"?`,
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      this.customerService.delete(customer.id).subscribe({
        next: () => {
          this.notification.success('Cliente excluído com sucesso.');
          this.load();
        },
        error: (err) => this.notification.error(err, 'Não foi possível excluir o cliente.'),
      });
    });
  }
}
