import { useAuthenticator } from '@aws-amplify/ui-react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';

export default function ProtectedRoute() {
  const { authStatus } = useAuthenticator((context) => [
    context.authStatus,
  ]);

  const location = useLocation();

  if (authStatus === 'configuring') {
    return <p>Cargando sesión...</p>;
  }

  if (authStatus !== 'authenticated') {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location }}
      />
    );
  }

  return <Outlet />;
}