import { claves } from './environment.local';

// Producción: la web se publica junto al backend, que atiende en /api/v1.
export const environment = {
  production: true,
  apiUrl: '/api/v1',
  ...claves,
};
