import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer, CustomerRequest } from '../models/customer.model';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  // Aponta pro Gateway (lb://customer-service via /customers/**), não direto pro serviço.
  private readonly apiUrl = 'http://localhost:8080/customers';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.apiUrl);
  }

  getById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/${id}`);
  }

  search(name: string): Observable<Customer[]> {
    const params = new HttpParams().set('name', name);
    return this.http.get<Customer[]>(`${this.apiUrl}/search`, { params });
  }

  create(dto: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, dto);
  }

  update(id: number, dto: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
