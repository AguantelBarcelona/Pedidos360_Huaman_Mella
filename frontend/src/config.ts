export const config = {
  userPoolId: import.meta.env.VITE_COGNITO_USER_POOL_ID as string,
  userPoolClientId: import.meta.env.VITE_COGNITO_CLIENT_ID as string,
  apiUrl: import.meta.env.VITE_API_URL as string,
};