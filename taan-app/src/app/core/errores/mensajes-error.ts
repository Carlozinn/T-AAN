/**
 * Mensajes para los códigos de error del contrato. Se redactan como medidas preventivas,
 * no como fallas (RNF-022), y nunca revelan los valores de los límites.
 */
const MENSAJES: Record<string, string> = {
  VALIDACION: 'Revisa los datos marcados.',
  FUERA_DE_COBERTURA: 'T’AAN todavía no opera en esta ubicación. Si es una emergencia, llama al 911.',
  CORREO_REGISTRADO: 'Ese correo ya tiene una cuenta. Inicia sesión o recupera tu acceso.',
  ENLACE_VENCIDO: 'El enlace ya no es válido. Solicita uno nuevo.',
  TELEFONO_REQUERIDO: 'Agrega tu número de teléfono en tu perfil antes de enviar el reporte.',
  CUENTA_SUSPENDIDA: 'Tu cuenta está suspendida temporalmente. Puedes consultar tus reportes, pero no crear nuevos.',
  LIMITE_EXCEDIDO: 'Por seguridad, espera unos minutos antes de volver a intentarlo. Lo que escribiste se conservó.',
  TRANSICION_INVALIDA: 'Este reporte ya cambió de estado. Actualiza la página para ver su situación actual.',
  YA_REABIERTO: 'Este reporte ya se reabrió una vez. Si el problema continúa, registra un reporte nuevo.',
  PLAZO_VENCIDO: 'El plazo para reabrir este reporte terminó. Si el problema continúa, registra un reporte nuevo.',
};

const MENSAJE_GENERAL = 'No pudimos completar la operación. Inténtalo de nuevo en un momento.';

export function mensajeDeError(codigo: string | null | undefined): string {
  return (codigo && MENSAJES[codigo]) || MENSAJE_GENERAL;
}
