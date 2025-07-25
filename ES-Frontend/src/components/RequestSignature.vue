<script setup>
import { ref } from 'vue';

const form = ref({
  nombre: '',
  correo: '',
  organizacion: '',
  password: '',
  opcion: 'descargar',
});
const certStatus = ref('');

function getToken() {
  return localStorage.getItem('token');
}
async function solicitarCertificado() {
  certStatus.value = '';
  try {
    const res = await fetch('/api/certificates/request', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken()}`,
      },
      body: JSON.stringify(form.value),
    });
    if (!res.ok) throw new Error('Error al generar el certificado');
    if (form.value.opcion === 'descargar') {
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'firma_electronica.p12';
      a.click();
      URL.revokeObjectURL(url);
      certStatus.value = 'Certificado generado y descargado correctamente';
    } else {
      const data = await res.json();
      certStatus.value = data.message || 'Certificado guardado correctamente';
    }
  } catch (err) {
    certStatus.value = 'Hubo un error: ' + err.message;
  }
}
</script>
<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-bold text-slate-900">Solicitar Firma Electrónica</h2>
        <p class="text-slate-600 mt-2">Envía solicitudes de firma a otros usuarios de forma segura</p>
      </div>
      <span class="bg-emerald-100 text-emerald-700 px-3 py-1 rounded flex items-center font-medium">
        <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h10a4 4 0 004-4M7 15V7a4 4 0 018 0v8"/></svg>
        Proceso Seguro
      </span>
    </div>
    <div class="grid lg:grid-cols-3 gap-8">
      <div class="lg:col-span-2">
        <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
          <div class="p-6">
            <h3 class="flex items-center text-xl font-semibold mb-2">
              <svg class="w-6 h-6 mr-3 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-7a4 4 0 11-8 0 4 4 0 018 0zm6 8a4 4 0 00-3-3.87M6 10a4 4 0 100-8 4 4 0 000 8z"/></svg>
              Información del Destinatario
            </h3>
            <p class="text-slate-600 mb-4">Completa los datos de la persona que debe firmar el documento</p>
            <form @submit.prevent="solicitarCertificado" class="space-y-6">
              <div class="grid md:grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label class="flex items-center text-slate-700 font-medium">
                    <!-- Icono de usuario (personas) -->
                    <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-7a4 4 0 11-8 0 4 4 0 018 0zm6 8a4 4 0 00-3-3.87M6 10a4 4 0 100-8 4 4 0 000 8z"/></svg>
                    Nombre Completo
                  </label>
                  <input v-model="form.nombre" placeholder="Nombre y apellidos del destinatario" required class="block w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2" />
                </div>
                <div class="space-y-2">
                  <label class="flex items-center text-slate-700 font-medium">
                    <!-- Icono de sobre (mail) -->
                    <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25H4.5a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-.876 1.797l-7.5 5.625a2.25 2.25 0 01-2.748 0l-7.5-5.625A2.25 2.25 0 012.25 6.993V6.75"/></svg>
                    Correo Electrónico
                  </label>
                  <input v-model="form.correo" placeholder="destinatario@ejemplo.com" type="email" required class="block w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2" />
                </div>
              </div>
              <div class="space-y-2">
                <label class="flex items-center text-slate-700 font-medium">
                  <!-- Icono de edificio (building) -->
                  <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 21V7a2 2 0 012-2h2a2 2 0 012 2v14m6 0V7a2 2 0 012-2h2a2 2 0 012 2v14M3 21h18"/></svg>
                  Organización (opcional)
                </label>
                <input v-model="form.organizacion" placeholder="Empresa u organización del destinatario" class="block w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2" />
              </div>
              <div class="space-y-2">
                <label class="flex items-center text-slate-700 font-medium">
                  <!-- Icono de llave (key) -->
                  <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 11-4 0 2 2 0 014 0zm-2 2v6m0 0h6m-6 0H7"/></svg>
                  Contraseña para .p12
                </label>
                <input v-model="form.password" type="password" placeholder="Contraseña del certificado digital" required class="block w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2" />
              </div>
              <div class="space-y-2">
                <label class="text-slate-700 font-medium">¿Qué deseas hacer con tu certificado?</label>
                <select v-model="form.opcion" class="block w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2">
                  <option value="descargar">Descargar</option>
                  <option value="guardar">Guardar en el sistema</option>
                </select>
              </div>
              <button type="submit" class="w-full h-12 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-lg">Solicitar Firma</button>
              <p v-if="certStatus" class="mt-2 text-center" :class="certStatus.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ certStatus }}</p>
            </form>
          </div>
        </div>
      </div>
      <div class="space-y-6">
        <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
          <div class="p-6">
            <h4 class="text-lg font-semibold mb-2">Proceso de Firma</h4>
            <p class="text-slate-600 mb-4">Cómo funciona el proceso</p>
            <div class="space-y-4">
              <div class="flex items-start space-x-4">
                <div class="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center flex-shrink-0 text-sm font-bold text-emerald-600">1</div>
                <div>
                  <h5 class="font-medium text-slate-900 mb-1">Envío de Solicitud</h5>
                  <p class="text-sm text-slate-600">Se envía un correo electrónico al destinatario con las instrucciones.</p>
                </div>
              </div>
              <div class="flex items-start space-x-4">
                <div class="w-8 h-8 bg-teal-100 rounded-full flex items-center justify-center flex-shrink-0 text-sm font-bold text-teal-600">2</div>
                <div>
                  <h5 class="font-medium text-slate-900 mb-1">Acceso Seguro</h5>
                  <p class="text-sm text-slate-600">El destinatario accede de forma segura con su certificado digital.</p>
                </div>
              </div>
              <div class="flex items-start space-x-4">
                <div class="w-8 h-8 bg-amber-100 rounded-full flex items-center justify-center flex-shrink-0 text-sm font-bold text-amber-600">3</div>
                <div>
                  <h5 class="font-medium text-slate-900 mb-1">Firma Digital</h5>
                  <p class="text-sm text-slate-600">Se completa la firma electrónica de forma criptográficamente segura.</p>
                </div>
              </div>
              <div class="flex items-start space-x-4">
                <div class="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0 text-sm font-bold text-green-600">4</div>
                <div>
                  <h5 class="font-medium text-slate-900 mb-1">Confirmación</h5>
                  <p class="text-sm text-slate-600">Ambas partes reciben confirmación del documento firmado.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="border-emerald-200 bg-emerald-50 rounded-xl p-4 flex items-center space-x-3">
          <svg class="h-5 w-5 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/></svg>
          <span class="text-emerald-800"><strong>Consejo:</strong> Verifica que el correo electrónico sea correcto antes de enviar. El destinatario recibirá todas las instrucciones necesarias para completar la firma.</span>
        </div>
        <div class="border-0 shadow-sm bg-gradient-to-r from-emerald-500 to-teal-600 text-white rounded-xl">
          <div class="p-6">
            <h3 class="font-semibold mb-2">Seguridad Garantizada</h3>
            <p class="text-sm text-emerald-100 mb-4">Todas las firmas utilizan criptografía avanzada y son legalmente válidas según la normativa vigente.</p>
            <span class="bg-white/20 text-white border-white/30 px-3 py-1 rounded inline-flex items-center">
              <svg class="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 11c0-1.104.896-2 2-2s2 .896 2 2v1a2 2 0 01-2 2h-2a2 2 0 01-2-2v-1c0-1.104.896-2 2-2z"/></svg>
              Certificado SSL
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template> 