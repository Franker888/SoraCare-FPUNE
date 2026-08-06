package py.fpune.soracare.mvc.model;

public class Medico extends Persona {

    private String matriculaProfesional;

    public Medico() {
        super();
    }

    public String getMatriculaProfesional() {
        return matriculaProfesional;
    }

    public void setMatriculaProfesional(String matriculaProfesional) {
        this.matriculaProfesional = matriculaProfesional;
    }
}