import { ApiError } from './api-error';

/**
 * Mensajes para los códigos de error del contrato. Se redactan como medidas preventivas,
 * no como fallas (RNF-022), y nunca revelan los valores de los límites.
 */
const MENSAJES: Record<string, string> = {
  VALIDACION: 'Revisa los datos marcados.',
  SIN_CONEXION: 'No pudimos comunicarnos con T’AAN. Revisa tu conexión e inténtalo de nuevo.',
  NO_ENCONTRADO: 'No encontramos lo que buscas o no tienes acceso a ello.',
  FUERA_DE_COBERTURA: 'T’AAN todavía no opera en esta ubicación. Si es una emergencia, llama al 911.',
  CORREO_REGISTRADO: 'Ese correo ya tiene una cuenta. Inicia sesión o recupera tu acceso.',
  ENLACE_VENCIDO: 'El enlace ya no es válido. Solicita uno nuevo.',
  REFRESCO_INVALIDO: 'Tu sesión terminó. Vuelve a iniciar sesión.',
  TELEFONO_REQUERIDO: 'Agrega tu número de teléfono en tu perfil antes de enviar el reporte.',
  CUENTA_SUSPENDIDA: 'Tu cuenta está suspendida temporalmente. Puedes consultar tus reportes, pero no crear nuevos.',
  LIMITE_EXCEDIDO:
    'Por seguridad, espera unos minutos antes de volver a intentarlo. Lo que escribiste se conservó. ' +
    'Si es una emergencia, llama al 911.',
  TRANSICION_INVALIDA: 'Este reporte ya cambió de estado. Actualiza la página para ver su situación actual.',
  YA_REABIERTO: 'Este reporte ya se reabrió una vez. Si el problema continúa, registra un reporte nuevo.',
  YA_CONFIRMADO: 'Ya confirmaste que este problema se resolvió, así que no puede reabrirse. Si vuelve a ocurrir, registra un reporte nuevo.',
  PLAZO_VENCIDO: 'El plazo para reabrir este reporte terminó. Si el problema continúa, registra un reporte nuevo.',
};

const MENSAJE_GENERAL = 'No pudimos completar la operación. Inténtalo de nuevo en un momento.';

/**
 * Mensaje para mostrar al usuario. Si el código no tiene mensaje propio se usa el del backend,
 * que ya viene redactado para el usuario, solo en errores 4xx; nunca en fallas del servidor (RNF-019).
 */
export function mensajeDeError(error: ApiError): string {
  const usarDelBackend = error.estado >= 400 && error.estado < 500 && error.mensaje;
  return MENSAJES[error.codigo] ?? (usarDelBackend ? error.mensaje : MENSAJE_GENERAL);
}
