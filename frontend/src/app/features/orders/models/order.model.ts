export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface OrderItemResponse {
  productId: number;
  quantity: number;
  unitPrice: number;
}

export interface OrderRequest {
  customerId: number;
  items: OrderItemRequest[];
}

export type OrderStatus = 'CREATED' | string;

export interface Order {
  id: number;
  customerId: number;
  total: number;
  status: OrderStatus;
  createdAt: string;
  items: OrderItemResponse[];
}
