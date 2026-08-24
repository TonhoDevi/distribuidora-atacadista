// Espelha o StandardError devolvido pelos serviços de backend
// (ver "Tratamento de erros" nos READMEs de customer-service/order-service/product-service).
export interface StandardError {
  timestamp: string;
  status: number;
  message: string;
  path: string;
}
