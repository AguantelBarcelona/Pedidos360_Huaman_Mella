import { fetchAuthSession } from 'aws-amplify/auth';

export type AuthInfo = {
  isAuthenticated: boolean;
  groups: string[];
  scopes: string[];
  username?: string;
  isAdmin: boolean;
  isCliente: boolean;
  claims: Record<string, unknown>;
};

function obtenerGrupos(valor: unknown): string[] {
  if (Array.isArray(valor)) {
    return valor.filter((grupo): grupo is string => typeof grupo === 'string');
  }

  if (typeof valor === 'string') {
    return [valor];
  }

  return [];
}

function obtenerScopes(valor: unknown): string[] {
  if (typeof valor !== 'string') {
    return [];
  }

  return valor.split(/\s+/).filter(Boolean);
}

export async function getAuthInfo(): Promise<AuthInfo> {
  const session = await fetchAuthSession();
  const accessToken = session.tokens?.accessToken;

  if (!accessToken) {
    return {
      isAuthenticated: false,
      groups: [],
      scopes: [],
      isAdmin: false,
      isCliente: false,
      claims: {},
    };
  }

  const claims = accessToken.payload as Record<string, unknown>;

  const groups = obtenerGrupos(claims['cognito:groups']);
  const scopes = obtenerScopes(claims.scope);

  const username =
    typeof claims.username === 'string'
      ? claims.username
      : undefined;

  return {
    isAuthenticated: true,
    groups,
    scopes,
    username,
    isAdmin: groups.includes('ADMIN'),
    isCliente: groups.includes('CLIENTE'),
    claims,
  };
}