export interface Pedido {
  id: number;
  clienteEmail: string;
  productoId: number;
  cantidad: number;
  precioUnitario: number;
  total: number;
  estado: string;
  fechaCreacion: string;
  clienteSub: string | null;
}