// Plantilla de claves locales: npm install la copia como environment.local.ts si no existe (ese archivo no se sube).
// Son claves públicas de navegador, restringidas por dominio en cada servicio; aquí nunca van secretos.
export const claves = {
  // Clave de prueba de Cloudflare Turnstile: siempre aprueba. En despliegue se reemplaza por la real.
  turnstileSiteKey: '1x00000000000000000000AA',
};
