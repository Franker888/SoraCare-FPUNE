<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!doctype html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>Acceso restringido | SoraCare-FPUNE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/app/css/soracare-app.css">
    <style>
        body.error-page {
            overflow-x: hidden;
            background:
                    radial-gradient(circle at 8% 12%, rgba(37, 99, 235, .10), transparent 28rem),
                    radial-gradient(circle at 92% 88%, rgba(20, 184, 166, .12), transparent 30rem),
                    #f8fafc !important;
        }

        .error-navbar {
            min-height: 82px;
        }

        .error-main {
            min-height: calc(100vh - 82px);
            display: grid;
            place-items: center;
            padding-block: 3rem;
        }

        .error-card {
            overflow: hidden;
            border: 1px solid rgba(226, 232, 240, .9);
            border-radius: 1.6rem;
            background: rgba(255, 255, 255, .96);
            box-shadow: 0 28px 70px rgba(30, 64, 175, .12);
        }

        .error-copy {
            padding: clamp(2rem, 5vw, 4.75rem);
        }

        .error-kicker {
            display: inline-flex;
            align-items: center;
            gap: .5rem;
            margin-bottom: 1.25rem;
            padding: .48rem .8rem;
            border: 1px solid #bfdbfe;
            border-radius: 999px;
            color: #1d4ed8;
            background: #eff6ff;
            font-size: .82rem;
            font-weight: 700;
        }

        .error-code {
            margin-bottom: .15rem;
            color: #2563eb;
            font-size: clamp(4.6rem, 11vw, 7.75rem);
            font-weight: 700;
            letter-spacing: -.08em;
            line-height: .9;
        }

        .error-title {
            margin-bottom: 1rem;
            color: #172033;
            font-size: clamp(2rem, 4vw, 3rem);
            font-weight: 700;
            letter-spacing: -.04em;
        }

        .error-description {
            max-width: 39rem;
            margin-bottom: 1.75rem;
            color: #64748b;
            font-size: 1.05rem;
            line-height: 1.8;
        }

        .error-note {
            display: flex;
            gap: .75rem;
            align-items: flex-start;
            max-width: 39rem;
            margin-bottom: 2rem;
            padding: 1rem 1.1rem;
            border-radius: .9rem;
            color: #334155;
            background: #f1f5f9;
            font-size: .92rem;
        }

        .error-visual {
            position: relative;
            display: grid;
            min-height: 100%;
            place-items: center;
            padding: 3rem;
            background: linear-gradient(145deg, #1e3a8a 0%, #2563eb 56%, #14b8a6 125%);
        }

        .error-visual::before,
        .error-visual::after {
            position: absolute;
            content: "";
            border-radius: 50%;
            border: 1px solid rgba(255, 255, 255, .16);
        }

        .error-visual::before {
            width: 22rem;
            height: 22rem;
        }

        .error-visual::after {
            width: 29rem;
            height: 29rem;
        }

        .error-illustration {
            position: relative;
            z-index: 1;
            width: min(100%, 31rem);
            filter: drop-shadow(0 24px 28px rgba(15, 23, 42, .22));
        }

        .error-actions {
            display: flex;
            flex-wrap: wrap;
            gap: .75rem;
        }

        .error-actions .btn {
            min-height: 48px;
            padding-inline: 1.2rem;
        }

        @media (max-width: 991.98px) {
            .error-main {
                align-items: start;
                padding-block: 1.5rem 3rem;
            }

            .error-visual {
                min-height: 19rem;
                padding: 2rem;
            }

            .error-illustration {
                width: min(100%, 23rem);
            }
        }
    </style>
</head>
<body class="app-shell error-page">
<nav class="navbar error-navbar">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/">SoraCare-FPUNE</a>
        <span class="navbar-text d-none d-sm-inline">Centro médico integral</span>
    </div>
</nav>

<main class="error-main">
    <div class="container">
        <section class="error-card">
            <div class="row g-0">
                <div class="col-lg-7 order-2 order-lg-1">
                    <div class="error-copy">
                        <span class="error-kicker"><i class="bi bi-shield-lock-fill"></i> Área protegida</span>
                        <div class="error-code">403</div>
                        <h1 class="error-title">Este sector no corresponde a tu acceso</h1>
                        <p class="error-description">
                            La página que intentaste abrir está reservada para otro perfil de SoraCare.
                            Tu sesión continúa activa y tus datos permanecen protegidos.
                        </p>
                        <div class="error-note">
                            <i class="bi bi-info-circle-fill text-primary mt-1"></i>
                            <span>Si necesitás realizar una gestión, regresá a tu panel para utilizar las opciones habilitadas para tu cuenta.</span>
                        </div>
                        <div class="error-actions">
                            <a class="btn btn-primary" href="${pageContext.request.contextPath}/dashboard">
                                <i class="bi bi-grid-1x2-fill me-2"></i>Volver a mi panel
                            </a>
                            <a class="btn btn-outline-primary" href="${pageContext.request.contextPath}/">
                                <i class="bi bi-house-door me-2"></i>Ir a la página principal
                            </a>
                        </div>
                    </div>
                </div>
                <div class="col-lg-5 order-1 order-lg-2">
                    <div class="error-visual" aria-hidden="true">
                        <svg class="error-illustration" viewBox="0 0 560 390" role="img">
                            <title>Ambulancia detenida ante un acceso restringido</title>
                            <ellipse cx="275" cy="338" rx="222" ry="24" fill="#0f255f" opacity=".24"/>
                            <path d="M77 280h329c23 0 42 19 42 42H56c0-23 9-42 21-42Z" fill="#dbeafe"/>
                            <rect x="92" y="137" width="261" height="170" rx="25" fill="#fff"/>
                            <path d="M353 194h70c18 0 34 10 43 25l35 59v29H353V194Z" fill="#f8fafc"/>
                            <path d="M375 214h43c10 0 19 5 24 14l19 33h-86v-47Z" fill="#93c5fd"/>
                            <rect x="115" y="178" width="214" height="25" rx="12.5" fill="#2563eb"/>
                            <rect x="115" y="242" width="214" height="16" rx="8" fill="#14b8a6"/>
                            <rect x="197" y="206" width="50" height="50" rx="13" fill="#eff6ff"/>
                            <path d="M216 214h12v12h12v12h-12v12h-12v-12h-12v-12h12v-12Z" fill="#2563eb"/>
                            <path d="M163 137v-17c0-11 9-20 20-20h42c11 0 20 9 20 20v17" fill="#bfdbfe"/>
                            <path d="M180 120h48" stroke="#2563eb" stroke-width="11" stroke-linecap="round"/>
                            <circle cx="150" cy="309" r="39" fill="#172554"/>
                            <circle cx="150" cy="309" r="19" fill="#bfdbfe"/>
                            <circle cx="414" cy="309" r="39" fill="#172554"/>
                            <circle cx="414" cy="309" r="19" fill="#bfdbfe"/>
                            <circle cx="488" cy="121" r="53" fill="#fff" opacity=".98"/>
                            <circle cx="488" cy="121" r="42" fill="#fee2e2"/>
                            <path d="M463 96l50 50M513 96l-50 50" stroke="#ef4444" stroke-width="13" stroke-linecap="round"/>
                            <path d="M488 174v105" stroke="#fff" stroke-width="10" stroke-linecap="round"/>
                            <path d="M451 278h74" stroke="#fff" stroke-width="12" stroke-linecap="round"/>
                        </svg>
                    </div>
                </div>
            </div>
        </section>
    </div>
</main>
</body>
</html>
