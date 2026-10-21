package cl.pedidos360.mensajeria_admin_service.comun;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class NombresRecursos {

    private static final Pattern NOMBRE = Pattern.compile("^[a-zA-Z0-9_-]{1,80}$");
    public static final String SUFIJO_DLQ = "-dlq";

    private final String prefijo;

    public NombresRecursos(AdminProperties props) {
        this.prefijo = props.prefijo();
    }

    public String prefijo() {
        return prefijo;
    }

    public String normalizar(String nombre) {
        if (nombre == null || !NOMBRE.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre invalido: solo letras, numeros, '-' y '_' (max 80)");
        }
        String completo = nombre.startsWith(prefijo + "-") ? nombre : prefijo + "-" + nombre;
        if (completo.length() > 80) {
            throw new IllegalArgumentException("Nombre demasiado largo");
        }
        return completo;
    }

    public void validarArn(String arn) {
        if (arn == null || !arn.startsWith("arn:aws:")) {
            throw new IllegalArgumentException("ARN invalido");
        }
        String recurso = arn.substring(arn.lastIndexOf(':') + 1);
        String[] partes = arn.split(":");
        boolean propio = recurso.startsWith(prefijo + "-")
                || (partes.length >= 7 && partes[5].startsWith(prefijo + "-"));
        if (!propio) {
            throw new AccesoDenegadoRecursoException("Solo se administran recursos con prefijo '" + prefijo + "-'");
        }
    }

    public boolean esDlq(String nombreCompleto) {
        return nombreCompleto.endsWith(SUFIJO_DLQ);
    }

    public String colaOrigenDeDlq(String nombreDlq) {
        if (!esDlq(nombreDlq)) {
            throw new IllegalArgumentException("'" + nombreDlq + "' no es una DLQ (debe terminar en -dlq)");
        }
        return nombreDlq.substring(0, nombreDlq.length() - SUFIJO_DLQ.length());
    }

    public void exigirConfirmacion(String nombreCompleto, String confirmar) {
        if (!nombreCompleto.equals(confirmar)) {
            throw new ConfirmacionRequeridaException(
                    "Operacion destructiva: envie ?confirmar=" + nombreCompleto + " para continuar");
        }
    }
}
