import { useState } from 'react';
import { apiFetch } from '../lib/api';
import './IntegrationPage.css';

export default function IntegrationPage() {
  const [resultado, setResultado] = useState('');
  const [modeloNuevo, setModeloNuevo] = useState(
    'Producto de prueba',
  );
  const [marcaNueva, setMarcaNueva] = useState('Pedidos360');
  const [categoriaNueva, setCategoriaNueva] =
    useState('General');
  const [tallaNueva, setTallaNueva] = useState(40);
  const [precioNuevo, setPrecioNuevo] = useState(10000);
  const [stockNuevo, setStockNuevo] = useState(10);
  const [productoEliminarId, setProductoEliminarId] =
    useState(1);

  async function probar(
    ruta: string,
    method = 'GET',
  ) {
    setResultado('Llamando...');

    try {
      const respuesta = await apiFetch(ruta, {
        method,
      });

      const texto = await respuesta.text();

      setResultado(
        `${method} ${ruta} -> ${respuesta.status}\n${texto.slice(
          0,
          500,
        )}`,
      );
    } catch (error) {
      setResultado(
        `Error: ${(error as Error).message}`,
      );
    }
  }

  async function crearProductoPrueba() {
    setResultado('Creando producto e inventario...');

    try {
      const respuesta = await apiFetch('/api/productos', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          modelo: modeloNuevo,
          marca: marcaNueva,
          categoria: categoriaNueva,
          talla: tallaNueva,
          precio: precioNuevo,
        }),
      });

      const texto = await respuesta.text();

      if (!respuesta.ok) {
        setResultado(
          `POST /api/productos -> ${respuesta.status}\n${texto.slice(
            0,
            1000,
          )}`,
        );
        return;
      }

      const producto = JSON.parse(texto) as { id: number };
      const respuestaInventario = await apiFetch(
        '/api/inventario',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            productoId: producto.id,
            stockDisponible: stockNuevo,
            stockReservado: 0,
          }),
        },
      );

      const textoInventario =
        await respuestaInventario.text();

      setResultado(
        `POST /api/productos -> ${respuesta.status}\n${texto}\n\nPOST /api/inventario -> ${respuestaInventario.status}\n${textoInventario.slice(
          0,
          1000,
        )}`,
      );
    } catch (error) {
      setResultado(
        `Error: ${(error as Error).message}`,
      );
    }
  }

  async function borrarProducto() {
    setResultado('Borrando producto...');

    try {
      const respuesta = await apiFetch(
        `/api/productos/${productoEliminarId}`,
        { method: 'DELETE' },
      );

      const texto = await respuesta.text();

      setResultado(
        `DELETE /api/productos/${productoEliminarId} -> ${respuesta.status}\n${texto.slice(
          0,
          1000,
        )}`,
      );
    } catch (error) {
      setResultado(
        `Error: ${(error as Error).message}`,
      );
    }
  }

  return (
    <div className="integration-page">
      <h1>Pedidos360 - prueba de integración</h1>

      <h2>Llamadas al API Gateway</h2>

      <div className="integration-page__cards">
        <section className="integration-card">
          <h3>Consultas GET</h3>

          <div className="integration-card__actions">
            <button
              onClick={() => probar('/api/pedidos')}
            >
              GET pedidos
            </button>

            <button
              onClick={() => probar('/api/pedidos/mios')}
            >
              GET mis pedidos
            </button>

            <button
              onClick={() => probar('/api/productos')}
            >
              GET productos
            </button>

            <button
              onClick={() => probar('/api/inventario')}
            >
              GET inventario
            </button>
          </div>
        </section>

        <section className="integration-card">
          <h3>Operaciones de escritura</h3>

          <div className="integration-card__actions integration-card__actions--products">
            <section className="integration-product-card">
              <h4>Productos</h4>

              <label className="integration-field">
                Modelo:{' '}
                <input
                  value={modeloNuevo}
                  onChange={(event) =>
                    setModeloNuevo(event.target.value)
                  }
                />
              </label>

              <label className="integration-field">
                Marca:{' '}
                <input
                  value={marcaNueva}
                  onChange={(event) =>
                    setMarcaNueva(event.target.value)
                  }
                />
              </label>

              <label className="integration-field">
                Categoría:{' '}
                <input
                  value={categoriaNueva}
                  onChange={(event) =>
                    setCategoriaNueva(event.target.value)
                  }
                />
              </label>

              <label className="integration-field">
                Talla:{' '}
                <input
                  type="number"
                  min={1}
                  value={tallaNueva}
                  onChange={(event) =>
                    setTallaNueva(Number(event.target.value))
                  }
                />
              </label>

              <label className="integration-field">
                Precio:{' '}
                <input
                  type="number"
                  min={0}
                  value={precioNuevo}
                  onChange={(event) =>
                    setPrecioNuevo(Number(event.target.value))
                  }
                  className="integration-field__price"
                />
              </label>

              <label className="integration-field">
                Stock del producto:{' '}
                <input
                  type="number"
                  min={0}
                  value={stockNuevo}
                  onChange={(event) =>
                    setStockNuevo(Number(event.target.value))
                  }
                />
              </label>

              <button onClick={crearProductoPrueba}>
                POST producto (solo ADMIN)
              </button>

              <label className="integration-field">
                ID del producto:{' '}
                <input
                  type="number"
                  min={1}
                  value={productoEliminarId}
                  onChange={(event) =>
                    setProductoEliminarId(
                      Number(event.target.value),
                    )
                  }
                />
              </label>

              <button onClick={borrarProducto}>
                DELETE producto (solo ADMIN)
              </button>
            </section>
          </div>
        </section>
      </div>

      <pre className="integration-result">{resultado}</pre>
    </div>
  );
}