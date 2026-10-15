import { useEffect, useState } from 'react';
import './ClientePage.css';

import { obtenerMisPedidos } from '../lib/pedidos';
import type { Pedido } from '../types/Pedido';

export default function ClientePage() {
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let activo = true;

    async function cargarPedidos() {
      try {
        setCargando(true);
        setError('');

        const datos = await obtenerMisPedidos();

        if (activo) {
          setPedidos(datos);
        }
      } catch (err) {
        if (activo) {
          setError(
            err instanceof Error
              ? err.message
              : 'Ocurrió un error al cargar los pedidos.',
          );
        }
      } finally {
        if (activo) {
          setCargando(false);
        }
      }
    }

    cargarPedidos();

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

  return (
    <main className="cliente-page">
      <header className="cliente-page__header">
        <h1>Mi cuenta</h1>

        <p className="cliente-page__intro">
          Consulta los pedidos asociados a tu cuenta.
        </p>
      </header>

      <section>
        <div className="cliente-page__section-header">
          <h2>Mis pedidos</h2>

          {!cargando && !error && (
            <span className="cliente-page__count">
              {pedidos.length}{' '}
              {pedidos.length === 1
                ? 'pedido'
                : 'pedidos'}
            </span>
          )}
        </div>

        {cargando && (
          <div className="cliente-page__message">
            Cargando pedidos...
          </div>
        )}

        {!cargando && error && (
          <div className="cliente-page__error">
            <strong>
              No se pudieron cargar los pedidos.
            </strong>

            <p>{error}</p>
          </div>
        )}

        {!cargando &&
          !error &&
          pedidos.length === 0 && (
            <div className="cliente-page__empty">
              <h3>Aún no tienes pedidos</h3>

              <p className="cliente-page__empty-description">
                Los pedidos que realices aparecerán
                en esta sección.
              </p>
            </div>
          )}

        {!cargando &&
          !error &&
          pedidos.length > 0 && (
            <div className="cliente-page__orders">
              {pedidos.map((pedido) => (
                <article
                  key={pedido.id}
                  className="cliente-page__order-card"
                >
                  <div className="cliente-page__order-header">
                    <div>
                      <h3>Pedido #{pedido.id}</h3>

                      <span className="cliente-page__date">
                        {formatoFecha.format(
                          new Date(
                            pedido.fechaCreacion,
                          ),
                        )}
                      </span>
                    </div>

                    <span className="cliente-page__status">
                      {pedido.estado}
                    </span>
                  </div>

                  <div className="cliente-page__details">
                    <div>
                      <div className="cliente-page__label">Producto</div>

                      <strong>
                        Producto #{pedido.productoId}
                      </strong>
                    </div>

                    <div>
                      <div className="cliente-page__label">Cantidad</div>

                      <strong>
                        {pedido.cantidad}
                      </strong>
                    </div>

                    <div>
                      <div className="cliente-page__label">
                        Precio unitario
                      </div>

                      <strong>
                        {formatoPrecio.format(
                          pedido.precioUnitario,
                        )}
                      </strong>
                    </div>

                    <div>
                      <div className="cliente-page__label">Total</div>

                      <strong className="cliente-page__total">
                        {formatoPrecio.format(
                          pedido.total,
                        )}
                      </strong>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
      </section>
    </main>
  );
}