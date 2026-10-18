package cl.pedidos360.mensajeria_admin_service.comun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NombresRecursosTest {

    private final NombresRecursos nombres = new NombresRecursos(SqsFalso.props(true));

    @Test
    void agregaPrefijoSiFalta() {
        assertEquals("pedidos360-pagos", nombres.normalizar("pagos"));
        assertEquals("pedidos360-pagos", nombres.normalizar("pedidos360-pagos"));
    }

    @Test
    void rechazaNombresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> nombres.normalizar("../otra"));
        assertThrows(IllegalArgumentException.class, () -> nombres.normalizar("con espacio"));
        assertThrows(IllegalArgumentException.class, () -> nombres.normalizar(null));
    }

    @Test
    void soloAceptaArnDelProyecto() {
        nombres.validarArn("arn:aws:sns:us-east-1:123:pedidos360-eventos");
        nombres.validarArn("arn:aws:sns:us-east-1:123:pedidos360-eventos:1b2c-uuid");
        assertThrows(AccesoDenegadoRecursoException.class,
                () -> nombres.validarArn("arn:aws:sns:us-east-1:123:otro-topico"));
        assertThrows(IllegalArgumentException.class, () -> nombres.validarArn("cualquier-cosa"));
    }

    @Test
    void colaOrigenDeDlq() {
        assertEquals("pedidos360-documentos", nombres.colaOrigenDeDlq("pedidos360-documentos-dlq"));
        assertThrows(IllegalArgumentException.class, () -> nombres.colaOrigenDeDlq("pedidos360-documentos"));
    }

    @Test
    void confirmacionDebeCoincidirExacta() {
        nombres.exigirConfirmacion("pedidos360-x", "pedidos360-x");
        assertThrows(ConfirmacionRequeridaException.class, () -> nombres.exigirConfirmacion("pedidos360-x", "x"));
        assertThrows(ConfirmacionRequeridaException.class, () -> nombres.exigirConfirmacion("pedidos360-x", null));
    }
}
