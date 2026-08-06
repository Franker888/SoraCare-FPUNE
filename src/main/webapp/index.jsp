<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>SoraCare-FPUNE</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
          rel="stylesheet">
</head>

<body class="bg-light">

<main class="container py-5">
    <section class="card border-0 shadow-sm">
        <div class="card-body p-5">

                <span class="badge text-bg-primary mb-3">
                    Primera prueba con Bootstrap 5
                </span>

            <h1 class="display-5 fw-bold text-primary">
                SoraCare-FPUNE
            </h1>

            <h2 class="h4 text-secondary">
                Sistema Hospitalario Inteligente
            </h2>

            <p class="lead mb-0">
                La primera página JSP con Bootstrap funciona correctamente.
            </p>
            <a href="${pageContext.request.contextPath}/estado"
               class="btn btn-primary mt-4">
                Comprobar backend
            </a>
        </div>
    </section>
</main>

</body>
</html>