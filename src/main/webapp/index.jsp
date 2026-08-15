<%@ page contentType="text/html;charset=UTF-8"
         pageEncoding="UTF-8"
         language="java" %>
<%@ page import="py.fpune.soracare.dto.UsuarioAutenticadoDTO" %>

<%
    String contexto = request.getContextPath();
    Object usuarioEnSesion = session.getAttribute("usuarioAutenticado");
    UsuarioAutenticadoDTO usuarioAutenticado =
            usuarioEnSesion instanceof UsuarioAutenticadoDTO
                    ? (UsuarioAutenticadoDTO) usuarioEnSesion
                    : null;
    boolean sesionIniciada = usuarioAutenticado != null;
    boolean esPaciente = sesionIniciada
            && usuarioAutenticado.tieneRol("PACIENTE");
    boolean esPersonalAgenda = sesionIniciada
            && (usuarioAutenticado.tieneRol("ADMINISTRADOR")
            || usuarioAutenticado.tieneRol("RECEPCIONISTA"));
    String rutaPanel = contexto + "/dashboard";
    String rutaAgenda = !sesionIniciada
            ? contexto + "/login"
            : esPaciente
                    ? contexto + "/mis-citas"
                    : esPersonalAgenda
                            ? contexto + "/agenda/citas"
                            : rutaPanel;
    String rutaPagos = !sesionIniciada
            ? contexto + "/login"
            : esPaciente
                    ? contexto + "/mis-citas"
                    : esPersonalAgenda
                            ? contexto + "/agenda/pagos"
                            : rutaPanel;
    String textoAcceso = !sesionIniciada
            ? "Iniciar sesión"
            : esPaciente
                    ? "Mis citas"
                    : "Mi panel";
%>

<!DOCTYPE html>
<html lang="es">

<head>

    <meta charset="UTF-8">

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <meta name="description"
          content="SoraCare-FPUNE: especialidades, médicos, citas y pagos en un solo lugar.">

    <title>SoraCare-FPUNE | Centro médico integral</title>

    <link rel="icon"
          type="image/png"
          href="<%= contexto %>/assets/home/img/logo/SoraCare1.png">


    <!-- =========================
         BOOTSTRAP
    ========================== -->

    <link
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
        rel="stylesheet">


    <!-- =========================
         BOOTSTRAP ICONS
    ========================== -->

    <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.13.1/font/bootstrap-icons.min.css">


    <!-- =========================
         FONT AWESOME
         Utilizado para algunos íconos
    ========================== -->

    <link
        rel="stylesheet"
        href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">


    <!-- =========================
         GOOGLE FONTS
    ========================== -->

    <link
        href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap"
        rel="stylesheet">


    <!-- =========================
         CSS DEL PROYECTO
    ========================== -->

    <link rel="stylesheet"
          href="<%= contexto %>/assets/home/css/style.css">

</head>


