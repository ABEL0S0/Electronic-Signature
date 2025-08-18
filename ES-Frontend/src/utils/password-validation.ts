/**
 * Sistema de validación de contraseñas con indicadores de seguridad
 */

export interface PasswordValidationResult {
  isValid: boolean;
  score: number; // 0-100
  strength: 'Muy Débil' | 'Débil' | 'Media' | 'Fuerte' | 'Muy Fuerte';
  color: string;
  requirements: {
    minLength: boolean;
    hasUppercase: boolean;
    hasLowercase: boolean;
    hasNumber: boolean;
    hasSpecialChar: boolean;
  };
  feedback: string[];
}

/**
 * Valida una contraseña según los criterios de seguridad
 */
export const validatePassword = (password: string): PasswordValidationResult => {
  const requirements = {
    minLength: password.length >= 8,
    hasUppercase: /[A-Z]/.test(password),
    hasLowercase: /[a-z]/.test(password),
    hasNumber: /\d/.test(password),
    hasSpecialChar: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)
  };

  // Calcular puntuación (0-100)
  let score = 0;
  const feedback: string[] = [];

  // Longitud mínima (20 puntos)
  if (requirements.minLength) {
    score += 20;
  } else {
    feedback.push('Mínimo 8 caracteres');
  }

  // Letra mayúscula (20 puntos)
  if (requirements.hasUppercase) {
    score += 20;
  } else {
    feedback.push('Al menos 1 letra mayúscula');
  }

  // Letra minúscula (20 puntos)
  if (requirements.hasLowercase) {
    score += 20;
  } else {
    feedback.push('Al menos 1 letra minúscula');
  }

  // Número (20 puntos)
  if (requirements.hasNumber) {
    score += 20;
  } else {
    feedback.push('Al menos 1 número');
  }

  // Carácter especial (20 puntos)
  if (requirements.hasSpecialChar) {
    score += 20;
  } else {
    feedback.push('Al menos 1 símbolo especial (!@#$%^&*)');
  }

  // Bonus por longitud extra
  if (password.length > 12) score += 10;
  if (password.length > 16) score += 10;

  // Determinar fortaleza
  let strength: PasswordValidationResult['strength'];
  let color: string;

  if (score < 40) {
    strength = 'Muy Débil';
    color = 'text-red-600';
  } else if (score < 60) {
    strength = 'Débil';
    color = 'text-orange-600';
  } else if (score < 80) {
    strength = 'Media';
    color = 'text-yellow-600';
  } else if (score < 90) {
    strength = 'Fuerte';
    color = 'text-blue-600';
  } else {
    strength = 'Muy Fuerte';
    color = 'text-green-600';
  }

  return {
    isValid: Object.values(requirements).every(req => req),
    score,
    strength,
    color,
    requirements,
    feedback
  };
};

/**
 * Obtiene el color del indicador de fortaleza
 */
export const getStrengthColor = (score: number): string => {
  if (score < 40) return 'bg-red-500';
  if (score < 60) return 'bg-orange-500';
  if (score < 80) return 'bg-yellow-500';
  if (score < 90) return 'bg-blue-500';
  return 'bg-green-500';
};

/**
 * Obtiene el color del texto del indicador de fortaleza
 */
export const getStrengthTextColor = (score: number): string => {
  if (score < 40) return 'text-red-600';
  if (score < 60) return 'text-orange-600';
  if (score < 80) return 'text-yellow-600';
  if (score < 90) return 'text-blue-600';
  return 'text-green-600';
};

/**
 * Genera sugerencias para mejorar la contraseña
 */
export const getPasswordSuggestions = (requirements: PasswordValidationResult['requirements']): string[] => {
  const suggestions: string[] = [];
  
  if (!requirements.minLength) {
    suggestions.push('Usa al menos 8 caracteres');
  }
  if (!requirements.hasUppercase) {
    suggestions.push('Agrega una letra mayúscula (A-Z)');
  }
  if (!requirements.hasLowercase) {
    suggestions.push('Agrega una letra minúscula (a-z)');
  }
  if (!requirements.hasNumber) {
    suggestions.push('Agrega un número (0-9)');
  }
  if (!requirements.hasSpecialChar) {
    suggestions.push('Agrega un símbolo especial (!@#$%^&*)');
  }
  
  return suggestions;
};

/**
 * Componente de indicador de fortaleza de contraseña
 */
export const PasswordStrengthIndicator = {
  template: `
    <div class="password-strength-indicator">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-gray-700">Fortaleza de la contraseña:</span>
        <span :class="['text-sm font-semibold', color]">{{ strength }}</span>
      </div>
      
      <!-- Barra de progreso -->
      <div class="w-full bg-gray-200 rounded-full h-2 mb-3">
        <div 
          :class="['h-2 rounded-full transition-all duration-300', getStrengthColor(score)]"
          :style="{ width: score + '%' }"
        ></div>
      </div>
      
      <!-- Puntuación -->
      <div class="text-xs text-gray-500 mb-3">
        Puntuación: {{ score }}/100
      </div>
      
      <!-- Requisitos -->
      <div class="space-y-1">
        <div class="flex items-center text-sm">
          <span :class="['w-4 h-4 mr-2 rounded-full', requirements.minLength ? 'bg-green-500' : 'bg-red-500']">
            <svg v-if="requirements.minLength" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
            <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </span>
          Mínimo 8 caracteres
        </div>
        <div class="flex items-center text-sm">
          <span :class="['w-4 h-4 mr-2 rounded-full', requirements.hasUppercase ? 'bg-green-500' : 'bg-red-500']">
            <svg v-if="requirements.hasUppercase" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
            <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </span>
          Al menos 1 letra mayúscula
        </div>
        <div class="flex items-center text-sm">
          <span :class="['w-4 h-4 mr-2 rounded-full', requirements.hasLowercase ? 'bg-green-500' : 'bg-red-500']">
            <svg v-if="requirements.hasLowercase" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
            <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </span>
          Al menos 1 letra minúscula
        </div>
        <div class="flex items-center text-sm">
          <span :class="['w-4 h-4 mr-2 rounded-full', requirements.hasNumber ? 'bg-green-500' : 'bg-red-500']">
            <svg v-if="requirements.hasNumber" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
            <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </span>
          Al menos 1 número
        </div>
        <div class="flex items-center text-sm">
          <span :class="['w-4 h-4 mr-2 rounded-full', requirements.hasSpecialChar ? 'bg-green-500' : 'bg-red-500']">
            <svg v-if="requirements.hasSpecialChar" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
            </svg>
            <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </span>
          Al menos 1 símbolo especial (!@#$%^&*)
        </div>
      </div>
      
      <!-- Sugerencias -->
      <div v-if="feedback.length > 0" class="mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded">
        <p class="text-xs text-yellow-800 font-medium mb-1">Sugerencias para mejorar:</p>
        <ul class="text-xs text-yellow-700 space-y-1">
          <li v-for="suggestion in feedback" :key="suggestion" class="flex items-center">
            <span class="w-1 h-1 bg-yellow-500 rounded-full mr-2"></span>
            {{ suggestion }}
          </li>
        </ul>
      </div>
    </div>
  `,
// Eliminado bloque de opciones de Vue 2. Usar solo funciones exportadas y helpers compatibles con Composition API.
};
