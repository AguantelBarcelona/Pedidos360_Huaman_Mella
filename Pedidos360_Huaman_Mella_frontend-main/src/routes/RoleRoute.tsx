import { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import {
  getAuthInfo,
  type AuthInfo,
} from '../lib/auth';

type RoleRouteProps = {
  allowedRoles: string[];
};

export default function RoleRoute({
  allowedRoles,
}: RoleRouteProps) {
  const [authInfo, setAuthInfo] = useState<AuthInfo | null>(null);

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
          setAuthInfo({
            isAuthenticated: false,
            groups: [],
            scopes: [],
            isAdmin: false,
            isCliente: false,
            claims: {},
          });
        }
      });

    return () => {
      activo = false;
    };
  }, []);

  if (!authInfo) {
    return <p>Verificando permisos...</p>;
  }

  if (!authInfo.isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const tieneRol = authInfo.groups.some((grupo) =>
    allowedRoles.includes(grupo),
  );

  if (!tieneRol) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}