<body>


    <!-- ==================================================
         NAVBAR
    =================================================== -->

    <nav class="navbar navbar-expand-lg">

        <div class="container">


            <!-- LOGO -->

            <a class="navbar-brand"
               href="<%= contexto %>/"
               aria-label="Ir al inicio de SoraCare">

                <img
                    src="<%= contexto %>/assets/home/img/logo/SoraCare1.png"
                    alt="Logo SoraCare">

            </a>


            <!-- BOTÓN PARA CELULAR -->

            <button
                class="navbar-toggler"
                type="button"
                data-bs-toggle="collapse"
                data-bs-target="#menu"
                aria-controls="menu"
                aria-expanded="false"
                aria-label="Abrir menú">

                <span class="navbar-toggler-icon"></span>

            </button>


            <!-- MENÚ -->

            <div class="collapse navbar-collapse" id="menu">

                <ul class="navbar-nav mx-auto">


                    <li class="nav-item">

                        <a
                            class="nav-link active"
                            href="<%= contexto %>/#inicio">

                            Inicio

                        </a>

                    </li>


                    <li class="nav-item">

                        <a
                            class="nav-link"
                            href="<%= contexto %>/#nosotros">

                            Nosotros

                        </a>

                    </li>


                    <li class="nav-item">

                        <a
                            class="nav-link"
                            href="<%= contexto %>/especialidades">

                            Especialidades

                        </a>

                    </li>


                    <li class="nav-item">

                        <a
                            class="nav-link"
                            href="<%= contexto %>/medicos">

                            Médicos

                        </a>

                    </li>


                    <li class="nav-item">

                        <a
                            class="nav-link"
                            href="<%= contexto %>/#contacto">

                            Contacto

                        </a>

                    </li>


                </ul>


                <!-- ACCESO A LA CUENTA -->

                <div class="session-actions">

                    <% if (!sesionIniciada) { %>

                    <a
                        href="<%= contexto %>/registro"
                        class="btn btn-outline-primary btn-register">

                        Crear cuenta

                    </a>

                    <% } %>

                    <a
                        href="<%= sesionIniciada
                                ? (esPaciente ? rutaAgenda : rutaPanel)
                                : contexto + "/login" %>"
                        class="btn btn-primary btn-login">

                        <i class="bi bi-person-circle me-2"></i>

                        <%= textoAcceso %>

                    </a>

                    <% if (sesionIniciada) { %>

                    <a
                        href="<%= contexto %>/logout"
                        class="btn btn-outline-secondary btn-logout"
                        aria-label="Cerrar sesión"
                        title="Cerrar sesión">

                        <i class="bi bi-box-arrow-right"></i>

                    </a>

                    <% } %>

                </div>


            </div>

        </div>

    </nav>



    <!-- ==================================================
         HERO / CARRUSEL
    =================================================== -->

    <section class="hero" id="inicio">

        <div
            id="heroCarousel"
            class="carousel slide"
            data-bs-ride="carousel">


            <!-- INDICADORES -->

            <div class="carousel-indicators">

                <button
                    type="button"
                    data-bs-target="#heroCarousel"
                    data-bs-slide-to="0"
                    class="active"
                    aria-current="true"
                    aria-label="Diapositiva 1">
                </button>

                <button
                    type="button"
                    data-bs-target="#heroCarousel"
                    data-bs-slide-to="1"
                    aria-label="Diapositiva 2">
                </button>

                <button
                    type="button"
                    data-bs-target="#heroCarousel"
                    data-bs-slide-to="2"
                    aria-label="Diapositiva 3">
                </button>

            </div>



            <!-- CONTENIDO DEL CARRUSEL -->

            <div class="carousel-inner">


                <!-- DIAPOSITIVA 1 -->

                <div class="carousel-item active">

                    <img
                        src="<%= contexto %>/assets/home/img/hero/hero1.jpg"
                        class="hero-slide-image"
                        alt="Atención médica">


                    <div class="hero-overlay"></div>


                    <div class="carousel-caption hero-caption">

                        <span class="hero-subtitle">

                            Bienvenido a SoraCare

                        </span>


                        <h1>

                            Tu salud es importante.

                        </h1>


                        <p>

                            Estamos acá para ayudarte a encontrar
                            la atención médica que necesitás.

                        </p>


                        <a
                            href="<%= rutaAgenda %>"
                            class="btn btn-primary">

                            Agendar una cita

                        </a>

                    </div>

                </div>



                <!-- DIAPOSITIVA 2 -->

                <div class="carousel-item">

                    <img
                        src="<%= contexto %>/assets/home/img/hero/hero2.jpg"
                        class="hero-slide-image"
                        alt="Equipo médico">


                    <div class="hero-overlay"></div>


                    <div class="carousel-caption hero-caption">

                        <span class="hero-subtitle">

                            Profesionales a tu alcance

                        </span>


                        <h1>

                            Encontrá el especialista adecuado.

                        </h1>


                        <p>

                            Conocé nuestros profesionales y sus
                            diferentes especialidades.

                        </p>


                        <a
                            href="<%= contexto %>/especialidades"
                            class="btn btn-primary">

                            Ver especialistas

                        </a>

                    </div>

                </div>



                <!-- DIAPOSITIVA 3 -->

                <div class="carousel-item">

                    <img
                        src="<%= contexto %>/assets/home/img/hero/hero3.jpg"
                        class="hero-slide-image"
                        alt="Gestión de atención médica">


                    <div class="hero-overlay"></div>


                    <div class="carousel-caption hero-caption">

                        <span class="hero-subtitle">

                            Todo en un solo lugar

                        </span>


                        <h1>

                            Gestioná tu atención de forma sencilla.

                        </h1>


                        <p>

                            Reservá, pagá y confirmá tus citas
                            desde SoraCare.

                        </p>


                        <a
                            href="<%= rutaAgenda %>"
                            class="btn btn-primary">

                            Agendar una cita

                        </a>

                    </div>

                </div>


            </div>



            <!-- BOTÓN ANTERIOR -->

            <button
                class="carousel-control-prev"
                type="button"
                data-bs-target="#heroCarousel"
                data-bs-slide="prev">

                <span class="carousel-control-prev-icon"></span>

                <span class="visually-hidden">

                    Anterior

                </span>

            </button>



            <!-- BOTÓN SIGUIENTE -->

            <button
                class="carousel-control-next"
                type="button"
                data-bs-target="#heroCarousel"
                data-bs-slide="next">

                <span class="carousel-control-next-icon"></span>

                <span class="visually-hidden">

                    Siguiente

                </span>

            </button>


        </div>

    </section>



    <!-- ==================================================
         ACCIONES RÁPIDAS
    =================================================== -->

    <section class="quick-actions">

        <div class="container">


            <!-- TÍTULO -->

            <div class="text-center mb-5">

                <h2>

                    ¿Qué necesitás hacer?

                </h2>


                <p>

                    Encontrá rápidamente el servicio
                    que estás buscando.

                </p>

            </div>



            <!-- TARJETAS -->

            <div class="row g-4">


                <!-- AGENDAR CITA -->

                <div class="col-lg-4 col-md-6">

                    <div class="quick-action-card">

                        <div class="quick-action-icon">

                            <i class="bi bi-calendar-check"></i>

                        </div>


                        <h4>

                            Agendar una cita

                        </h4>


                        <p>

                            Reservá una consulta con el especialista
                            que necesitás de forma rápida y sencilla.

                        </p>


                        <a
                            href="<%= rutaAgenda %>"
                            class="quick-action-link">

                            Agendar cita

                            <i class="bi bi-arrow-right"></i>

                        </a>

                    </div>

                </div>



                <!-- ENCONTRAR MÉDICO -->

                <div class="col-lg-4 col-md-6">

                    <div class="quick-action-card">

                        <div class="quick-action-icon">

                            <i class="bi bi-person-badge"></i>

                        </div>


                        <h4>

                            Encontrar un médico

                        </h4>


                        <p>

                            Conocé a nuestros especialistas y
                            encontrá el profesional adecuado para vos.

                        </p>


                        <a
                            href="<%= contexto %>/medicos"
                            class="quick-action-link">

                            Ver médicos

                            <i class="bi bi-arrow-right"></i>

                        </a>

                    </div>

                </div>



                <!-- ESPECIALIDADES -->

                <div class="col-lg-4 col-md-6">

                    <div class="quick-action-card">

                        <div class="quick-action-icon">

                            <i class="bi bi-heart-pulse"></i>

                        </div>


                        <h4>

                            Ver especialidades

                        </h4>


                        <p>

                            Explorá las diferentes especialidades
                            y servicios médicos disponibles.

                        </p>


                        <a
                            href="<%= contexto %>/especialidades"
                            class="quick-action-link">

                            Ver especialidades

                            <i class="bi bi-arrow-right"></i>

                        </a>

                    </div>

                </div>


            </div>

        </div>

    </section>



    <!-- ==================================================
         ESPECIALIDADES
    =================================================== -->

    <section class="specialties" id="especialidades-destacadas">

        <div class="container">


            <!-- TÍTULO -->

            <div class="text-center mb-5">

                <h2>

                    Nuestras especialidades

                </h2>


                <p>

                    Contamos con profesionales especializados
                    para brindarte una atención médica integral
                    y de calidad.

                </p>

            </div>



            <!-- TARJETAS -->

            <div class="row g-4">


                <!-- CARDIOLOGÍA -->

                <div class="col-lg-3 col-md-6">

                    <div class="card specialty-card h-100">

                        <div class="card-body text-center">

                            <i class="bi bi-heart-pulse-fill specialty-icon"></i>


                            <h4 class="mt-3">

                                Cardiología

                            </h4>


                            <p>

                                Diagnóstico, tratamiento y prevención
                                de enfermedades cardiovasculares.

                            </p>


                            <a
                                href="<%= contexto %>/especialidades"
                                class="btn btn-outline-primary">

                                Ver más

                            </a>

                        </div>

                    </div>

                </div>



                <!-- NEUROLOGÍA -->

                <div class="col-lg-3 col-md-6">

                    <div class="card specialty-card h-100">

                        <div class="card-body text-center">

                            <i class="fa-solid fa-brain specialty-icon"></i>


                            <h4 class="mt-3">

                                Neurología

                            </h4>


                            <p>

                                Atención especializada para el diagnóstico
                                y tratamiento de enfermedades neurológicas.

                            </p>


                            <a
                                href="<%= contexto %>/especialidades"
                                class="btn btn-outline-primary">

                                Ver más

                            </a>

                        </div>

                    </div>

                </div>



                <!-- PEDIATRÍA -->

                <div class="col-lg-3 col-md-6">

                    <div class="card specialty-card h-100">

                        <div class="card-body text-center">

                            <i class="bi bi-emoji-smile specialty-icon"></i>


                            <h4 class="mt-3">

                                Pediatría

                            </h4>


                            <p>

                                Atención integral para el cuidado y
                                desarrollo saludable de niños y adolescentes.

                            </p>


                            <a
                                href="<%= contexto %>/especialidades"
                                class="btn btn-outline-primary">

                                Ver más

                            </a>

                        </div>

                    </div>

                </div>



                <!-- TRAUMATOLOGÍA -->

                <div class="col-lg-3 col-md-6">

                    <div class="card specialty-card h-100">

                        <div class="card-body text-center">

                            <i class="bi bi-person-walking specialty-icon"></i>


                            <h4 class="mt-3">

                                Traumatología

                            </h4>


                            <p>

                                Prevención, diagnóstico y tratamiento
                                de lesiones y enfermedades musculoesqueléticas.

                            </p>


                            <a
                                href="<%= contexto %>/especialidades"
                                class="btn btn-outline-primary">

                                Ver más

                            </a>

                        </div>

                    </div>

                </div>


            </div>



            <!-- VER TODAS -->

            <div class="text-center mt-5">

                <a
                    href="<%= contexto %>/especialidades"
                    class="btn btn-outline-primary specialties-link">

                    Ver todas las especialidades

                    <i class="bi bi-arrow-right ms-2"></i>

                </a>

            </div>


        </div>

    </section>



    <!-- ==================================================
         MÉDICOS DESTACADOS
    =================================================== -->

    <section class="doctors" id="medicos-destacados">

        <div class="container">


            <!-- TÍTULO -->

            <div class="text-center mb-5">

                <h2>

                    Nuestros especialistas

                </h2>


                <p>

                    Explorá el catálogo actualizado de profesionales
                    y elegí la atención que necesitás.

                </p>

            </div>



            <!-- MÉDICOS -->

            <div class="row g-4">


                <!-- MÉDICO 1 -->

                <div class="col-lg-4 col-md-6">

                    <div class="doctor-card">


                        <div class="doctor-image">

                            <img
                                src="<%= contexto %>/assets/home/img/doctors/doctor1.jpg"
                                alt="Profesional del equipo médico">

                        </div>


                        <div class="doctor-info">

                            <h4>

                                Profesionales verificados

                            </h4>


                            <span class="doctor-specialty">

                                Catálogo actualizado

                            </span>


                            <p>

                                Consultá los médicos activos y las
                                especialidades asociadas en SoraCare.

                            </p>


                            <div class="doctor-experience">

                                <i class="bi bi-award"></i>

                                Información profesional disponible

                            </div>


                            <div class="doctor-actions">

                                <a
                                    href="<%= contexto %>/medicos"
                                    class="btn btn-outline-primary">

                                    Ver médicos

                                </a>


                                <a
                                    href="<%= rutaAgenda %>"
                                    class="btn btn-primary">

                                    Agendar cita

                                </a>

                            </div>

                        </div>

                    </div>

                </div>



                <!-- MÉDICO 2 -->

                <div class="col-lg-4 col-md-6">

                    <div class="doctor-card">


                        <div class="doctor-image">

                            <img
                                src="<%= contexto %>/assets/home/img/doctors/doctor2.jpg"
                                alt="Profesional del equipo médico">

                        </div>


                        <div class="doctor-info">

                            <h4>

                                Horarios disponibles

                            </h4>


                            <span class="doctor-specialty">

                                Agenda organizada

                            </span>


                            <p>

                                Revisá las fechas y los turnos disponibles
                                antes de reservar tu consulta.

                            </p>


                            <div class="doctor-experience">

                                <i class="bi bi-award"></i>

                                Disponibilidad en tiempo real

                            </div>


                            <div class="doctor-actions">

                                <a
                                    href="<%= contexto %>/medicos"
                                    class="btn btn-outline-primary">

                                    Ver médicos

                                </a>


                                <a
                                    href="<%= rutaAgenda %>"
                                    class="btn btn-primary">

                                    Agendar cita

                                </a>

                            </div>

                        </div>

                    </div>

                </div>



                <!-- MÉDICO 3 -->

                <div class="col-lg-4 col-md-6">

                    <div class="doctor-card">


                        <div class="doctor-image">

                            <img
                                src="<%= contexto %>/assets/home/img/doctors/doctor3.jpg"
                                alt="Profesional del equipo médico">

                        </div>


                        <div class="doctor-info">

                            <h4>

                                Atención por especialidad

                            </h4>


                            <span class="doctor-specialty">

                                Búsqueda sencilla

                            </span>


                            <p>

                                Encontrá el área médica adecuada y accedé
                                a los profesionales relacionados.

                            </p>


                            <div class="doctor-experience">

                                <i class="bi bi-award"></i>

                                Especialidades activas

                            </div>


                            <div class="doctor-actions">

                                <a
                                    href="<%= contexto %>/especialidades"
                                    class="btn btn-outline-primary">

                                    Ver especialidades

                                </a>


                                <a
                                    href="<%= rutaAgenda %>"
                                    class="btn btn-primary">

                                    Agendar cita

                                </a>

                            </div>

                        </div>

                    </div>

                </div>


            </div>



            <!-- VER TODOS -->

            <div class="text-center mt-5">

                <a
                    href="<%= contexto %>/medicos"
                    class="btn btn-outline-primary doctors-link">

                    Ver todos los médicos

                    <i class="bi bi-arrow-right ms-2"></i>

                </a>

            </div>


        </div>

    </section>



    <!-- ==================================================
         SERVICIOS PARA PACIENTES
    =================================================== -->

    <section class="patient-services">

        <div class="container">


            <!-- TÍTULO -->

            <div class="text-center mb-5">

                <h2>

                    Todo lo que necesitás, en un solo lugar

                </h2>


                <p>

                    Gestioná tu atención médica de manera sencilla
                    desde SoraCare.

                </p>

            </div>



            <div class="row g-4">


                <!-- MIS CITAS -->

                <div class="col-lg-3 col-md-6">

                    <div class="patient-service">

                        <div class="patient-service-icon">

                            <i class="bi bi-calendar-check"></i>

                        </div>


                        <h4>

                            Mis citas

                        </h4>


                        <p>

                            Consultá tus próximas citas y
                            horarios disponibles.

                        </p>


                        <a href="<%= rutaAgenda %>">

                            Ver mis citas

                            <i class="bi bi-arrow-right"></i>

                        </a>

                    </div>

                </div>



                <!-- INDICACIONES MEDICAS -->

                <div class="col-lg-3 col-md-6">

                    <div class="patient-service">

                        <div class="patient-service-icon">

                            <i class="bi bi-file-medical"></i>

                        </div>


                        <h4>

                            Mis indicaciones médicas

                        </h4>


                        <p>

                            Accedé a las indicaciones médicas
                            disponibles en tu cuenta.

                        </p>


                        <span class="service-coming-soon">

                            Próximamente

                        </span>

                    </div>

                </div>



                <!-- PAGOS -->

                <div class="col-lg-3 col-md-6">

                    <div class="patient-service">

                        <div class="patient-service-icon">

                            <i class="bi bi-credit-card"></i>

                        </div>


                        <h4>

                            Pagos

                        </h4>


                        <p>

                            Consultá y gestioná el pago
                            de tus consultas médicas.

                        </p>


                        <a href="<%= rutaPagos %>">

                            Ver pagos

                            <i class="bi bi-arrow-right"></i>

                        </a>

                    </div>

                </div>



                <!-- INFORMACIÓN -->

                <div class="col-lg-3 col-md-6">

                    <div class="patient-service">

                        <div class="patient-service-icon">

                            <i class="bi bi-person-vcard"></i>

                        </div>


                        <h4>

                            Mi información

                        </h4>


                        <p>

                            Consultá la información disponible
                            de tu atención médica.

                        </p>


                        <span class="service-coming-soon">

                            Próximamente

                        </span>

                    </div>

                </div>


            </div>

        </div>

    </section>



    <!-- ==================================================
         ¿POR QUÉ ELEGIR SORACARE?
    =================================================== -->

    <section class="why-soracare" id="nosotros">

        <div class="container">

            <div class="row align-items-center">


                <!-- CONTENIDO -->

                <div class="col-lg-6">

                    <span class="why-subtitle">

                        ¿Por qué SoraCare?

                    </span>


                    <h2>

                        Una experiencia de atención médica
                        pensada para vos.

                    </h2>


                    <p class="why-description">

                        Combinamos atención médica y tecnología para
                        ayudarte a gestionar tus consultas de manera
                        sencilla, segura y cómoda.

                    </p>



                    <!-- BENEFICIO 1 -->

                    <div class="why-item">

                        <div class="why-icon">

                            <i class="bi bi-heart-pulse"></i>

                        </div>


                        <div>

                            <h4>

                                Atención especializada

                            </h4>


                            <p>

                                Accedé a profesionales de diferentes
                                especialidades médicas.

                            </p>

                        </div>

                    </div>



                    <!-- BENEFICIO 2 -->

                    <div class="why-item">

                        <div class="why-icon">

                            <i class="bi bi-calendar-check"></i>

                        </div>


                        <div>

                            <h4>

                                Gestión sencilla de citas

                            </h4>


                            <p>

                                Reservá y administrá tus consultas
                                de manera rápida y sencilla.

                            </p>

                        </div>

                    </div>



                    <!-- BENEFICIO 3 -->

                    <div class="why-item">

                        <div class="why-icon">

                            <i class="bi bi-shield-check"></i>

                        </div>


                        <div>

                            <h4>

                                Información segura

                            </h4>


                            <p>

                                La información de los pacientes
                                se gestiona de forma segura.

                            </p>

                        </div>

                    </div>



                    <!-- BENEFICIO 4 -->

                    <div class="why-item">

                        <div class="why-icon">

                            <i class="bi bi-laptop"></i>

                        </div>


                        <div>

                            <h4>

                                Acceso desde cualquier lugar

                            </h4>


                            <p>

                                Gestioná tu atención médica desde
                                cualquier dispositivo.

                            </p>

                        </div>

                    </div>


                </div>



                <!-- IMAGEN -->

                <div class="col-lg-6">

                    <div class="why-image">

                        <img
                            src="<%= contexto %>/assets/home/img/why-soracare/medical-team.jpg"
                            class="img-fluid"
                            alt="Equipo médico">

                    </div>

                </div>


            </div>

        </div>

    </section>



    <!-- ==================================================
         ¿CÓMO FUNCIONA?
    =================================================== -->

    <section class="how-it-works">

        <div class="container">


            <!-- TÍTULO -->

            <div class="text-center mb-5">

                <span class="how-subtitle">

                    Sencillo y rápido

                </span>


                <h2>

                    ¿Cómo funciona SoraCare?

                </h2>


                <p>

                    Gestionar tu atención médica nunca fue tan sencillo.

                </p>

            </div>



            <!-- PASOS -->

            <div class="row g-4">


                <!-- PASO 1 -->

                <div class="col-lg-3 col-md-6">

                    <div class="step-card">

                        <div class="step-number">

                            1

                        </div>


                        <div class="step-icon">

                            <i class="bi bi-person-check"></i>

                        </div>


                        <h4>

                            Elegí

                        </h4>


                        <p>

                            Encontrá el médico o la especialidad
                            que necesitás.

                        </p>

                    </div>

                </div>



                <!-- PASO 2 -->

                <div class="col-lg-3 col-md-6">

                    <div class="step-card">

                        <div class="step-number">

                            2

                        </div>


                        <div class="step-icon">

                            <i class="bi bi-calendar-check"></i>

                        </div>


                        <h4>

                            Reservá

                        </h4>


                        <p>

                            Seleccioná el día y horario disponible
                            que más te convenga.

                        </p>

                    </div>

                </div>



                <!-- PASO 3 -->

                <div class="col-lg-3 col-md-6">

                    <div class="step-card">

                        <div class="step-number">

                            3

                        </div>


                        <div class="step-icon">

                            <i class="bi bi-credit-card"></i>

                        </div>


                        <h4>

                            Pagá

                        </h4>


                        <p>

                            Elegí el método de pago disponible
                            para tu consulta.

                        </p>

                    </div>

                </div>



                <!-- PASO 4 -->

                <div class="col-lg-3 col-md-6">

                    <div class="step-card">

                        <div class="step-number">

                            4

                        </div>


                        <div class="step-icon">

                            <i class="bi bi-check-circle"></i>

                        </div>


                        <h4>

                            Confirmá

                        </h4>


                        <p>

                            Recibí la confirmación de tu cita
                            y consultá todos los detalles.

                        </p>

                    </div>

                </div>


            </div>

        </div>

    </section>



    <!-- ==================================================
         LLAMADO A LA ACCIÓN
    =================================================== -->

    <section class="cta-section">

        <div class="container">

            <div class="cta-content">


                <div>

                    <span class="cta-subtitle">

                        Tu salud, en un solo lugar

                    </span>


                    <h2>

                        ¿Necesitás atención médica?

                    </h2>


                    <p>

                        Encontrá el especialista adecuado y
                        reservá tu cita de forma rápida y sencilla.

                    </p>

                </div>



                <div class="cta-buttons">

                    <a
                        href="<%= rutaAgenda %>"
                        class="btn btn-light">

                        Agendar una cita

                        <i class="bi bi-arrow-right ms-2"></i>

                    </a>


                    <a
                        href="<%= contexto %>/medicos"
                        class="btn btn-outline-light">

                        Ver médicos

                    </a>

                </div>


            </div>

        </div>

    </section>



    <!-- ==================================================
         CONTACTO
    =================================================== -->

    <section class="contact-section" id="contacto">

        <div class="container">


            <!-- TÍTULO -->

            <div class="text-center mb-5">

                <span class="contact-subtitle">

                    Estamos para ayudarte

                </span>


                <h2>

                    ¿Necesitás ayuda?

                </h2>


                <p>

                    Ponete en contacto con nosotros.
                    Estaremos encantados de ayudarte.

                </p>

            </div>



            <!-- INFORMACIÓN -->

            <div class="row g-4">


                <!-- UBICACIÓN -->

                <div class="col-lg-3 col-md-6">

                    <div class="contact-item">

                        <div class="contact-icon">

                            <i class="bi bi-geo-alt"></i>

                        </div>


                        <h4>

                            Ubicación

                        </h4>


                        <p>

                            Ciudad del Este, Paraguay

                        </p>

                    </div>

                </div>



                <!-- TELÉFONO -->

                <div class="col-lg-3 col-md-6">

                    <div class="contact-item">

                        <div class="contact-icon">

                            <i class="bi bi-telephone"></i>

                        </div>


                        <h4>

                            Teléfono

                        </h4>


                        <p>

                            +595 61 000 000

                        </p>

                    </div>

                </div>



                <!-- CORREO -->

                <div class="col-lg-3 col-md-6">

                    <div class="contact-item">

                        <div class="contact-icon">

                            <i class="bi bi-envelope"></i>

                        </div>


                        <h4>

                            Correo electrónico

                        </h4>


                        <p>

                            contacto@soracare.com

                        </p>

                    </div>

                </div>



                <!-- HORARIOS -->

                <div class="col-lg-3 col-md-6">

                    <div class="contact-item">

                        <div class="contact-icon">

                            <i class="bi bi-clock"></i>

                        </div>


                        <h4>

                            Horarios

                        </h4>


                        <p>

                            Lunes a viernes<br>
                            07:00 - 18:00

                        </p>

                    </div>

                </div>


            </div>

        </div>

    </section>



    <!-- ==================================================
         FOOTER
    =================================================== -->

    <footer class="footer">

        <div class="container">

            <div class="row g-4">


                <!-- INFORMACIÓN -->

                <div class="col-lg-5">

                    <img
                        src="<%= contexto %>/assets/home/img/logo/SoraCare1.png"
                        class="footer-logo"
                        alt="Logo SoraCare">


                    <p class="footer-description">

                        SoraCare es una plataforma de gestión
                        hospitalaria diseñada para facilitar el
                        acceso y la administración de servicios
                        médicos.

                    </p>


                    <!-- REDES -->

                    <div class="footer-social">

                        <span
                            class="social-placeholder"
                            aria-label="Facebook próximamente"
                            title="Facebook próximamente">

                            <i class="bi bi-facebook"></i>

                        </span>


                        <span
                            class="social-placeholder"
                            aria-label="Instagram próximamente"
                            title="Instagram próximamente">

                            <i class="bi bi-instagram"></i>

                        </span>


                        <span
                            class="social-placeholder"
                            aria-label="WhatsApp próximamente"
                            title="WhatsApp próximamente">

                            <i class="bi bi-whatsapp"></i>

                        </span>

                    </div>

                </div>



                <!-- ENLACES -->

                <div class="col-lg-2 col-md-4">

                    <h4>

                        Enlaces

                    </h4>


                    <ul>

                        <li>
                            <a href="<%= contexto %>/#inicio">Inicio</a>
                        </li>

                        <li>
                            <a href="<%= contexto %>/#nosotros">Nosotros</a>
                        </li>

                        <li>
                            <a href="<%= contexto %>/especialidades">Especialidades</a>
                        </li>

                        <li>
                            <a href="<%= contexto %>/medicos">Médicos</a>
                        </li>

                    </ul>

                </div>



                <!-- SERVICIOS -->

                <div class="col-lg-2 col-md-4">

                    <h4>

                        Servicios

                    </h4>


                    <ul>

                        <li>
                            <a href="<%= rutaAgenda %>">Agendar cita</a>
                        </li>

                        <li>
                            <a href="<%= rutaAgenda %>">Mis citas</a>
                        </li>

                        <li>
                            <a href="<%= rutaPagos %>">Pagos</a>
                        </li>

                        <li>
                            <a href="<%= contexto %>/#contacto">Contacto</a>
                        </li>

                    </ul>

                </div>



                <!-- CONTACTO -->

                <div class="col-lg-3 col-md-4">

                    <h4>

                        Contacto

                    </h4>


                    <ul class="footer-contact">

                        <li>

                            <i class="bi bi-geo-alt"></i>

                            Ciudad del Este, Paraguay

                        </li>


                        <li>

                            <i class="bi bi-telephone"></i>

                            +595 61 000 000

                        </li>


                        <li>

                            <i class="bi bi-envelope"></i>

                            contacto@soracare.com

                        </li>

                    </ul>

                </div>


            </div>



            <!-- COPYRIGHT -->

            <div class="footer-bottom">

                <p>

                    © 2026 SoraCare-FPUNE.
                    Todos los derechos reservados.

                </p>


                <p>

                    Proyecto académico -
                    Facultad Politécnica UNE

                </p>

            </div>


        </div>

    </footer>



    <!-- ==================================================
         BOOTSTRAP JAVASCRIPT
    =================================================== -->

    <script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js">
    </script>


</body>

</html>
