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

export type OrderStatus = 'CREATED' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELED';

// Espelha OrderStatus.allowedNext() do order-service (o backend é quem valida de verdade).
export const NEXT_STATUSES: Record<OrderStatus, OrderStatus[]> = {
  CREATED: ['CONFIRMED', 'CANCELED'],
  CONFIRMED: ['SHIPPED', 'CANCELED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELED: [],
};

export const STATUS_LABELS: Record<string, string> = {
  CREATED: 'Criado',
  CONFIRMED: 'Confirmado',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregue',
  CANCELED: 'Cancelado',
};

export interface Order {
  id: number;
  customerId: number;
  total: number;
  status: OrderStatus;
  createdAt: string;
  items: OrderItemResponse[];
}
