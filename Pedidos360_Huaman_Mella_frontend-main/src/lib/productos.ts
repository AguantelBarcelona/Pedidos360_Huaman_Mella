import { apiFetch } from './api';
import type { Producto } from '../types/Producto';

export async function obtenerProductos(): Promise<Producto[]> {
  const respuesta = await apiFetch('/api/productos');

  if (!respuesta.ok) {
    throw new Error(
      `No fue posible obtener los productos (${respuesta.status})`,
    );
  }

  return respuesta.json() as Promise<Producto[]>;
}