package py.fpune.soracare.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import py.fpune.soracare.api.controller.ApiRestExcepciones;
import py.fpune.soracare.api.controller.CitaRestController;
import py.fpune.soracare.api.controller.DisponibilidadRestController;
import py.fpune.soracare.api.controller.DisponibilidadMedicoRestController;
import py.fpune.soracare.api.controller.EspecialidadRestController;
import py.fpune.soracare.api.controller.InformacionClinicaPacienteRestController;
import py.fpune.soracare.api.controller.MedicoRestController;
import py.fpune.soracare.api.controller.SesionRestController;
import py.fpune.soracare.service.CitaService;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.DocumentoClinicoService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;
import py.fpune.soracare.service.GestionDisponibilidadMedicoService;
import py.fpune.soracare.service.PacienteService;
import py.fpune.soracare.service.RecetaMedicaService;
import py.fpune.soracare.service.RecetaPdfService;
import py.fpune.soracare.service.ReservaPagoService;

@Configuration
@EnableWebMvc
public class ApiRestConfiguracion {

    @Bean
    public EspecialidadService especialidadService() {
        return new EspecialidadService();
    }

    @Bean
    public EspecialidadRestController especialidadRestController(
            EspecialidadService especialidadService
    ) {
        return new EspecialidadRestController(especialidadService);
    }

    @Bean
    public MedicoService medicoService() {
        return new MedicoService();
    }

    @Bean
    public MedicoEspecialidadService medicoEspecialidadService() {
        return new MedicoEspecialidadService();
    }

    @Bean
    public MedicoRestController medicoRestController(
            MedicoService medicoService,
            EspecialidadService especialidadService,
            MedicoEspecialidadService medicoEspecialidadService
    ) {
        return new MedicoRestController(
                medicoService,
                especialidadService,
                medicoEspecialidadService
        );
    }

    @Bean
    public DisponibilidadService disponibilidadService() {
        return new DisponibilidadService();
    }

    @Bean
    public DisponibilidadRestController disponibilidadRestController(
            DisponibilidadService disponibilidadService
    ) {
        return new DisponibilidadRestController(disponibilidadService);
    }

    @Bean
    public GestionDisponibilidadMedicoService gestionDisponibilidadMedicoService() {
        return new GestionDisponibilidadMedicoService();
    }

    @Bean
    public DisponibilidadMedicoRestController disponibilidadMedicoRestController(
            GestionDisponibilidadMedicoService gestionDisponibilidadMedicoService
    ) {
        return new DisponibilidadMedicoRestController(
                gestionDisponibilidadMedicoService
        );
    }

    @Bean
    public SesionRestController sesionRestController() {
        return new SesionRestController();
    }

    @Bean
    public CitaService citaService() {
        return new CitaService();
    }

    @Bean
    public PacienteService pacienteService() {
        return new PacienteService();
    }

    @Bean
    public ReservaPagoService reservaPagoService() {
        return new ReservaPagoService();
    }

    @Bean
    public CitaRestController citaRestController(
            CitaService citaService,
            PacienteService pacienteService,
            ReservaPagoService reservaPagoService
    ) {
        return new CitaRestController(
                citaService,
                pacienteService,
                reservaPagoService
        );
    }

    @Bean
    public RecetaMedicaService recetaMedicaService() {
        return new RecetaMedicaService();
    }

    @Bean
    public RecetaPdfService recetaPdfService() {
        return new RecetaPdfService();
    }

    @Bean
    public DocumentoClinicoService documentoClinicoService() {
        return new DocumentoClinicoService();
    }

    @Bean
    public InformacionClinicaPacienteRestController
    informacionClinicaPacienteRestController(
            RecetaMedicaService recetaMedicaService,
            RecetaPdfService recetaPdfService,
            DocumentoClinicoService documentoClinicoService
    ) {
        return new InformacionClinicaPacienteRestController(
                recetaMedicaService,
                recetaPdfService,
                documentoClinicoService
        );
    }

    @Bean
    public ApiRestExcepciones apiRestExcepciones() {
        return new ApiRestExcepciones();
    }
}
