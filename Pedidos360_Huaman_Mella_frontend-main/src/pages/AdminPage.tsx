import { useEffect, useMemo, useState } from 'react';

import { obtenerProductos } from '../lib/productos';
import { obtenerInventario } from '../lib/inventario';
import { obtenerTodosLosPedidos } from '../lib/pedidos';

import type { Producto } from '../types/Producto';
import type { Inventario } from '../types/Inventario';
import type { Pedido } from '../types/Pedido';

export default function AdminPage() {
  const [productos, setProductos] = useState<Producto[]>([]);
  const [inventario, setInventario] = useState<Inventario[]>([]);
  const [pedidos, setPedidos] = useState<Pedido[]>([]);

  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let activo = true;

    async function cargarDatos() {
      try {
        setCargando(true);
        setError('');

        const [
          productosObtenidos,
          inventarioObtenido,
          pedidosObtenidos,
        ] = await Promise.all([
          obtenerProductos(),
          obtenerInventario(),
          obtenerTodosLosPedidos(),
        ]);

        if (!activo) {
          return;
        }

        setProductos(productosObtenidos);
        setInventario(inventarioObtenido);
        setPedidos(pedidosObtenidos);
      } catch (err) {
        if (activo) {
          setError(
            err instanceof Error
              ? err.message
              : 'Ocurrió un error al cargar el panel administrativo.',
          );
        }
      } finally {
        if (activo) {
          setCargando(false);
        }
      }
    }

    cargarDatos();

    return () => {
      activo = false;
    };
  }, []);

  const formatoPrecio = new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
  });

  const formatoFecha = new Intl.DateTimeFormat('es-CL', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });

  const productosPorId = useMemo(
    () =>
      new Map(
        productos.map((producto) => [
          producto.id,
          producto,
        ]),
      ),
    [productos],
  );

  const pedidosOrdenados = useMemo(
    () =>
      [...pedidos].sort(
        (pedidoA, pedidoB) =>
          pedidoB.id - pedidoA.id,
      ),
    [pedidos],
  );

  const stockDisponibleTotal = inventario.reduce(
    (total, item) => total + item.stockDisponible,
    0,
  );

  const stockReservadoTotal = inventario.reduce(
    (total, item) => total + item.stockReservado,
    0,
  );

  if (cargando) {
    return (
      <main
        style={{
          maxWidth: 1200,
          margin: '0 auto',
          padding: '32px 24px',
          fontFamily: 'system-ui',
        }}
      >
        <p>Cargando panel administrativo...</p>
      </main>
    );
  }

  if (error) {
    return (
      <main
        style={{
          maxWidth: 1200,
          margin: '0 auto',
          padding: '32px 24px',
          fontFamily: 'system-ui',
        }}
      >
        <h1>Panel de Administración</h1>

        <div
          style={{
            marginTop: 24,
            padding: 20,
            border: '1px solid #fecaca',
            borderRadius: 10,
            background: '#fef2f2',
            color: '#991b1b',
          }}
        >
          <strong>
            No fue posible cargar la información.
          </strong>

          <p style={{ marginBottom: 0 }}>
            {error}
          </p>
        </div>
      </main>
    );
  }

  return (
    <main
      style={{
        maxWidth: 1200,
        margin: '0 auto',
        padding: '32px 24px',
        fontFamily: 'system-ui',
      }}
    >
      <header style={{ marginBottom: 32 }}>
        <h1 style={{ marginBottom: 8 }}>
          Panel de Administración
        </h1>

        <p
          style={{
            margin: 0,
            color: '#4b5563',
          }}
        >
          Consulta productos, inventario y pedidos
          registrados en Pedidos360.
        </p>
      </header>

      <section
        style={{
          display: 'grid',
          gridTemplateColumns:
            'repeat(auto-fit, minmax(200px, 1fr))',
          gap: 16,
          marginBottom: 40,
        }}
      >
        <article
          style={{
            padding: 22,
            border: '1px solid #e5e7eb',
            borderRadius: 12,
            background: '#ffffff',
            boxShadow:
              '0 1px 3px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              color: '#6b7280',
              marginBottom: 8,
            }}
          >
            Productos registrados
          </div>

          <strong style={{ fontSize: 30 }}>
            {productos.length}
          </strong>
        </article>

        <article
          style={{
            padding: 22,
            border: '1px solid #e5e7eb',
            borderRadius: 12,
            background: '#ffffff',
            boxShadow:
              '0 1px 3px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              color: '#6b7280',
              marginBottom: 8,
            }}
          >
            Stock disponible
          </div>

          <strong style={{ fontSize: 30 }}>
            {stockDisponibleTotal}
          </strong>
        </article>

        <article
          style={{
            padding: 22,
            border: '1px solid #e5e7eb',
            borderRadius: 12,
            background: '#ffffff',
            boxShadow:
              '0 1px 3px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              color: '#6b7280',
              marginBottom: 8,
            }}
          >
            Stock reservado
          </div>

          <strong style={{ fontSize: 30 }}>
            {stockReservadoTotal}
          </strong>
        </article>

        <article
          style={{
            padding: 22,
            border: '1px solid #e5e7eb',
            borderRadius: 12,
            background: '#ffffff',
            boxShadow:
              '0 1px 3px rgba(0, 0, 0, 0.08)',
          }}
        >
          <div
            style={{
              color: '#6b7280',
              marginBottom: 8,
            }}
          >
            Pedidos registrados
          </div>

          <strong style={{ fontSize: 30 }}>
            {pedidos.length}
          </strong>
        </article>
      </section>

      <section style={{ marginBottom: 40 }}>
        <h2>Pedidos</h2>

        {pedidosOrdenados.length === 0 ? (
          <p>No existen pedidos registrados.</p>
        ) : (
          <div
            style={{
              display: 'grid',
              gap: 14,
              marginTop: 18,
            }}
          >
            {pedidosOrdenados.map((pedido) => {
              const producto =
                productosPorId.get(
                  pedido.productoId,
                );

              return (
                <article
                  key={pedido.id}
                  style={{
                    padding: 22,
                    border: '1px solid #e5e7eb',
                    borderRadius: 12,
                    background: '#ffffff',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      justifyContent:
                        'space-between',
                      alignItems: 'flex-start',
                      gap: 16,
                      flexWrap: 'wrap',
                      marginBottom: 18,
                    }}
                  >
                    <div>
                      <h3
                        style={{
                          margin: '0 0 6px',
                        }}
                      >
                        Pedido #{pedido.id}
                      </h3>

                      <div
                        style={{
                          color: '#6b7280',
                          fontSize: 14,
                        }}
                      >
                        {formatoFecha.format(
                          new Date(
                            pedido.fechaCreacion,
                          ),
                        )}
                      </div>
                    </div>

                    <span
                      style={{
                        padding: '6px 12px',
                        borderRadius: 999,
                        background: '#ecfdf5',
                        color: '#047857',
                        fontWeight: 700,
                        fontSize: 13,
                      }}
                    >
                      {pedido.estado}
                    </span>
                  </div>

                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns:
                        'repeat(auto-fit, minmax(170px, 1fr))',
                      gap: 18,
                    }}
                  >
                    <div>
                      <div
                        style={{
                          color: '#6b7280',
                          fontSize: 13,
                          marginBottom: 4,
                        }}
                      >
                        Cliente
                      </div>

                      <strong>
                        {pedido.clienteEmail}
                      </strong>
                    </div>

                    <div>
                      <div
                        style={{
                          color: '#6b7280',
                          fontSize: 13,
                          marginBottom: 4,
                        }}
                      >
                        Producto
                      </div>

                      <strong>
                        {producto
                          ? `${producto.marca} ${producto.modelo}`
                          : `Producto #${pedido.productoId}`}
                      </strong>
                    </div>

                    <div>
                      <div
                        style={{
                          color: '#6b7280',
                          fontSize: 13,
                          marginBottom: 4,
                        }}
                      >
                        Cantidad
                      </div>

                      <strong>
                        {pedido.cantidad}
                      </strong>
                    </div>

                    <div>
                      <div
                        style={{
                          color: '#6b7280',
                          fontSize: 13,
                          marginBottom: 4,
                        }}
                      >
                        Total
                      </div>

                      <strong>
                        {formatoPrecio.format(
                          pedido.total,
                        )}
                      </strong>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section style={{ marginBottom: 40 }}>
        <h2>Inventario</h2>

        {inventario.length === 0 ? (
          <p>
            No existen registros de inventario.
          </p>
        ) : (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns:
                'repeat(auto-fit, minmax(260px, 1fr))',
              gap: 16,
              marginTop: 18,
            }}
          >
            {inventario.map((item) => {
              const producto =
                productosPorId.get(
                  item.productoId,
                );

              return (
                <article
                  key={item.id}
                  style={{
                    padding: 22,
                    border: '1px solid #e5e7eb',
                    borderRadius: 12,
                    background: '#ffffff',
                  }}
                >
                  <h3
                    style={{
                      marginTop: 0,
                    }}
                  >
                    {producto
                      ? `${producto.marca} ${producto.modelo}`
                      : `Producto #${item.productoId}`}
                  </h3>

                  <p>
                    <strong>
                      Stock disponible:
                    </strong>{' '}
                    {item.stockDisponible}
                  </p>

                  <p style={{ marginBottom: 0 }}>
                    <strong>
                      Stock reservado:
                    </strong>{' '}
                    {item.stockReservado}
                  </p>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section>
        <h2>Productos</h2>

        {productos.length === 0 ? (
          <p>No existen productos registrados.</p>
        ) : (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns:
                'repeat(auto-fit, minmax(260px, 1fr))',
              gap: 16,
              marginTop: 18,
            }}
          >
            {productos.map((producto) => (
              <article
                key={producto.id}
                style={{
                  padding: 22,
                  border: '1px solid #e5e7eb',
                  borderRadius: 12,
                  background: '#ffffff',
                }}
              >
                <div
                  style={{
                    color: '#6b7280',
                    fontSize: 13,
                  }}
                >
                  {producto.categoria}
                </div>

                <h3>
                  {producto.marca}{' '}
                  {producto.modelo}
                </h3>

                <p>
                  <strong>Talla:</strong>{' '}
                  {producto.talla}
                </p>

                <p
                  style={{
                    fontSize: 20,
                    fontWeight: 700,
                    marginBottom: 0,
                  }}
                >
                  {formatoPrecio.format(
                    producto.precio,
                  )}
                </p>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}