import { apiFetch } from './api';
import type { Inventario } from '../types/Inventario';

export async function obtenerInventario(): Promise<Inventario[]> {
  const respuesta = await apiFetch('/api/inventario');

  if (!respuesta.ok) {
    throw new Error(
      `No fue posible obtener el inventario (${respuesta.status})`,
    );
  }

  return respuesta.json() as Promise<Inventario[]>;
}