package mx.uv.taan.ia;

import java.util.Map;

/** Proveedor de prueba: responde un texto fijo o falla, y guarda lo último que recibió. */
class ProveedorFalso implements ProveedorIa {

    private final String nombre;
    private final boolean configurado;
    private final String respuesta;
    int llamadas;
    String ultimosDatos;

    private ProveedorFalso(String nombre, boolean configurado, String respuesta) {
        this.nombre = nombre;
        this.configurado = configurado;
        this.respuesta = respuesta;
    }

    static ProveedorFalso queResponde(String nombre, String json) {
        return new ProveedorFalso(nombre, true, json);
    }

    static ProveedorFalso queFalla(String nombre) {
        return new ProveedorFalso(nombre, true, null);
    }

    static ProveedorFalso sinClave(String nombre) {
        return new ProveedorFalso(nombre, false, null);
    }

    @Override
    public String nombre() {
        return nombre;
    }

    @Override
    public String modelo() {
        return nombre + "-modelo";
    }

    @Override
    public boolean configurado() {
        return configurado;
    }

    @Override
    public String generarJson(String instrucciones, String datos, Map<String, Object> esquema) {
        llamadas++;
        ultimosDatos = datos;
        if (respuesta == null) {
            throw new IllegalStateException("proveedor caído");
        }
        return respuesta;
    }
}
