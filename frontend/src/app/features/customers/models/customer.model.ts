export interface CustomerRequest {
  name: string;
  email: string;
  document: string;
}

export interface Customer extends CustomerRequest {
  id: number;
  createdAt: string;
}
