import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, OrderRequest } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  // Aponta pro Gateway (lb://order-service via /orders/**), não direto pro serviço.
  // Backend só expõe POST/GET — não há PUT/DELETE de pedido (ver OrderController).
  private readonly apiUrl = 'http://localhost:8080/orders';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Order[]> {
    return this.http.get<Order[]>(this.apiUrl);
  }

  getById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  create(dto: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, dto);
  }
}
