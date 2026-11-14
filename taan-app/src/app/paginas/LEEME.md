# Páginas de la app (solo para ciudadanos)

La app es para ciudadanos; el personal institucional y el administrador usan la web.
Crear cada página con `ionic generate page paginas/<nombre>`:

- **acceso**: Continuar con Google / Usar mi correo, registro y recuperación (CU-04, CU-24)
- **nuevo-reporte**: descripción, ubicación (UbicacionService) y fotos (CamaraService) (CU-01)
- **mis-reportes** y **detalle-reporte**: seguimiento, canales externos y reapertura (CU-02, CU-03, CU-23)
- **perfil**: teléfono, avisos y eliminación de cuenta (CU-25)

Al iniciar sesión, llamar a NotificacionesPushService.registrar() y enviar el token a
POST /ciudadano/dispositivos.
