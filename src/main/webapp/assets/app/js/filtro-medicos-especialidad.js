(() => {
    const especialidad = document.getElementById('idEspecialidad');
    const medico = document.getElementById('idMedico');
    const sinMedicos = document.getElementById('sinMedicosEspecialidad');
    if (!especialidad || !medico || !sinMedicos) return;

    const filtrarMedicos = () => {
        const idEspecialidad = especialidad.value;
        let cantidadVisible = 0;

        Array.from(medico.options).slice(1).forEach((opcion) => {
            const especialidades = (opcion.dataset.especialidades || '').split(',');
            const compatible = !idEspecialidad || especialidades.includes(idEspecialidad);
            opcion.hidden = !compatible;
            opcion.disabled = !compatible;
            if (compatible) cantidadVisible++;
        });

        if (medico.selectedOptions[0]?.disabled) medico.value = '';
        const sinCoincidencias = Boolean(idEspecialidad) && cantidadVisible === 0;
        medico.disabled = sinCoincidencias;
        medico.options[0].textContent = sinCoincidencias
                ? 'Sin médicos disponibles'
                : 'Todos los médicos';
        sinMedicos.classList.toggle('d-none', !sinCoincidencias);
    };

    especialidad.addEventListener('change', filtrarMedicos);
    filtrarMedicos();
})();
