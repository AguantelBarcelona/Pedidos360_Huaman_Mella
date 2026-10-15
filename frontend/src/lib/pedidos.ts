import { apiFetch } from './api';
import type { Pedido } from '../types/Pedido';

export type CrearPedidoRequest = {
  clienteEmail: string;
  productoId: number;
  cantidad: number;
};

export async function obtenerMisPedidos(): Promise<Pedido[]> {
  const respuesta = await apiFetch('/api/pedidos/mios');

  if (!respuesta.ok) {
    throw new Error(
      `No fue posible obtener los pedidos. Código ${respuesta.status}`,
    );
  }

  return respuesta.json() as Promise<Pedido[]>;
}

export async function obtenerTodosLosPedidos(): Promise<Pedido[]> {
  const respuesta = await apiFetch('/api/pedidos');

  if (!respuesta.ok) {
    throw new Error(
      `No fue posible obtener todos los pedidos (${respuesta.status})`,
    );
  }

  return respuesta.json() as Promise<Pedido[]>;
}

export async function crearPedido(
  datos: CrearPedidoRequest,
): Promise<Pedido> {
  const respuesta = await apiFetch('/api/pedidos', {
    method: 'POST',
    body: JSON.stringify(datos),
  });

  if (!respuesta.ok) {
    const detalle = await respuesta.text();

    throw new Error(
      detalle
        ? `No fue posible crear el pedido (${respuesta.status}): ${detalle}`
        : `No fue posible crear el pedido (${respuesta.status})`,
    );
  }

  return respuesta.json() as Promise<Pedido>;
}