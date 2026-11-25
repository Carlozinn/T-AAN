/** Pantalla de inicio de sesión que corresponde a una URL: la institucional para /institucional y /admin. */
export function rutaDeAcceso(url: string): string {
  return url.startsWith('/institucional') || url.startsWith('/admin') ? '/institucional/acceso' : '/acceso';
}

/** Devuelve «volver» solo si es una ruta interna (evita redirecciones a otros sitios). */
export function destinoSeguro(volver: string | null | undefined, porDefecto: string): string {
  return volver && volver.startsWith('/') && !volver.startsWith('//') && !volver.startsWith('/\\') ? volver : porDefecto;
}
