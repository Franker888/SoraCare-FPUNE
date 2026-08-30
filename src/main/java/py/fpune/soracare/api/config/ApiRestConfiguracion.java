package py.fpune.soracare.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import py.fpune.soracare.api.controller.ApiRestExcepciones;
import py.fpune.soracare.api.controller.DisponibilidadRestController;
import py.fpune.soracare.api.controller.EspecialidadRestController;
import py.fpune.soracare.api.controller.MedicoRestController;
import py.fpune.soracare.service.DisponibilidadService;
import py.fpune.soracare.service.EspecialidadService;
import py.fpune.soracare.service.MedicoEspecialidadService;
import py.fpune.soracare.service.MedicoService;

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
    public ApiRestExcepciones apiRestExcepciones() {
        return new ApiRestExcepciones();
    }
}
