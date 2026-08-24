import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User, UserRequest } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  // Aponta pro Gateway (lb://auth-service via /users/**). POST/DELETE exigem role ADMIN
  // no JwtGlobalFilter — a rota já é protegida no frontend por roleGuard(['ADMIN']).
  private readonly apiUrl = 'http://localhost:8080/users';

  constructor(private http: HttpClient) {}

  // O auth-service não expõe GET /users/{id} — apenas findAll (ver UserController).
  getAll(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  create(dto: UserRequest): Observable<User> {
    return this.http.post<User>(this.apiUrl, dto);
  }

  update(id: number, dto: UserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
