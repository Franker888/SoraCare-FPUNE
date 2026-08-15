package py.fpune.soracare.mvc.model;

public final class MedicamentoValidacionTest {

    private MedicamentoValidacionTest() {
    }

    public static void main(String[] args) {
        Medicamento medicamento = new Medicamento();
        medicamento.setNombreComercial(" Paracetamol ");
        medicamento.setPrincipioActivo(" Acetaminofén ");
        medicamento.setPresentacion(" Comprimidos ");
        medicamento.setConcentracion(" 500 mg ");
        medicamento.setViaAdministracion(ViaAdministracion.ORAL);

        verificar("Paracetamol".equals(medicamento.getNombreComercial()),
                "El nombre debe normalizar espacios laterales.");
        verificar("500 mg".equals(medicamento.getConcentracion()),
                "La concentración debe normalizarse.");
        esperarRechazo(() -> medicamento.setNombreComercial(""));
        esperarRechazo(() -> medicamento.setViaAdministracion(null));

        System.out.println("OK: validaciones del catálogo de medicamentos verificadas.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void esperarRechazo(Operacion operacion) {
        try {
            operacion.ejecutar();
            throw new AssertionError("Se esperaba que el dato inválido fuera rechazado.");
        } catch (IllegalArgumentException esperado) {
            // Resultado esperado.
        }
    }

    @FunctionalInterface
    private interface Operacion {
        void ejecutar();
    }
}
