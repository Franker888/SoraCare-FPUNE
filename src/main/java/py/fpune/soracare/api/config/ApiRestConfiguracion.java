package py.fpune.soracare.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import py.fpune.soracare.api.controller.ApiRestExcepciones;
import py.fpune.soracare.api.controller.EspecialidadRestController;
import py.fpune.soracare.service.EspecialidadService;

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
    public ApiRestExcepciones apiRestExcepciones() {
        return new ApiRestExcepciones();
    }
}
