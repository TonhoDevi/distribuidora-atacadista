export interface ProductRequest {
  name: string;
  sku: string;
  description: string | null;
  price: number;
  stockQuantity: number;
}

export interface Product extends ProductRequest {
  id: number;
  createdAt: string;
}
