package py.fpune.soracare.mvc.model;

import java.time.LocalDate;

public class Medico extends Persona {

    private Long idMedico;
    private String matriculaProfesional;
    private LocalDate fechaIngreso;
    private EstadoMedico estado;
    private String biografia;

    public Medico() {
        super();

        this.estado =
                EstadoMedico.ACTIVO;
    }

    public Long getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(
            Long idMedico
    ) {
        this.idMedico =
                idMedico;
    }

    public String getMatriculaProfesional() {
        return matriculaProfesional;
    }

    public void setMatriculaProfesional(
            String matriculaProfesional
    ) {
        this.matriculaProfesional =
                matriculaProfesional;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(
            LocalDate fechaIngreso
    ) {
        this.fechaIngreso =
                fechaIngreso;
    }

    public EstadoMedico getEstado() {
        return estado;
    }

    public void setEstado(
            EstadoMedico estado
    ) {
        this.estado =
                estado;
    }

    public String getBiografia() {
        return biografia;
    }

    public void setBiografia(
            String biografia
    ) {
        this.biografia =
                biografia;
    }
}