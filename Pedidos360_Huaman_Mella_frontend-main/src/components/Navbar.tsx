import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthenticator } from '@aws-amplify/ui-react';

import {
  getAuthInfo,
  type AuthInfo,
} from '../lib/auth';

export default function Navbar() {
  const { signOut, user } = useAuthenticator((context) => [
    context.user,
  ]);

  const navigate = useNavigate();

  const [authInfo, setAuthInfo] =
    useState<AuthInfo | null>(null);

  useEffect(() => {
    let activo = true;

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

  async function cerrarSesion() {
    await signOut();
    navigate('/login', { replace: true });
  }

  const email = user?.signInDetails?.loginId;

  const linkStyle = ({
    isActive,
  }: {
    isActive: boolean;
  }) => ({
    textDecoration: 'none',
    color: isActive ? '#ffffff' : '#e5e7eb',
    fontWeight: isActive ? 700 : 500,
  });

  return (
    <header
      style={{
        background: '#111827',
        color: '#ffffff',
        padding: '16px 24px',
      }}
    >
      <div
        style={{
          maxWidth: 1200,
          margin: '0 auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 24,
          flexWrap: 'wrap',
        }}
      >
        <strong style={{ fontSize: 22 }}>
          Pedidos360
        </strong>

        <nav
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 20,
            flexWrap: 'wrap',
          }}
        >
          <NavLink
            to="/catalogo"
            style={linkStyle}
          >
            Catálogo
          </NavLink>

          {authInfo?.isCliente && (
            <NavLink
              to="/cliente"
              style={linkStyle}
            >
              Mi cuenta
            </NavLink>
          )}

          {authInfo?.isAdmin && (
            <NavLink
              to="/admin"
              style={linkStyle}
            >
              Administración
            </NavLink>
          )}

          <NavLink
            to="/diagnostico"
            style={linkStyle}
          >
            Diagnóstico
          </NavLink>
        </nav>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <span>{email}</span>

          <button onClick={cerrarSesion}>
            Cerrar sesión
          </button>
        </div>
      </div>
    </header>
  );
}