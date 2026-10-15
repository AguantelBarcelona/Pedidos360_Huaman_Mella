export type Producto = {
  id: number;
  modelo: string;
  marca: string;
  talla: number;
  precio: number;
  categoria: string;
  descripcion: string | null;
};