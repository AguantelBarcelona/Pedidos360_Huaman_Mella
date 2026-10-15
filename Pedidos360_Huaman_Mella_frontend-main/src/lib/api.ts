import { fetchAuthSession, signOut } from 'aws-amplify/auth';
import { config } from '../config';

export class SesionExpirada extends Error {}

export async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const session = await fetchAuthSession();
  const token = session.tokens?.accessToken?.toString();

  if (!token) throw new SesionExpirada('No hay una sesion activa');

  const respuesta = await fetch(`${config.apiUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  });

  if (respuesta.status === 401) {
    await signOut();
    throw new SesionExpirada('Tu sesion expiro, vuelve a iniciar sesion');
  }

  return respuesta;
}