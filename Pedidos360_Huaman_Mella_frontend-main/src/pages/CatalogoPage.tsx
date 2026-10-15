import { useEffect, useState } from 'react';
import { useAuthenticator } from '@aws-amplify/ui-react';
import { useNavigate } from 'react-router-dom';
import './CatalogoPage.css';

import { obtenerProductos } from '../lib/productos';
import { obtenerInventario } from '../lib/inventario';
import { crearPedido } from '../lib/pedidos';
import {
  getAuthInfo,
  type AuthInfo,
} from '../lib/auth';

import type { Producto } from '../types/Producto';

type ProductoCatalogo = Producto & {
  stockDisponible: number;
};

export default function CatalogoPage() {
  const { user } = useAuthenticator((context) => [
    context.user,
  ]);

  const navigate = useNavigate();

  const [productos, setProductos] = useState<
    ProductoCatalogo[]
  >([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const [authInfo, setAuthInfo] =
    useState<AuthInfo | null>(null);

  const [cantidades, setCantidades] = useState<
    Record<number, number>
  >({});

  const [productoProcesando, setProductoProcesando] =
    useState<number | null>(null);

  const [mensaje, setMensaje] = useState('');
  const [errorPedido, setErrorPedido] = useState('');

  const email = user?.signInDetails?.loginId;

  useEffect(() => {
    let activo = true;

    Promise.all([obtenerProductos(), obtenerInventario()])
      .then(([datos, inventario]) => {
        if (activo) {
          const stockPorProducto = new Map(
            inventario.map((registro) => [
              registro.productoId,
              registro.stockDisponible,
            ]),
          );

          const productosConStock = datos.map((producto) => ({
            ...producto,
            stockDisponible:
              stockPorProducto.get(producto.id) ?? 0,
          }));

          setProductos(productosConStock);

          const cantidadesIniciales =
            datos.reduce<Record<number, number>>(
              (acumulador, producto) => {
                acumulador[producto.id] = 1;
                return acumulador;
              },
              {},
            );

          setCantidades(cantidadesIniciales);
        }
      })
      .catch((err: Error) => {
        if (activo) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (activo) {
          setCargando(false);
        }
      });

    getAuthInfo()
      .then((info) => {
        if (activo) {
          setAuthInfo(info);
        }
      })
      .catch(() => {
        if (activo) {
          setAuthInfo(null);
        }
      });

    return () => {
      activo = false;
    };
  }, []);

  function actualizarCantidad(
    productoId: number,
    cantidad: number,
  ) {
    const cantidadValida =
      Number.isFinite(cantidad) && cantidad >= 1
        ? Math.floor(cantidad)
        : 1;

    setCantidades((actuales) => ({
      ...actuales,
      [productoId]: cantidadValida,
    }));
  }

  async function realizarPedido(producto: Producto) {
    if (!email) {
      setErrorPedido(
        'No fue posible obtener el correo del usuario autenticado.',
      );
      return;
    }

    const cantidad = cantidades[producto.id] ?? 1;

    setProductoProcesando(producto.id);
    setMensaje('');
    setErrorPedido('');

    try {
      const pedido = await crearPedido({
        clienteEmail: email,
        productoId: producto.id,
        cantidad,
      });

      setMensaje(
        `Pedido #${pedido.id} creado correctamente. Total: ${pedido.total.toLocaleString(
          'es-CL',
          {
            style: 'currency',
            currency: 'CLP',
            maximumFractionDigits: 0,
          },
        )}.`,
      );
    } catch (err) {
      setErrorPedido(
        err instanceof Error
          ? err.message
          : 'Ocurrió un error al crear el pedido.',
      );
    } finally {
      setProductoProcesando(null);
    }
  }

  const formatoPrecio = new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    maximumFractionDigits: 0,
  });

  if (cargando) {
    return (
      <main className="catalogo-page catalogo-page__loading">
        <p>Cargando catálogo...</p>
      </main>
    );
  }

  if (error) {
    return (
      <main className="catalogo-page">
        <h1>Catálogo</h1>

        <div className="catalogo-page__error">
          {error}
        </div>
      </main>
    );
  }

  return (
    <main className="catalogo-page">
      <header className="catalogo-page__header">
        <h1>Catálogo de productos</h1>

        <p className="catalogo-page__intro">
          Explora los productos disponibles en Pedidos360.
        </p>
      </header>

      {mensaje && (
        <div className="catalogo-page__success">
          <strong>Pedido realizado correctamente.</strong>

          <p>{mensaje}</p>

          <button onClick={() => navigate('/cliente')}>
            Ver mis pedidos
          </button>
        </div>
      )}

      {errorPedido && (
        <div className="catalogo-page__error">
          <strong>
            No se pudo realizar el pedido.
          </strong>

          <p>{errorPedido}</p>
        </div>
      )}

      {productos.length === 0 ? (
        <div className="catalogo-page__empty">
          No hay productos disponibles.
        </div>
      ) : (
        <div className="catalogo-page__grid">
          {productos.map((producto) => {
            const procesando =
              productoProcesando === producto.id;

            return (
              <article key={producto.id} className="catalogo-product-card">
                <div className="catalogo-product-card__content">
                  <div className="catalogo-product-card__category">
                    {producto.categoria}
                  </div>

                  <h2>{producto.modelo}</h2>

                  <p>
                    <strong>Marca:</strong>{' '}
                    {producto.marca}
                  </p>

                  <p>
                    <strong>Talla:</strong>{' '}
                    {producto.talla}
                  </p>

                  <p>
                    <strong>Stock disponible:</strong>{' '}
                    {producto.stockDisponible}
                  </p>

                  {producto.descripcion && (
                    <p className="catalogo-product-card__description">
                      {producto.descripcion}
                    </p>
                  )}

                  <p className="catalogo-product-card__price">
                    {formatoPrecio.format(
                      producto.precio,
                    )}
                  </p>
                </div>

                {authInfo?.isCliente && (
                  <div className="catalogo-product-card__order">
                    <label
                      htmlFor={`cantidad-${producto.id}`}
                    >
                      Cantidad
                    </label>

                    <input
                      id={`cantidad-${producto.id}`}
                      type="number"
                      min={1}
                      step={1}
                      value={
                        cantidades[producto.id] ?? 1
                      }
                      disabled={procesando}
                      onChange={(event) =>
                        actualizarCantidad(
                          producto.id,
                          Number(event.target.value),
                        )
                      }
                      className="catalogo-product-card__quantity"
                    />

                    <button
                      onClick={() =>
                        realizarPedido(producto)
                      }
                      disabled={procesando}
                      className="catalogo-product-card__order-button"
                    >
                      {procesando
                        ? 'Procesando...'
                        : 'Realizar pedido'}
                    </button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </main>
  );
}